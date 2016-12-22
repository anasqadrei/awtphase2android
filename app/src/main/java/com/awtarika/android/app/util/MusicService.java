package com.awtarika.android.app.util;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.awtarika.android.app.R;
import com.awtarika.android.app.SongActivity;
import com.awtarika.android.app.model.Song;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.IOException;

public class MusicService
        extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener
{
    private MediaPlayer mAudioPlayer;
    private State mPlayerState;
    public Song mSong = null;
    public Bitmap mSongImageBitmap = null;

    private final IBinder mBinder = new MusicBinder();

    private AudioManager audioManager;
    private MediaSessionCompat mMediaSessionCompat;

    private OnServiceInteractionListener mListener;

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = MusicService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        // set up the audio player
        mAudioPlayer = new MediaPlayer();
        mAudioPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mAudioPlayer.setOnPreparedListener(this);
        mAudioPlayer.setOnCompletionListener(this);
        mAudioPlayer.setOnErrorListener(this);

        // set initial state
        mPlayerState = State.IDLE;

        // setup the audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // set up media session
        mMediaSessionCompat = new MediaSessionCompat(this, TAG);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(mMediaSessionCallback);

//        WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//
//        // acquire on play
//        wifiLock.acquire();
//
//        // release on pause or stop
//        wifiLock.release();
        Log.v(TAG, "created");
    }

    @Override
    public void onDestroy() {
        // release the media session
        if (mMediaSessionCompat != null) {
            mMediaSessionCompat.release();
            mMediaSessionCompat = null;
        }

        // release the audio player
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }

        // change state
        mPlayerState = State.END;

        // stop notification in case it wasn't stopped
        stopForeground(true);

        Log.v(TAG," music serivce onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // handle notification buttons
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        // play
        resumeSong();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // change state
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_STOPPED, mAudioPlayer.getCurrentPosition(), 1.0f)
                .build();
        mMediaSessionCompat.setPlaybackState(playbackState);
        mPlayerState = State.COMPLETED;

        // no longer require focus
        audioManager.abandonAudioFocus(this);

        // stop listening for button presses
        mMediaSessionCompat.setActive(false);

        // stop notification
        stopForeground(true);

        // notify listeners
        if (mListener != null) {
            mListener.onAudioCompleted();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int type, int extra) {
        // Log the error
        Log.v(TAG, "MediaPlayer Error type: " + String.valueOf(type) + " extra: " + String.valueOf(extra));

        // change state
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f)
                .build();
        mMediaSessionCompat.setPlaybackState(playbackState);
        mPlayerState = State.ERROR;

        // no longer require focus
        audioManager.abandonAudioFocus(this);

        // stop listening for button presses
        mMediaSessionCompat.setActive(false);

        // notify listeners
        if (mListener != null) {
            mListener.onAudioError();
        }

        // error handled(logged)
        return true;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mPlayerState == State.PAUSED) {
                    resumeSong();
                }
                mAudioPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // lost focus for a long time
                pauseSong(true);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // lost focus for a short time
                pauseSong(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // lost focus for a short time, but it's ok to keep playing at an attenuated level
                if (mAudioPlayer.isPlaying()) {
                    mAudioPlayer.setVolume(0.25f, 0.25f);
                }
                break;
        }
    }

    public void startPlayingSong() {
        // get audio focus
        if (tryRequestAudioFocus()) {
            // reset notification
            stopForeground(true);

            try {
                // reset and set song image bitmap to be used in notification.
                // do this early so we have time for the image to set before starting notification
                mSongImageBitmap = null;
                Glide.with(getApplicationContext())
                        .load(mSong.imageURL)
                        .asBitmap()
                        .centerCrop()
                        .dontAnimate()
                        .into(new SimpleTarget<Bitmap>(600, 600) {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                // set the new song image bitmap
                                mSongImageBitmap = bitmap;
                            }
                        });

                // set the audio player to play the current song
                mAudioPlayer.reset();
                mAudioPlayer.setDataSource(mSong.playbackTempURL);
                mAudioPlayer.prepareAsync();

                // change state
                mPlayerState = State.PREPARING;

            } catch (IllegalArgumentException iae) {
                Log.v(TAG, "IllegalArgumentException on setDataSource");
                iae.printStackTrace();
            } catch (IOException ioe) {
                Log.v(TAG, "IOException on setDataSource");
                ioe.printStackTrace();
            }
        }
    }

    public void pauseSong(boolean loseFocus) {
        // pause player
        mAudioPlayer.pause();

        // change state
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PAUSED, mAudioPlayer.getCurrentPosition(), 1.0f)
                .build();
        mMediaSessionCompat.setPlaybackState(playbackState);
        mPlayerState = State.PAUSED;

        // no longer require focus
        if (loseFocus) {
            audioManager.abandonAudioFocus(this);
        }

        // update notification
        Notification notification = buildInitialNotification()
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)))
                .build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);

        // stop foreground but keep notification
        stopForeground(false);

        // notify listeners
        if (mListener != null) {
            mListener.onAudioPause();
        }
    }

    public void resumeSong() {
        // get audio focus
        if (tryRequestAudioFocus()) {
            // resume playing
            mAudioPlayer.start();

            // change state
            PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PLAYING, mAudioPlayer.getCurrentPosition(), 1.0f)
                    .build();
            mMediaSessionCompat.setPlaybackState(playbackState);
            mPlayerState = State.STARTED;

            // create notification
            Notification notification = buildInitialNotification()
                    .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)))
                    .build();

            // start foreground notification
            startForeground(NOTIFICATION_ID, notification);

            // notify listeners
            if (mListener != null) {
                mListener.onAudioPlay();
            }
        }
    }

    private NotificationCompat.Builder buildInitialNotification() {
        // set notification intent
        Intent notificationIntent = new Intent(this, SongActivity.class);
        notificationIntent.putExtra(Song.class.getSimpleName(), mSong);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // use default image if none available or none loaded
        if (mSongImageBitmap == null) {
            mSongImageBitmap = BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher);
        }

        // build notification and set its properties
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_media_play);
        builder.setContentTitle(mSong.title);
        builder.setContentText(mSong.artistName);
        builder.setLargeIcon(mSongImageBitmap);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        builder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setMediaSession(mMediaSessionCompat.getSessionToken()));

        // return
        return builder;
    }

    public State getState() {
        return mPlayerState;
    }

    public int getCurrentPosition() {
        return mAudioPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mAudioPlayer.getDuration();
    }

    private boolean tryRequestAudioFocus() {
        int requestResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // start listening for button presses
            mMediaSessionCompat.setActive(true);

            return true;
        } else {
            Log.v(TAG, "no couldn;t focus, log");

            return false;
        }
    }

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            resumeSong();
        }

        @Override
        public void onPause() {
            super.onPause();
            pauseSong(false);
        }
    };

    public void registerClient(Fragment fragment) {
        // set listener
        if (fragment instanceof OnServiceInteractionListener) {
            mListener = (OnServiceInteractionListener) fragment;
        } else {
            throw new RuntimeException(fragment.toString() + " must implement OnServiceInteractionListener");
        }
    }

    public void unregisterClient() {
        mListener = null;
    }

    // class used for the client Binder. runs in the same process as its clients
    public class MusicBinder extends Binder {
        public MusicService getService() {
            // return this instance of LocalService so clients can call public methods
            return MusicService.this;
        }
    }

    public interface OnServiceInteractionListener {
        void onAudioPlay();
        void onAudioPause();
        void onAudioCompleted();
        void onAudioError();
    }

    // this state is different from PlaybackState(which is used for media buttons only for now)
    public enum State {
        IDLE,
        PREPARING,
        STARTED,
        PAUSED,
        COMPLETED,
        ERROR,
        END
    }
}
