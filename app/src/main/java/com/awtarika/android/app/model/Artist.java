package com.awtarika.android.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anasqadrei on 10/11/16.
 */

public class Artist implements Parcelable {
    public int id;
    public String name;
    public String url;
    public String imageURL;
    public int totalSongsPages;
    public int songsPageSize;
    public int likersCount;
    public int songsCount;

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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(imageURL);
        dest.writeInt(totalSongsPages);
        dest.writeInt(songsPageSize);
    }

    private Artist(Parcel src)
    {
        id = src.readInt();
        name = src.readString();
        url = src.readString();
        imageURL = src.readString();
        totalSongsPages = src.readInt();
        songsPageSize = src.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
        public Artist createFromParcel(Parcel in) {
            return new Artist(in);
        }

        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
}
