package com.awtarika.android.app.util;

import android.app.Fragment;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.awtarika.android.app.R;
import com.awtarika.android.app.SongActivity;
import com.awtarika.android.app.model.Song;

import java.io.IOException;

public class MusicService
        extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
    private MediaPlayer mAudioPlayer;
    private State mPlayerState;
    public Song mSong = null;

    private final IBinder mBinder = new MusicBinder();

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
        mPlayerState = State.IDLE;

//        // what about 3g 4g lte and other internet
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
        // release the audio player
        if (mAudioPlayer != null) {
            mAudioPlayer.release();
            mAudioPlayer = null;
            mPlayerState = State.END;
        }

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
    public void onPrepared(MediaPlayer mediaPlayer) {
        // start playing
        mAudioPlayer.start();

        // set notification intent
        Intent notificationIntent = new Intent(this, SongActivity.class);
        notificationIntent.putExtra(Song.class.getSimpleName(), mSong);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // build notification and set its properties
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(mSong.title)
                .setContentText(mSong.artistName)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .build();

        // start foreground notification
        startForeground(NOTIFICATION_ID, notification);

        // change state
        mPlayerState = State.STARTED;

        // notify listeners
        if (mListener != null) {
            mListener.onAudioPlay();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        // change state
        mPlayerState = State.COMPLETED;

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
        mPlayerState = State.ERROR;

        // notify listeners
        if (mListener != null) {
            mListener.onAudioError();
        }

        // error handled(logged)
        return true;
    }

    public void startPlayingSong() {
        try {
            // reset notification
            stopForeground(true);

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

    public void pauseSong() {
        // pause player
        mAudioPlayer.pause();

        // change state
        mPlayerState = State.PAUSED;

        // notify listeners
        if (mListener != null) {
            mListener.onAudioPause();
        }
    }

    public void resumeSong() {
        // resume playing
        mAudioPlayer.start();

        // change state
        mPlayerState = State.STARTED;

        // notify listeners
        if (mListener != null) {
            mListener.onAudioPlay();
        }
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
