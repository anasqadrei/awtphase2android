package com.awtarika.android.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.awtarika.android.app.model.Artist;
import com.awtarika.android.app.util.AwtarikaJsonObjectRequest;
import com.awtarika.android.app.util.Constants;
import com.awtarika.android.app.util.NetworkingSingleton;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistsListActivity extends BaseActivity {

    // state variables  that has to be saved
    private ArrayList<Artist> artistsList = new ArrayList<Artist>();
    private int totalPages = 1;
    private int lastFetchedPage = 0;

    private static final String ARTISTS_LIST_ARRAY_KEY = "artistsList";
    private static final String TOTAL_PAGES_KEY = "totalPages";
    private static final String LAST_FETCHED_PAGE_KEY = "lastFetchedPage";

    // other variables
    private boolean fetching = false;
    private ArtistsListAdapter mArtistsListAdapter;
    private static final String DEFAULT_SORT = "-songsCount";

    private static final String TAG = ArtistsListActivity.class.getSimpleName();

    // TODO: 15/11/16 Caching on server for volley to cache
    // TODO: 15/11/16 Log Entries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists_list);
        setTitle("المطربين");
        gaScreenCategory = "Artists List";

        // it means artistsList array is empty, so fetch artists
        if (savedInstanceState == null) {
            getArtistsList(lastFetchedPage + 1, DEFAULT_SORT);
        }

        mArtistsListAdapter = new ArtistsListAdapter(this);

        final GridView gridView = (GridView) findViewById(R.id.activity_artists_list_artists_list);
        gridView.setAdapter(mArtistsListAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // pass selected artist to the next view
                Intent artistIntent = new Intent(parent.getContext(), ArtistActivity.class);
                artistIntent.putExtra(Artist.class.getSimpleName(), (Artist) parent.getItemAtPosition(position));
                startActivity(artistIntent);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save state
        outState.putInt(TOTAL_PAGES_KEY, totalPages);
        outState.putInt(LAST_FETCHED_PAGE_KEY, lastFetchedPage);
        outState.putParcelableArrayList(ARTISTS_LIST_ARRAY_KEY, artistsList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // restore state
        totalPages = savedInstanceState.getInt(TOTAL_PAGES_KEY);
        lastFetchedPage = savedInstanceState.getInt(LAST_FETCHED_PAGE_KEY);
        artistsList = savedInstanceState.getParcelableArrayList(ARTISTS_LIST_ARRAY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // cancel network activities
        fetching = false;
        NetworkingSingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    private void getArtistsList(final int page, String sort) {

        // build request url
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .encodedAuthority(Constants.URLs.HOST)
                .appendPath("artists-list")
                .appendQueryParameter("sort", sort)
                .appendQueryParameter("page", String.valueOf(page))
                .build();
        String url = builder.toString();

        // define callback
        AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest (Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            // parse incoming data
                            JSONArray jsonArtistsList = response.getJSONArray("artistsList");

                            // fill artistsList
                            for (int i = 0; i < jsonArtistsList.length(); i++) {
                                artistsList.add(Artist.createArtist(jsonArtistsList.getJSONObject(i)));
                            }

                            // set artist page related values
                            lastFetchedPage = page;
                            totalPages = response.getInt("totalPages");

                            // signal reload data
                            mArtistsListAdapter.notifyDataSetChanged();

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

    private class ArtistsListAdapter extends BaseAdapter {
        private final Context mContext;

        ArtistsListAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return artistsList.size();
        }

        @Override
        public Object getItem(int position) {
            return artistsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return artistsList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // item variable
            final Artist artist = artistsList.get(position);

            // in case it was recycled, recreate it
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.grid_item_artist, null);

                // inflate views
                final TextView artistNameTextView = (TextView) convertView.findViewById(R.id.grid_item_artist_name);
                final CircleImageView artistImageView = (CircleImageView) convertView.findViewById(R.id.grid_item_artist_image);

                // add them to view holder
                final ViewHolder viewHolder = new ViewHolder(artistNameTextView, artistImageView);
                convertView.setTag(viewHolder);
            }

            // get views from view holder
            final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

            // set views
            viewHolder.artistNameTextView.setText(artist.name);
            Glide.with(mContext)
                    .load(artist.imageURL)
                    .centerCrop()
                    .dontAnimate()
                    .into(viewHolder.artistImageView);

            // load more data
            if (!fetching && lastFetchedPage < totalPages && position >= artistsList.size() - 4) {
                getArtistsList(lastFetchedPage + 1, DEFAULT_SORT);
            }

            return convertView;
        }

        // view holder design pattern for performance enhancements
        private class ViewHolder {
            private final TextView artistNameTextView;
            private final CircleImageView artistImageView;

            ViewHolder(TextView artistNameTextView, CircleImageView artistImageView) {
                this.artistNameTextView = artistNameTextView;
                this.artistImageView = artistImageView;
            }
        }
    }
}
