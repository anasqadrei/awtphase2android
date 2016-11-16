package com.awtarika.android.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.awtarika.android.app.model.Artist;
import com.bumptech.glide.Glide;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistActivity extends AppCompatActivity {

    // TODO: 16/11/16 finish artist activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Artist.class.getSimpleName())) {
            Artist artist = intent.getParcelableExtra(Artist.class.getSimpleName());
            setTitle(artist.name);

            final TextView artistNameTextView = (TextView) findViewById(R.id.artist_name);
            artistNameTextView.setText(artist.name);

            final CircleImageView artistImageView = (CircleImageView) findViewById(R.id.artist_image);
            Glide.with(this)
                    .load(artist.imageURL)
                    .centerCrop()
                    .dontAnimate()
                    .into(artistImageView);
        }
    }
}
