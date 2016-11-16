package com.awtarika.android.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.awtarika.android.app.model.Artist;
import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AwtarikaJsonArrayRequest;
import com.awtarika.android.app.util.Constants;
import com.awtarika.android.app.util.NetworkingSingleton;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistActivity extends AppCompatActivity {

    // state variables  that has to be saved
    private ArrayList<Song> songsList = new ArrayList<Song>();
    private int totalPages = 1;
    private int lastFetchedPage = 0;

    private static final String SONGS_LIST_ARRAY_KEY = "songsList";
    private static final String TOTAL_PAGES_KEY = "totalPages";
    private static final String LAST_FETCHED_PAGE_KEY = "lastFetchedPage";

    // other variables
    private Artist artist;
    private boolean fetching = false;
    private SongsListAdapter mSongsListAdapter;

    private static final String DEFAULT_SORT = "-playsCount";
    private static final String TAG = ArtistActivity.class.getSimpleName();

    // TODO: 17/11/16 share button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Artist.class.getSimpleName())) {
            artist = intent.getParcelableExtra(Artist.class.getSimpleName());
            setTitle(artist.name);

            totalPages = artist.totalSongsPages;

            final TextView artistNameTextView = (TextView) findViewById(R.id.activity_artist_name);
            artistNameTextView.setText(artist.name);

            final TextView artistLikersCountTextView = (TextView) findViewById(R.id.activity_artist_likers_count);
            artistLikersCountTextView.setText(String.format(getResources().getString(R.string.artist_likers_count), artist.likersCount));

            final TextView artistSongsCountTextView = (TextView) findViewById(R.id.activity_artist_songs_count);
            artistSongsCountTextView.setText(String.format(getResources().getString(R.string.artist_songs_count), artist.songsCount));

            final CircleImageView artistImageView = (CircleImageView) findViewById(R.id.activity_artist_image);
            Glide.with(this)
                    .load(artist.imageURL)
                    .centerCrop()
                    .dontAnimate()
                    .into(artistImageView);


            // it means songsList array is empty, so fetch songs
            if (savedInstanceState == null) {
                getSongsList(lastFetchedPage + 1, DEFAULT_SORT);
            }

            mSongsListAdapter = new SongsListAdapter(this);

            final ListView listView = (ListView) findViewById(R.id.activity_artist_songs_list);
            listView.setAdapter(mSongsListAdapter);
//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    // pass selected artist to the next view
//                    Intent artistIntent = new Intent(parent.getContext(), ArtistActivity.class);
//                    artistIntent.putExtra(Artist.class.getSimpleName(), (Artist) parent.getItemAtPosition(position));
//                    startActivity(artistIntent);
//                }
//            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save state
        outState.putInt(TOTAL_PAGES_KEY, totalPages);
        outState.putInt(LAST_FETCHED_PAGE_KEY, lastFetchedPage);
        outState.putParcelableArrayList(SONGS_LIST_ARRAY_KEY, songsList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // restore state
        totalPages = savedInstanceState.getInt(TOTAL_PAGES_KEY);
        lastFetchedPage = savedInstanceState.getInt(LAST_FETCHED_PAGE_KEY);
        songsList = savedInstanceState.getParcelableArrayList(SONGS_LIST_ARRAY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // cancel network activities
        fetching = false;
        NetworkingSingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    private void getSongsList(final int page, String sort) {

        // build request url
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .authority(Constants.URLs.HOST)
                .appendPath("song")
                .appendPath("list")
                .appendQueryParameter("artist", String.valueOf(artist.id))
                .appendQueryParameter("page", String.valueOf(page))
                .appendQueryParameter("pagesize", String.valueOf(artist.songsPageSize))
                .appendQueryParameter("sort", sort)
                .build();
        String url = builder.toString();

        // define callback
        AwtarikaJsonArrayRequest jsObjRequest = new AwtarikaJsonArrayRequest (Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray jsonSongsList) {

                        try {

                            // fill songsList
                            for (int i = 0; i < jsonSongsList.length(); i++) {
                                songsList.add(Song.createSong(jsonSongsList.getJSONObject(i)));
                            }

                            // set song page related values
                            lastFetchedPage = page;

                            // signal reload data
                            mSongsListAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            Log.v(TAG, "Parsing JSON Exception");
                            e.printStackTrace();
                        } finally {
                            fetching = false;
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v(TAG, "JSON didn't work!");
                error.printStackTrace();
                fetching = false;
            }
        });
        jsObjRequest.setTag(TAG);

        // call the request url
        fetching = true;
        NetworkingSingleton.getInstance(this).addToRequestQueue(jsObjRequest);

    }

    private class SongsListAdapter extends BaseAdapter {
        private final Context mContext;

        SongsListAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return songsList.size();
        }

        @Override
        public Object getItem(int position) {
            return songsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return songsList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // item variable
            final Song song = songsList.get(position);

            // in case it was recycled, recreate it
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.list_item_song, null);

                // inflate views
                final TextView songTitleTextView = (TextView) convertView.findViewById(R.id.list_item_song_title);
                final TextView durationTextView = (TextView) convertView.findViewById(R.id.list_item_song_duration);
                final TextView playsCountTextView = (TextView) convertView.findViewById(R.id.list_item_song_plays_count);

                // add them to view holder
                final ViewHolder viewHolder = new ViewHolder(songTitleTextView, durationTextView, playsCountTextView);
                convertView.setTag(viewHolder);
            }

            // get views from view holder
            final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            // set views
            viewHolder.songTitleTextView.setText(song.title);
            viewHolder.durationTextView.setText(song.durationDesc);
            viewHolder.playsCountTextView.setText(String.valueOf(song.playsCount));

            // load more data
            if (!fetching && lastFetchedPage < totalPages && position >= songsList.size() - 4) {
                getSongsList(lastFetchedPage + 1, DEFAULT_SORT);
            }

            return convertView;
        }

        // view holder design pattern for performance enhancements
        private class ViewHolder {
            private final TextView songTitleTextView;
            private final TextView durationTextView;
            private final TextView playsCountTextView;

            ViewHolder(TextView songTitleTextView, TextView durationTextView, TextView playsCountTextView) {
                this.songTitleTextView = songTitleTextView;
                this.durationTextView = durationTextView;
                this.playsCountTextView = playsCountTextView;
            }
        }
    }
}
