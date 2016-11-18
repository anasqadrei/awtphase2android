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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AwtarikaJsonObjectRequest;
import com.awtarika.android.app.util.Constants;
import com.awtarika.android.app.util.NetworkingSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HashtagActivity extends AppCompatActivity {

    // state variables  that has to be saved
    private ArrayList<Song> songsList = new ArrayList<>();
    private int totalPages = 1;
    private int lastFetchedPage = 0;

    private static final String SONGS_LIST_ARRAY_KEY = "songsList";
    private static final String TOTAL_PAGES_KEY = "totalPages";
    private static final String LAST_FETCHED_PAGE_KEY = "lastFetchedPage";

    // other variables
    private String hashtag;
    private boolean fetching = false;
    private DetailedSongsListAdapter mSongsListAdapter;

    private static final String TAG = HashtagActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtag);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("hashtag")) {
            hashtag = intent.getStringExtra("hashtag");
            setTitle(hashtag);

            // it means songsList array is empty, so fetch songs
            if (savedInstanceState == null) {
                getSongsList(lastFetchedPage + 1);
            }

            // songs list
            mSongsListAdapter = new DetailedSongsListAdapter(this);

            final ListView listView = (ListView) findViewById(R.id.activity_hashtag_songs_list);
            listView.setAdapter(mSongsListAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // pass selected song to the next view
                    Intent songIntent = new Intent(parent.getContext(), SongActivity.class);
                    songIntent.putExtra(Song.class.getSimpleName(), (Song) parent.getItemAtPosition(position));
                    startActivity(songIntent);
                }
            });
        } else {
            Log.v(TAG, "Hashtag is not here!!!");
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

    private void getSongsList(final int page) {

        // build request url
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .authority(Constants.URLs.HOST)
                .appendPath("hashtag")
                .appendPath(hashtag.replaceFirst("#", ""))
                .appendQueryParameter("page", String.valueOf(page))
                .build();
        String url = builder.toString();

        // define callback
        AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            // parse incoming data
                            JSONArray jsonSongsList = response.getJSONArray("songsList");

                            // fill songsList
                            for (int i = 0; i < jsonSongsList.length(); i++) {
                                songsList.add(Song.createSong(jsonSongsList.getJSONObject(i)));
                            }

                            // set song page related values
                            lastFetchedPage = page;
                            totalPages = response.getInt("totalPages");

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

    private class DetailedSongsListAdapter extends BaseAdapter {
        private final Context mContext;

        DetailedSongsListAdapter(Context context) {
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
                convertView = layoutInflater.inflate(R.layout.list_item_detailed_song, null);

                // inflate views
                final TextView songTitleTextView = (TextView) convertView.findViewById(R.id.list_item_detailed_song_title);
                final TextView artistNameTextView = (TextView) convertView.findViewById(R.id.list_item_detailed_song_artist_name);
                final TextView durationTextView = (TextView) convertView.findViewById(R.id.list_item_detailed_song_duration);
                final TextView playsCountTextView = (TextView) convertView.findViewById(R.id.list_item_detailed_song_plays_count);

                // add them to view holder
                final HashtagActivity.DetailedSongsListAdapter.ViewHolder viewHolder = new HashtagActivity.DetailedSongsListAdapter.ViewHolder(songTitleTextView, artistNameTextView, durationTextView, playsCountTextView);
                convertView.setTag(viewHolder);
            }

            // get views from view holder
            final HashtagActivity.DetailedSongsListAdapter.ViewHolder viewHolder = (HashtagActivity.DetailedSongsListAdapter.ViewHolder) convertView.getTag();

            // set views
            viewHolder.songTitleTextView.setText(song.title);
            viewHolder.artistNameTextView.setText(song.artistName);
            viewHolder.durationTextView.setText(song.durationDesc);
            viewHolder.playsCountTextView.setText(String.valueOf(song.playsCount));

            // load more data
            if (!fetching && lastFetchedPage < totalPages && position >= songsList.size() - 4) {
                getSongsList(lastFetchedPage + 1);
            }

            return convertView;
        }

        // view holder design pattern for performance enhancements
        private class ViewHolder {
            private final TextView songTitleTextView;
            private final TextView artistNameTextView;
            private final TextView durationTextView;
            private final TextView playsCountTextView;

            ViewHolder(TextView songTitleTextView, TextView artistNameTextView, TextView durationTextView, TextView playsCountTextView) {
                this.songTitleTextView = songTitleTextView;
                this.artistNameTextView = artistNameTextView;
                this.durationTextView = durationTextView;
                this.playsCountTextView = playsCountTextView;
            }
        }
    }
}
