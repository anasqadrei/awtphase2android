package com.awtarika.android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AwtarikaApplication;
import com.awtarika.android.app.util.MusicService;
import com.awtarika.android.app.util.MusicServiceManager;
import com.bumptech.glide.Glide;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class MiniPlayerFragment extends Fragment implements MusicService.OnServiceInteractionListener {

    private Song mSong = null;
    private Intent mPlayerIntent;
    private MusicService mMusicService;
    private boolean mBound = false;
    private boolean mPlayWhenReady = false;

    private TextView mSongTitle;
    private TextView mArtistName;
    private ImageView mSongImage;
    private ImageButton mPlayPause;
    private ProgressBar mSpinnerProgress;
    private ProgressBar mPlaybackProgress;

    private Handler mHandler = new Handler();
    private static int PLAYBACK_PROGRESS_TIMER = 200;

    // google analytics
    private Tracker mTracker;
    private String gaScreenCategory = "Player";
    private String gaScreenID = "";

    private OnFragmentInteractionListener mListener;

    private static final String TAG = MiniPlayerFragment.class.getSimpleName();

    public MiniPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // obtain the shared Tracker instance.
        AwtarikaApplication application = (AwtarikaApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.enableAdvertisingIdCollection(true);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);

        mSongTitle = (TextView) rootView.findViewById(R.id.fragment_mini_player_song_title);
        mArtistName = (TextView) rootView.findViewById(R.id.fragment_mini_player_artist_name);
        mSongImage = (ImageView) rootView.findViewById(R.id.fragment_mini_player_song_image);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.fragment_mini_player_play_pause_button);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlayPauseButtonPressed();
            }
        });

        mSpinnerProgress = (ProgressBar) rootView.findViewById(R.id.fragment_mini_player_spinner_progress);
        mPlaybackProgress = (ProgressBar) rootView.findViewById(R.id.fragment_mini_player_playback_progress);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // bind to MusicService if needed to
        if (MusicServiceManager.shouldBindMusicService) {
            bindMusicService();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        unbindMusicService();

        mHandler.removeCallbacks(mPlaybackProgressRunnable);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // set listener
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onAudioPlay() {
        // show the right image on the button
        mPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        showButtonHideProgress();

        // set playback progress
        mPlaybackProgress.setMax(mMusicService.getDuration());
        mHandler.postDelayed(mPlaybackProgressRunnable, PLAYBACK_PROGRESS_TIMER);
    }

    @Override
    public void onAudioPause() {
        // show the right image on the button
        mPlayPause.setImageResource(android.R.drawable.ic_media_play);
        showButtonHideProgress();

        // set playback progress
        mPlaybackProgress.setMax(mMusicService.getDuration());
        mPlaybackProgress.setProgress(mMusicService.getCurrentPosition());

        // stop playback progress from updating
        mHandler.removeCallbacks(mPlaybackProgressRunnable);
    }

    @Override
    public void onAudioCompleted() {
        // stop playback progress from updating
        mHandler.removeCallbacks(mPlaybackProgressRunnable);

        // hide player if song is finished
        if (mListener != null) {
            mListener.onFragmentReadyToBeKilled();
        }
    }

    @Override
    public void onAudioError() {
        // show the right image on the button
        mPlayPause.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        showButtonHideProgress();

        // stop playback progress from updating
        mHandler.removeCallbacks(mPlaybackProgressRunnable);
    }

    public void setSong(Song song) {
        if (song != null) {
            // set metadata
            mSongTitle.setText(song.title);
            mArtistName.setText(song.artistName);

            // set google analytics
            gaScreenID = song.id + "/" + song.title + " - " + song.artistName;

            // if song has an image then show it, otherwise, show app icon
            if (!song.imageURL.isEmpty()) {
                Glide.with(this)
                        .load(song.imageURL)
                        .centerCrop()
                        .dontAnimate()
                        .into(mSongImage);
            } else {
                Glide.with(this)
                        .load(R.mipmap.ic_launcher)
                        .centerCrop()
                        .dontAnimate()
                        .into(mSongImage);
            }
        }
    }

    public void startPlaying(Song song) {
        // set song
        mSong = song;
        setSong(song);

        // loading...
        hideButtonShowProgress();

        // if not bound then start service and bind then play when bound, if already bound then play
        if (!mBound) {
            bindMusicService();
            mPlayWhenReady = true;
        } else {
            mMusicService.mSong = mSong;
            mMusicService.startPlayingSong();
        }
    }

    public void pausePlaying() {
        if (mBound) {
            mMusicService.pauseSong(true);
        }
    }

    public void resumePlaying() {
        if (mBound) {
            mMusicService.resumeSong();
        }
    }

    protected void bindMusicService() {
        // set the intent
        if (mPlayerIntent == null) {
            mPlayerIntent = new Intent(getActivity(), MusicService.class);
        }

        // start service if not started. this is needed to keep the service alive when changing activities
        if (!MusicServiceManager.musicServiceStarted) {
            MusicServiceManager.musicServiceStarted = true;
            getActivity().startService(mPlayerIntent);
        }

        // bind
        getActivity().bindService(mPlayerIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void unbindMusicService() {

        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    // defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // bound to MusicService, cast the IBinder and get MusicService instance
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mMusicService = binder.getService();
            mBound = true;

            // listen to service
            mMusicService.registerClient(MiniPlayerFragment.this);

            // if it's a new song then play when bound
            if (mPlayWhenReady) {
                mPlayWhenReady = false;
                mMusicService.mSong = mSong;
                mMusicService.startPlayingSong();
            }

            // set service song
            setSong(mMusicService.mSong);

            // update the playpause button when bound
            switch (mMusicService.getState()) {
                case STARTED:
                    onAudioPlay();
                    break;
                case PAUSED:
                    onAudioPause();
                    break;
                case ERROR:
                    onAudioError();
                    break;
                default:
                    hideButtonShowProgress();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mMusicService.unregisterClient();
        }
    };

    private Runnable mPlaybackProgressRunnable = new Runnable() {
        @Override
        public void run() {

            if (mBound) {
                mPlaybackProgress.setProgress(mMusicService.getCurrentPosition());
            }

            mHandler.postDelayed(this, PLAYBACK_PROGRESS_TIMER);
        }
    };

    public void onPlayPauseButtonPressed() {
        if (mBound) {
            switch (mMusicService.getState()) {
                case STARTED:
                    // Google Analytics - Event
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(gaScreenCategory)
                            .setAction("Pause - Mini Player")
                            .setLabel(gaScreenID)
                            .build());
                    // pause
                    pausePlaying();
                    break;
                case PAUSED:
                    // Google Analytics - Event
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(gaScreenCategory)
                            .setAction("Play - Mini Player")
                            .setLabel(gaScreenID)
                            .build());
                    // play
                    resumePlaying();
                    break;
                case ERROR:
                    // Google Analytics - Event
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(gaScreenCategory)
                            .setAction("Close - Mini Player")
                            .setLabel(gaScreenID)
                            .build());
                    // close
                    if (mListener != null) {
                        mListener.onFragmentReadyToBeKilled();
                    }
                    break;
            }
        }
    }

    private void showButtonHideProgress() {
        mPlayPause.setVisibility(View.VISIBLE);
        mSpinnerProgress.setVisibility(View.GONE);
    }

    private void hideButtonShowProgress() {
        mPlayPause.setVisibility(View.GONE);
        mSpinnerProgress.setVisibility(View.VISIBLE);

        // in case spinner in on, reset playback because playback is saved when back is pressed
        mPlaybackProgress.setProgress(0);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentReadyToBeKilled();
    }
}
