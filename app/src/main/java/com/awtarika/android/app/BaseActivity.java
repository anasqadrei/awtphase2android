package com.awtarika.android.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.awtarika.android.app.util.AudioPlayerSingleton;

/**
 * Created by anasqadrei on 23/11/16.
 */

public class BaseActivity extends AppCompatActivity implements MiniPlayerFragment.OnFragmentInteractionListener {

    protected MiniPlayerFragment mMiniPlayerFragment;

    private static final String TAG = BaseActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    protected void onResume() {
        super.onResume();

        // show mini player or not based on song existence
        if (AudioPlayerSingleton.getInstance().mSong == null) {
            hidePlaybackControls();
        } else {
            showPlaybackControls();
        }
    }

    protected void hidePlaybackControls() {
        getFragmentManager()
                .beginTransaction()
                .hide(mMiniPlayerFragment)
                .commit();
    }

    protected void showPlaybackControls() {
        getFragmentManager()
                .beginTransaction()
                .show(mMiniPlayerFragment)
                .commit();
    }

    @Override
    public void onFragmentInteraction() {
        Log.v(TAG, "play pause or stop ");
        hidePlaybackControls();
        AudioPlayerSingleton.getInstance().mSong = null;
    }
}
