package com.awtarika.android.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.awtarika.android.app.model.Artist;
import com.awtarika.android.app.model.SearchResult;
import com.awtarika.android.app.model.Song;
import com.awtarika.android.app.util.AwtarikaJsonArrayRequest;
import com.awtarika.android.app.util.AwtarikaJsonObjectRequest;
import com.awtarika.android.app.util.Constants;
import com.awtarika.android.app.util.LoggerSingleton;
import com.awtarika.android.app.util.NetworkingSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SearchActivity extends BaseActivity {

    // state variables  that has to be saved
    private String searchTerm;
    private ArrayList<SearchResult> searchResultsList = new ArrayList<>();
    private int totalResults = 0;
    private int totalPages = 1;
    private int lastFetchedPage = 0;

    private static final String SEARCH_TERM_KEY = "searchTerm";
    private static final String SEARCH_RESULTS_LIST_ARRAY_KEY = "searchResultsList";
    private static final String TOTAL_RESULTS_KEY = "totalResults";
    private static final String TOTAL_PAGES_KEY = "totalPages";
    private static final String LAST_FETCHED_PAGE_KEY = "lastFetchedPage";

    // other variables
    private boolean fetching = false;
    private SearchResultsListAdapter mSearchResultsListAdapter;

    private TextView summeryTextView;

    private static final String TAG = SearchActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setTitle("بحث");
        gaScreenCategory = "Search";

        // to display summery
        summeryTextView = (TextView) findViewById(R.id.activity_search_summery);

        // search results
        mSearchResultsListAdapter = new SearchResultsListAdapter(this);
        final ListView listView = (ListView) findViewById(R.id.activity_search_songs_list);
        listView.setAdapter(mSearchResultsListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {

                // show spinner
                final ProgressDialog plzWait = new ProgressDialog(parent.getContext());
                plzWait.setIndeterminate(false);
                plzWait.setCancelable(true);
                plzWait.setMessage("لحظة");
                plzWait.show();

                // get the current item from search results
                final SearchResult currentSearchResult = (SearchResult) parent.getItemAtPosition(position);

                // build request url
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(Constants.URLs.PROTOCOL)
                        .encodedAuthority(Constants.URLs.HOST);
                if (currentSearchResult.mType.equals(Constants.ElasticSearch.Type.SONGS)) {
                    builder.appendPath("song");
                } else if (currentSearchResult.mType.equals(Constants.ElasticSearch.Type.ARTISTS)) {
                    builder.appendPath("artist");
                }
                builder.appendPath(String.valueOf(currentSearchResult.mId))
                        .appendPath(currentSearchResult.mSlug)
                        .build();
                String url = builder.toString();

                // define callback
                AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {

                                // hide spinner
                                plzWait.dismiss();

                                try {
                                    if (currentSearchResult.mType.equals(Constants.ElasticSearch.Type.SONGS)) {
                                        // pass selected song to the next view
                                        Intent songIntent = new Intent(parent.getContext(), SongActivity.class);
                                        songIntent.putExtra(Song.class.getSimpleName(), Song.createSong(response.getJSONObject("song")));
                                        startActivity(songIntent);
                                    } else if (currentSearchResult.mType.equals(Constants.ElasticSearch.Type.ARTISTS)) {
                                        // pass selected artist to the next view
                                        Intent artistIntent = new Intent(parent.getContext(), ArtistActivity.class);
                                        artistIntent.putExtra(Artist.class.getSimpleName(), Artist.createArtist(response.getJSONObject("artist")));
                                        startActivity(artistIntent);
                                    }
                                } catch (JSONException e) {
                                    String errorMessage = TAG + " onItemClick(" + currentSearchResult.mMetaTitle + "). Server Error: " + e.getMessage();
                                    LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                                }
                            }
                        }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // hide spinner
                        plzWait.dismiss();

                        String errorMessage = TAG + " onItemClick(" + currentSearchResult.mMetaTitle + "). Server Error: ";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage += new String(error.networkResponse.data);
                        }
                        LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                    }
                });
                jsObjRequest.setTag(TAG);

                // call the request url
                NetworkingSingleton.getInstance(parent.getContext()).addToRequestQueue(jsObjRequest);
            }
        });

        // autocomplete
        AutoCompleteListAdapter mAutoCompleteListAdapter = new AutoCompleteListAdapter(this, android.R.layout.simple_dropdown_item_1line);
        AutoCompleteTextView searchBoxTextView = (AutoCompleteTextView) findViewById(R.id.activity_search_auto_complete_search_box);
        searchBoxTextView.setAdapter(mAutoCompleteListAdapter);

        // search button
        searchBoxTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // set search term for activity
                    searchTerm = textView.getText().toString();

                    // reset
                    searchResultsList.clear();
                    totalPages = 1;
                    lastFetchedPage = 0;

                    // search
                    getSearchResultList(lastFetchedPage + 1);

                    // hide keyboard
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                    handled = true;
                }
                return handled;
            }
        });
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save state
        outState.putString(SEARCH_TERM_KEY, searchTerm);
        outState.putInt(TOTAL_RESULTS_KEY, totalResults);
        outState.putInt(TOTAL_PAGES_KEY, totalPages);
        outState.putInt(LAST_FETCHED_PAGE_KEY, lastFetchedPage);
        outState.putParcelableArrayList(SEARCH_RESULTS_LIST_ARRAY_KEY, searchResultsList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // restore state
        searchTerm = savedInstanceState.getString(SEARCH_TERM_KEY);
        totalResults = savedInstanceState.getInt(TOTAL_RESULTS_KEY);
        totalPages = savedInstanceState.getInt(TOTAL_PAGES_KEY);
        lastFetchedPage = savedInstanceState.getInt(LAST_FETCHED_PAGE_KEY);
        searchResultsList = savedInstanceState.getParcelableArrayList(SEARCH_RESULTS_LIST_ARRAY_KEY);

        summeryTextView.setText(String.format(getResources().getString(R.string.search_summery), totalResults));
    }

    @Override
    protected void onStop() {
        super.onStop();

        // cancel network activities
        fetching = false;
        NetworkingSingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu
        super.onCreateOptionsMenu(menu);

        //hide everything
        menu.findItem(R.id.menu_share).setVisible(false);
        menu.findItem(R.id.menu_flag).setVisible(false);
        menu.findItem(R.id.menu_search).setVisible(false);

        return true;
    }

    private class AutoCompleteListAdapter extends ArrayAdapter implements Filterable {

        private final Context mContext;
        private ArrayList<String> mAutoCompleteList;

        AutoCompleteListAdapter(Context context, int resource) {
            super(context, resource);
            mContext = context;
            mAutoCompleteList = new ArrayList<String>();
        }

        @Override
        public int getCount() {
            return mAutoCompleteList.size();
        }

        @Override
        public String getItem(int index) {
            return mAutoCompleteList.get(index);
        }

        @NonNull
        @Override
        public Filter getFilter() {

            Filter myFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    if (charSequence != null) {
                        getAutoCompleteSuggestions(charSequence.toString());
                    }

                    // useless in our case
                    return null;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    // notifyDataSetChanged has been already done at getAutoCompleteSuggestions
                }
            };
            return myFilter;
        }

        private void getAutoCompleteSuggestions(final String query){
            // build request url
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(Constants.URLs.PROTOCOL)
                    .encodedAuthority(Constants.URLs.HOST)
                    .appendPath("search")
                    .appendPath("autocomplete-suggestions")
                    .appendQueryParameter("limit", String.valueOf(7))
                    .appendQueryParameter("q", query)
                    .build();
            String url = builder.toString();

            // define callback
            AwtarikaJsonArrayRequest jsArrRequest = new AwtarikaJsonArrayRequest (Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {

                        @Override
                        public void onResponse(JSONArray jsonSuggestionsList) {

                            try {

                                // fill mAutoCompleteList
                                mAutoCompleteList.clear();
                                for (int i = 0; i < jsonSuggestionsList.length(); i++) {
                                    mAutoCompleteList.add(jsonSuggestionsList.getJSONObject(i).getJSONObject("_source").getString("metaTitle"));
                                }

                                // signal reload data
                                notifyDataSetChanged();

                            } catch (JSONException e) {
                                String errorMessage = TAG + " getAutoCompleteSuggestions(" + query + "). Server Error: " + e.getMessage();
                                LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                            }
                        }
                    }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                String errorMessage = TAG + " getAutoCompleteSuggestions(" + query + "). Server Error: ";
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    errorMessage += new String(error.networkResponse.data);
                }
                LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                }
            });
            jsArrRequest.setTag(TAG);

            // call the request url
            NetworkingSingleton.getInstance(mContext).addToRequestQueue(jsArrRequest);
        }
    }

    private void getSearchResultList(final int page) {

        final int pageSize = 10;

        // build request url
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Constants.URLs.PROTOCOL)
                .encodedAuthority(Constants.URLs.HOST)
                .appendPath("search")
                .appendQueryParameter("page", String.valueOf(page))
                .appendQueryParameter("pageSize", String.valueOf(pageSize))
                .appendQueryParameter("q", searchTerm)
                .build();
        String url = builder.toString();

        // define callback
        AwtarikaJsonObjectRequest jsObjRequest = new AwtarikaJsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            // parse incoming data
                            JSONArray jsonSearchResultsList = response.getJSONArray("searchResultsList");

                            // fill searchResultsList
                            for (int i = 0; i < jsonSearchResultsList.length(); i++) {
                                // create a search result object
                                SearchResult searchResult = new SearchResult();
                                searchResult.mId = jsonSearchResultsList.getJSONObject(i).getInt("_id");
                                searchResult.mType = jsonSearchResultsList.getJSONObject(i).getString("_type");
                                searchResult.mSlug = jsonSearchResultsList.getJSONObject(i).getJSONObject("_source").getString("slug");
                                searchResult.mMetaTitle = jsonSearchResultsList.getJSONObject(i).getJSONObject("_source").getString("metaTitle");
                                if (searchResult.mType.equals(Constants.ElasticSearch.Type.SONGS)) {
                                    searchResult.mDuration = jsonSearchResultsList.getJSONObject(i).getJSONObject("_source").getString("durationDesc");
                                }

                                // append to array
                                searchResultsList.add(searchResult);
                            }

                            // set related values
                            totalResults = response.getInt("totalResults");
                            totalPages = (int) Math.ceil(totalResults/(double) pageSize);
                            lastFetchedPage = page;

                            // summery
                            summeryTextView.setText(String.format(getResources().getString(R.string.search_summery), totalResults));

                            // signal reload data
                            mSearchResultsListAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            String errorMessage = TAG + " getSearchResultList(" + page + "). Server Error: " + e.getMessage();
                            LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                        } finally {
                            fetching = false;
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = TAG + " getSearchResultList(" + page + "). Server Error: ";
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    errorMessage += new String(error.networkResponse.data);
                }
                LoggerSingleton.getInstance(getApplicationContext()).log(errorMessage);
                fetching = false;
            }
        });
        jsObjRequest.setTag(TAG);

        // call the request url
        fetching = true;
        NetworkingSingleton.getInstance(this).addToRequestQueue(jsObjRequest);

    }

    private class SearchResultsListAdapter extends BaseAdapter {
        private final Context mContext;

        SearchResultsListAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return searchResultsList.size();
        }

        @Override
        public Object getItem(int position) {
            return searchResultsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // item variable
            final SearchResult aSearchResult = searchResultsList.get(position);

            // in case it was recycled, recreate it
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.list_item_search_result, null);

                // inflate views
                final TextView metaTitleTextView = (TextView) convertView.findViewById(R.id.list_item_search_result_meta_title);
                final TextView durationTextView = (TextView) convertView.findViewById(R.id.list_item_search_result_song_duration);
                final ImageView durationIconImageView = (ImageView) convertView.findViewById(R.id.list_item_search_result_duration_icon);

                // add them to view holder
                final SearchResultsListAdapter.ViewHolder viewHolder = new SearchResultsListAdapter.ViewHolder(metaTitleTextView, durationTextView, durationIconImageView);
                convertView.setTag(viewHolder);
            }

            // get views from view holder
            final SearchResultsListAdapter.ViewHolder viewHolder = (SearchResultsListAdapter.ViewHolder) convertView.getTag();

            // set views
            viewHolder.metaTitleTextView.setText(aSearchResult.mMetaTitle);
            viewHolder.durationTextView.setText(aSearchResult.mDuration);
            if (aSearchResult.mType.equals(Constants.ElasticSearch.Type.ARTISTS)) {
                viewHolder.durationIconImageView.setVisibility(View.INVISIBLE);
            } else if (aSearchResult.mType.equals(Constants.ElasticSearch.Type.SONGS)) {
                viewHolder.durationIconImageView.setVisibility(View.VISIBLE);
            }

            // load more data
            if (!fetching && lastFetchedPage < totalPages && position >= searchResultsList.size() - 4) {
                getSearchResultList(lastFetchedPage + 1);
            }

            return convertView;
        }

        // view holder design pattern for performance enhancements
        private class ViewHolder {
            private final TextView metaTitleTextView;
            private final TextView durationTextView;
            private final ImageView durationIconImageView;

            ViewHolder(TextView metaTitleTextView, TextView durationTextView, ImageView durationIconImageView) {
                this.metaTitleTextView = metaTitleTextView;
                this.durationTextView = durationTextView;
                this.durationIconImageView = durationIconImageView;
            }
        }
    }
}
