package com.awtarika.android.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.apradanas.simplelinkabletext.Link;
import com.apradanas.simplelinkabletext.LinkableTextView;
import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AwtarikaJsonObjectRequest;
import com.awtarika.android.app.util.Constants;
import com.awtarika.android.app.util.LoggerSingleton;
import com.awtarika.android.app.util.MusicServiceManager;
import com.awtarika.android.app.util.NetworkingSingleton;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONObject;

import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class SongActivity extends BaseActivity {

    private Song song;

    private static final String TAG = SongActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
        gaScreenCategory = "Song";

        // ad
        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_mob_application_id));
        AdView songAdView = (AdView) findViewById(R.id.activity_song_ad_view);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(getString(R.string.ad_mob_test_device_zte_blade))
                .build();
        songAdView.loadAd(adRequest);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Song.class.getSimpleName())) {
            song = intent.getParcelableExtra(Song.class.getSimpleName());
            setTitle(song.title);
            gaScreenID = song.id + "/" + song.title + " - " + song.artistName;

            //
            final TextView titleTextView = (TextView) findViewById(R.id.activity_song_title);
            titleTextView.setText(song.title);

            final TextView artistNameTextView = (TextView) findViewById(R.id.activity_song_artist_name);
            artistNameTextView.setText(song.artistName);

            final TextView durationTextView = (TextView) findViewById(R.id.activity_song_duration);
            durationTextView.setText(song.durationDesc);

            final TextView playsCountTextView = (TextView) findViewById(R.id.activity_song_plays_count);
            playsCountTextView.setText(String.valueOf(song.playsCount));

            final TextView likesCountTextView = (TextView) findViewById(R.id.activity_song_likes_count);
            likesCountTextView.setText(String.valueOf(song.likesCount));

            // find hashtags
            Link linkHashtag = new Link(Pattern.compile("#(\\S+)"))
                    .setTextColor(Color.parseColor("#003569"))
                    .setClickListener(new Link.OnClickListener() {
                        @Override
                        public void onClick(String hashtagText) {
                            // pass selected hashtag to the next view
                            Intent hashtagIntent = new Intent(SongActivity.this, HashtagActivity.class);
                            hashtagIntent.putExtra("hashtag", hashtagText);
                            startActivity(hashtagIntent);
                        }
                    });

            final LinkableTextView descriptionTextView = (LinkableTextView) findViewById(R.id.activity_song_description);
            descriptionTextView.setText(song.description)
                    .addLink(linkHashtag)
                    .build();

            final CircleImageView songImageView = (CircleImageView) findViewById(R.id.activity_song_image);
            Glide.with(this)
                    .load(song.imageURL)
                    .centerCrop()
                    .dontAnimate()
                    .into(songImageView);
        } else {
            LoggerSingleton.getInstance(getApplicationContext()).log(TAG + " Song wasn't passed in the intent");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // set action provider for share menu item
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        if (mShareActionProvider != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, song.title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, song.url);
            mShareActionProvider.setShareIntent(shareIntent);
            mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
                @Override
                public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                    // Google Analytics - Event
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(gaScreenCategory)
                            .setAction("Share")
                            .setLabel(gaScreenID)
                            .build());

                    return false;
                }
            });
        }

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // cancel network activities
        NetworkingSingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    public void play(View view) {
        // Google Analytics - Event
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(gaScreenCategory)
                .setAction("Play")
                .setLabel(gaScreenID)
                .build());

        // build request url
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .encodedAuthority(Constants.URLs.HOST)
                .appendPath("song")
                .appendPath("play")
                .appendQueryParameter("songId", String.valueOf(song.id))
                .build();
        String url = builder.toString();

        // define callback
        AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        // parse and set song url
                        song.playbackTempURL = response.optString("url");

                        // start play process
                        mMiniPlayerFragment.startPlaying(song);
                        showPlaybackControls();
                        MusicServiceManager.shouldBindMusicService = true;      //for next onStart() to use


                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = TAG + " play(" + String.valueOf(song.id) + "). Server Error: ";
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    errorMessage += new String(error.networkResponse.data);
                }
                LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
            }
        });
        jsObjRequest.setTag(TAG);

        // call the request url
        NetworkingSingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }
}
