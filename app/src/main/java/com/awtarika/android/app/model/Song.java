package com.awtarika.android.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by anasqadrei on 16/11/16.
 */

public class Song implements Parcelable {
    public int id;
    public String title;
    public int artistID;
    public String artistName;
    public String url;
    public String playbackTempURL;
    public String description;
    public String imageURL;
    public String durationDesc;
    public int playsCount;
    public int likesCount;

    public Song(int id, String title, int artistID, String artistName) {
        this.id = id;
        this.title = title;
        this.artistID = artistID;
        this.artistName = artistName;

        // Defaults
        playsCount = 0;
        likesCount = 0;
    }

    public static Song createSong(JSONObject parsedSong) throws JSONException {

        final int id = parsedSong.getInt("_id");
        final String title = parsedSong.getString("title");
        final int artistID = parsedSong.getJSONObject("artist").getInt("_id");
        final String artistName = parsedSong.getJSONObject("artist").getString("name");

        final Song song = new Song(id, title, artistID, artistName);

        song.url = parsedSong.optString("url");
        song.description = parsedSong.optString("desc");
        song.imageURL = parsedSong.optString("defaultImage");
        song.durationDesc = parsedSong.optString("durationDesc");
        song.playsCount = parsedSong.optInt("playsCount");
        song.likesCount = parsedSong.optInt("likesCount");

        return song;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeInt(artistID);
        dest.writeString(artistName);
        dest.writeString(url);
        dest.writeString(playbackTempURL);
        dest.writeString(description);
        dest.writeString(imageURL);
        dest.writeString(durationDesc);
        dest.writeInt(playsCount);
        dest.writeInt(likesCount);
    }

    private Song(Parcel src)
    {
        id = src.readInt();
        title = src.readString();
        artistID = src.readInt();
        artistName = src.readString();
        url = src.readString();
        playbackTempURL = src.readString();
        description = src.readString();
        imageURL = src.readString();
        durationDesc = src.readString();
        playsCount = src.readInt();
        likesCount = src.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
