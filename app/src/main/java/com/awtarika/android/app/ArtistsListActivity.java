package com.awtarika.android.app;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ArtistsListActivity extends AppCompatActivity {

    private ArtistsListAdapter mArtistsListAdapter;
    private int totalPages = 1;
    private int lastFetchedPage = 0;
    private boolean fetching = false;
    private String defaultSort = "-songsCount";
    private static final String TAG = ArtistsListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists_list);
        setTitle("المطربين");

        mArtistsListAdapter = new ArtistsListAdapter(this, new ArrayList<Artist>());

        final GridView gridView = (GridView) findViewById(R.id.grid_view_artists_list);
        gridView.setAdapter(mArtistsListAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!fetching) {
            getArtistsList(1, defaultSort);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        NetworkingSingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    private void getArtistsList(final int page, String sort) {

        fetching = true;

        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .authority(Constants.URLs.HOST)
                .appendPath("artists-list")
                .appendQueryParameter("sort", sort)
                .appendQueryParameter("page", String.valueOf(page))
                .build();
        String url = builder.toString();

        AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest (Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            JSONArray jsonArtistsList = response.getJSONArray("artistsList");

                            for (int i = 0; i < jsonArtistsList.length(); i++) {
                                mArtistsListAdapter.artists.add(Artist.createArtist(jsonArtistsList.getJSONObject(i)));
                            }

                            lastFetchedPage = page;
                            totalPages = response.getInt("totalPages");

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

        NetworkingSingleton.getInstance(this).addToRequestQueue(jsObjRequest);

    }

    private class ArtistsListAdapter extends BaseAdapter {
        private final Context mContext;
        ArrayList<Artist> artists;

        ArtistsListAdapter(Context context, ArrayList<Artist> artists) {
            this.mContext = context;
            this.artists = artists;
        }

        @Override
        public int getCount() {
            return artists.size();
        }

        @Override
        public Object getItem(int position) {
            return artists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return artists.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Artist artist = artists.get(position);

            // in case it was recycled, recreate it
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.grid_item_artist, null);
            }

            // TextView
            final TextView artistNameTextView = (TextView) convertView.findViewById(R.id.artist_name);
            artistNameTextView.setText(artist.name);

            // ImageView
            final CircleImageView artistImageView = (CircleImageView) convertView.findViewById(R.id.artist_image);
            Glide.with(mContext)
                    .load(artist.imageURL)
                    .centerCrop()
                    .dontAnimate()
                    .into(artistImageView);

            //load more data
            if (!fetching && lastFetchedPage < totalPages && position >= artists.size() - 4) {
                Log.v(TAG, "fetching at pos: " + position);
                getArtistsList(lastFetchedPage + 1, defaultSort);
            }

            return convertView;
        }
    }
}
