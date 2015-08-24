package nanodegree.reshmi.com.spotify;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import adapters.ArtistSearchListAdapter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;
import model.ArtistInfo;

/**
 * Created by annupinju on 8/12/2015.
 */
public class ArtistSearchFragment extends Fragment {
    public static final String ARTIST_ID = "artistId";
    public static final String ARTIST_NAME = "artistName";

    private static final String ARTISTS_LIST = "artistsList";
    private static final String ARTIST_QUERY = "artistQuery";
    private static final String LIST_POSITION = "listPosition";

    private String LOG_TAG = ArtistSearchFragment.class.getSimpleName();
    private ArtistSearchListAdapter mAdapter = null;
    private SearchArtistFromSpotifyTask mSearchArtistTask = null;
    private SearchView mSearchView = null;
    private ListView mListView = null;
    private String mLastQuery = null;
    private Context mContext = null;
    private OnListItemClickListener mListItemClickListener = null;
    private int mPosition = ListView.INVALID_POSITION;
    private ArrayList<ArtistInfo> mArtistInfoList = new ArrayList<>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mListItemClickListener = (OnListItemClickListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.artist_search_fragment, container, false);

        //Get a handle to the views
        mListView = (ListView) rootView.findViewById(R.id.list_view_artist_search_results);
        mSearchView = (SearchView) rootView.findViewById(R.id.search_view_artist_search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setIconifiedByDefault(false);

        if (savedInstanceState != null) {
            mArtistInfoList = savedInstanceState.getParcelableArrayList(ARTISTS_LIST);
            mLastQuery = savedInstanceState.getString(ARTIST_QUERY);
            mPosition = savedInstanceState.getInt(LIST_POSITION, mListView.getSelectedItemPosition());
        } else {
            mLastQuery = null;
        }

        mAdapter = new ArtistSearchListAdapter(mContext, mArtistInfoList);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtistInfo artist = (ArtistInfo) mAdapter.getItem(position);
                mListItemClickListener.onItemClick(artist.getName(), artist.getId());
                mPosition = position;
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                doMySearch(s);
                return true;
            }
        });

        if(savedInstanceState!=null) {
            //Highlight  the last selected item on the list
            if (mPosition != ListView.INVALID_POSITION) {
                mListView.smoothScrollToPosition(mPosition);
            }
        }

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();
        //Highlight  the last selected item on the list
        //if (mPosition != ListView.INVALID_POSITION) {
        //    mListView.smoothScrollToPosition(mPosition);
        //}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the instance of the search results and query
        if (mArtistInfoList != null) {
            outState.putParcelableArrayList(ARTISTS_LIST, mArtistInfoList);
            outState.putString(ARTIST_QUERY, mSearchView.getQuery().toString());

            if (mPosition != ListView.INVALID_POSITION) {
                outState.putInt(LIST_POSITION, mPosition);
            }
        }
    }

    public void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
    }

    private void doMySearch(String query) {
        if (query != null && (query.length() > 0)) {

            if (query.equals(mLastQuery)) {
                //Query has not changed. Return here.
                return;
            }
            //If an async task is already running, cancel it
            if (mSearchArtistTask != null && !mSearchArtistTask.isCancelled()) {
                mSearchArtistTask.cancel(true);
            }

            mSearchArtistTask = new SearchArtistFromSpotifyTask();
            mSearchArtistTask.execute(query);

        } else {
            //Search field is empty. Clear the list
            mAdapter.clear();
            clearListItemSelection();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Cancel any async task since we're leaving
        if (mSearchArtistTask != null && !mSearchArtistTask.isCancelled()) {
            mSearchArtistTask.cancel(true);
        }
    }

    private class SearchArtistFromSpotifyTask extends AsyncTask<String, Void, List<Artist>> {
        @Override
        protected List<Artist> doInBackground(String... strings) {
            List<Artist> artistsList = null;
            try {
                String query = strings[0];
                //Make a remote web call to get the artist search results
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotifyService = api.getService();
                ArtistsPager results = spotifyService.searchArtists(query);
                artistsList = results.artists.items;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return artistsList;
        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
            super.onPostExecute(artists);
            try {
                mArtistInfoList.clear();
                clearListItemSelection();
                //If search result is empty, clear the list and display a toast
                if (artists.isEmpty()) {
                    mAdapter.clear();
                    Toast.makeText(getActivity(), getResources().getString(R.string.artist_not_found), Toast.LENGTH_SHORT).show();

                    return;
                }
                //Search returned valid results. Extract the required fields from it.
                for (Artist a : artists) {
                    ArtistInfo artistInfo = new ArtistInfo(a.id, a.name, getThumbnail(a.images));
                    mArtistInfoList.add(artistInfo);
                }
                mAdapter.removeAll(); // Remove the existing data in the list
                mAdapter.addAll(mArtistInfoList);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        private String getThumbnail(List<Image> images) {
            //Method to get the smallest image in the list, which is the last one
            if ((images == null) || images.isEmpty()) {
                return null;
            }

            // Choose the smallest image
            int last = images.size() - 1;
            return images.get(last).url;
        }
    }

    public interface OnListItemClickListener {
        public void onItemClick(String artistName, String artistId);
    }


    private void clearListItemSelection( ){
        mListView.clearChoices();
        mAdapter.notifyDataSetChanged();
        mPosition = ListView.INVALID_POSITION;
    }
}
