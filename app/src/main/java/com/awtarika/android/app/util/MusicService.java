package com.awtarika.android.app.util;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.awtarika.android.app.model.Song;

import java.io.IOException;

public class MusicService
        extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
    private MediaPlayer mAudioPlayer;
    public Song mSong = null;

    private final IBinder mBinder = new MusicBinder();

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
        }

        Log.v(TAG," music serivce onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.v(TAG, "playing soon");
        mAudioPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mSong = null;
        Log.v(TAG, "completed");
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.v(TAG, "error playing");
        mSong = null;
        return false;
    }

    public void playSong() {
        try {
            mAudioPlayer.reset();
            mAudioPlayer.setDataSource(mSong.playbackTempURL);
            mAudioPlayer.prepareAsync();
        } catch (IllegalArgumentException iae) {
            Log.v(TAG, "IllegalArgumentException on setDataSource");
            iae.printStackTrace();
        } catch (IOException ioe) {
            Log.v(TAG, "IOException on setDataSource");
            ioe.printStackTrace();
        }
    }

    // class used for the client Binder. runs in the same process as its clients
    public class MusicBinder extends Binder {
        public MusicService getService() {
            // return this instance of LocalService so clients can call public methods
            return MusicService.this;
        }
    }
}
