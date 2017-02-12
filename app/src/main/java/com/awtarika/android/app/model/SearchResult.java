package com.awtarika.android.app.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by anasqadrei on 9/2/17.
 */

public class SearchResult implements Parcelable {
    public int mId;
    public String mType;
    public String mSlug;
    public String mMetaTitle;
    public String mDuration;

    public SearchResult() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mType);
        dest.writeString(mSlug);
        dest.writeString(mMetaTitle);
        dest.writeString(mDuration);
    }

    private SearchResult(Parcel src) {
        mId = src.readInt();
        mType = src.readString();
        mSlug= src.readString();
        mMetaTitle = src.readString();
        mDuration = src.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };
}
