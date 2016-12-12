package com.awtarika.android.app.util;

import com.awtarika.android.app.model.Song;

/**
 * Created by anasqadrei on 9/12/16.
 */
public class AudioPlayerSingleton {
    public Song mSong;

    private static AudioPlayerSingleton ourInstance = new AudioPlayerSingleton();

    public static AudioPlayerSingleton getInstance() {
        return ourInstance;
    }

    private AudioPlayerSingleton() {
    }

}
