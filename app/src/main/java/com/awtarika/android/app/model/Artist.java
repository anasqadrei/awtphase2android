package com.awtarika.android.app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anasqadrei on 10/11/16.
 */

public class Artist {
    public int id;
    public String name;
    public String url;
    public String imageURL;
    public int totalSongsPages;
    public int songsPageSize;

    public Artist(int id, String name) {
        this.id = id;
        this.name = name;

        // Defaults
        totalSongsPages = 1;
        songsPageSize = 20;
    }

    public static Artist createArtist(JSONObject parsedArtist) throws JSONException {

        final Artist artist = new Artist(parsedArtist.getInt("_id"), parsedArtist.getString("name"));

        artist.url = parsedArtist.optString("url");
        artist.imageURL = parsedArtist.optString("image");
        artist.totalSongsPages = parsedArtist.getInt("totalSongsPages");
        artist.songsPageSize = parsedArtist.getInt("songsPageSize");

        return artist;
    }
}
