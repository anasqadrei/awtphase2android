package com.awtarika.android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.awtarika.android.app.util.AwtarikaApplication;
import com.awtarika.android.app.util.MusicService;
import com.awtarika.android.app.util.MusicServiceManager;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by anasqadrei on 23/11/16.
 */

public class BaseActivity extends AppCompatActivity implements MiniPlayerFragment.OnFragmentInteractionListener {

    protected MiniPlayerFragment mMiniPlayerFragment;
    protected boolean allowManageFragment;

    protected Intent mPlayerIntent;
    protected MusicService mMusicService;
    protected boolean mBound = false;

    // google analytics
    protected Tracker mTracker;
    protected String gaScreenCategory = "";
    protected String gaScreenID = "";

    private static final String TAG = BaseActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // direct volume key presses to the music audio stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // obtain the shared Tracker instance.
        AwtarikaApplication application = (AwtarikaApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.enableAdvertisingIdCollection(true);

        // get the mini player
        mMiniPlayerFragment = (MiniPlayerFragment) getFragmentManager().findFragmentById(R.id.fragment_mini_player);

        // if it's null then create it and add it to fragment manager to show and hide later
        if (mMiniPlayerFragment == null) {

            mMiniPlayerFragment = new MiniPlayerFragment();

            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_mini_player, mMiniPlayerFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (MusicServiceManager.shouldBindMusicService) {
            bindMusicService();
        }

        // allow show and hide fragment. mainly to solve the "can't hide after onSavedState is called" error
        allowManageFragment = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Google Analytics - Screen View
        String screenName = gaScreenCategory;
        if (!gaScreenID.isEmpty()) {
            screenName += ": " + gaScreenID;
        }
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        // show mini player or not based on music service
        if (MusicServiceManager.musicServiceStarted) {
            showPlaybackControls();
        } else {
            hidePlaybackControls();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindMusicService();

        // allow show and hide fragment. mainly to solve the "can't hide after onSavedState is called" error
        allowManageFragment = false;
    }

    @Override
    public void onFragmentReadyToBeKilled() {
        // beginning of kill process. hide and don't bind
        hidePlaybackControls();
        MusicServiceManager.shouldBindMusicService = false;     //for next onStart() to use

        // unbind form current activity and miniplayer
        unbindMusicService();
        mMiniPlayerFragment.unbindMusicService();

        // stop the service
        if (MusicServiceManager.musicServiceStarted) {
            // set the player intent
            if (mPlayerIntent == null) {
                mPlayerIntent = new Intent(this, MusicService.class);
            }

            // stop
            stopService(mPlayerIntent);
            MusicServiceManager.musicServiceStarted = false;
            mMusicService = null;
        }
    }

    protected void bindMusicService() {
        // set the player intent
        if (mPlayerIntent == null) {
            mPlayerIntent = new Intent(this, MusicService.class);
        }

        // start the service if it wasn't started
        if (!MusicServiceManager.musicServiceStarted) {
            MusicServiceManager.musicServiceStarted = true;
            startService(mPlayerIntent);
        }

        bindService(mPlayerIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected void unbindMusicService() {
        if (mBound) {
            unbindService(mConnection);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    protected void showPlaybackControls() {
        if (allowManageFragment) {
            getFragmentManager()
                    .beginTransaction()
                    .show(mMiniPlayerFragment)
                    .commit();
        }
    }

    protected void hidePlaybackControls() {
        if (allowManageFragment) {
            getFragmentManager()
                    .beginTransaction()
                    .hide(mMiniPlayerFragment)
                    .commit();
        }
    }
}
