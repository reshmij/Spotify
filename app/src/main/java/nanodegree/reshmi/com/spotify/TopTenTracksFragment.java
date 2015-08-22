package nanodegree.reshmi.com.spotify;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapters.ArtistTopTracksListAdapter;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import model.TrackInfo;

/**
 * Created by annupinju on 8/12/2015.
 */
public class TopTenTracksFragment extends Fragment {

    GetArtistTopTracksTask mGetTopTracksTask = null;
    ArtistTopTracksListAdapter mAdapter = null;
    ListView mTopTenTrackList = null;
    String mArtistName = null;
    ArrayList<TrackInfo> mTrackInfoResults = new ArrayList<>();

    public static final int SMALL_THUMBNAIL_WIDTH = 200; //240px
    public static final int LARGE_THUMBNAIL_WIDTH = 640; //640px

    private static final String LOG_TAG = TopTenTracksFragment.class.getSimpleName();
    public static final String TRACK_LIST = "trackList";
    public static final String TRACK_LIST_POS = "trackListPosition";
    public static final String ARTIST_NAME = "artistName";
    private OnTrackSelectedListener mListener = null;

    public static TopTenTracksFragment newInstance(String artistId) {
        TopTenTracksFragment f = new TopTenTracksFragment();

        // Supply artistId input as an argument.
        Bundle args = new Bundle();
        args.putString(ArtistSearchFragment.ARTIST_ID, artistId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnTrackSelectedListener)activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.top_ten_tracks_fragment, container, false);


        int selectPosition = -1;
        if(savedInstanceState != null){
            //Get the last saved list of Top 10 tracts
            mTrackInfoResults = savedInstanceState.getParcelableArrayList(TRACK_LIST);
            selectPosition = savedInstanceState.getInt(TRACK_LIST_POS);
        }

        // Get a handle to the list view
        mTopTenTrackList = (ListView) rootView.findViewById(R.id.list_view_top_ten_tracks);
        mTopTenTrackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mListener.onSelectTrack(mTrackInfoResults,i, mArtistName);
            }
        });

        // Set list adapter
        mAdapter = new ArtistTopTracksListAdapter(getActivity(), mTrackInfoResults);
        mTopTenTrackList.setAdapter(mAdapter);

        if(selectPosition >= 0){
            mTopTenTrackList.setSelection(selectPosition);
        }

        //Make a remote call to fetch the top tracks, if there is no saved instance
        Bundle args = getArguments();
        mArtistName = args.getString(ArtistSearchFragment.ARTIST_NAME);

        if(savedInstanceState == null){

            if(args!=null) {
                String artistId = args.getString(ArtistSearchFragment.ARTIST_ID);
                mGetTopTracksTask = new GetArtistTopTracksTask();
                mGetTopTracksTask.execute(artistId);
            }
            else{
                //There are no args set on the fragment. Do nothing
            }
        }

        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        //Cancel any async task
        if (mGetTopTracksTask != null && !mGetTopTracksTask.isCancelled()) {
            mGetTopTracksTask.cancel(true);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save an instance of the Top 10 tracks list
        outState.putParcelableArrayList(TRACK_LIST, mTrackInfoResults);
    }

    public interface OnTrackSelectedListener{
        public void onSelectTrack(ArrayList<TrackInfo> trackInfoResults, int position , String artistName);
    }


    private class GetArtistTopTracksTask extends AsyncTask<String, Void, Tracks> {

        @Override
        protected Tracks doInBackground(String... strings) {

            Tracks tracks = null;

            try {
                String artistId = strings[0];

                //Call to get the Top 10 tracks for the artist
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotifyService = api.getService();

                Map<String, Object> options = new HashMap<>();
                options.put("country", "US");

                tracks = spotifyService.getArtistTopTrack(artistId, options);

            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }

            return tracks;
        }

        @Override
        protected void onPostExecute(Tracks artistTopTracks) {
            super.onPostExecute(artistTopTracks);

            try {
                mTrackInfoResults.clear();

                if (artistTopTracks.tracks.isEmpty()) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.tracks_not_found), Toast.LENGTH_SHORT).show();
                    mAdapter.clear();
                    return;
                }

                for (Track t : artistTopTracks.tracks) {

                    TrackInfo trackInfo = new TrackInfo(t.name,
                            t.album.name,
                            getSmallThumbnail(t.album.images),
                            getLargeThumbnail(t.album.images),
                            t.preview_url);

                    mTrackInfoResults.add(trackInfo);
                }

                mAdapter.removeAll();
                mAdapter.addAll(mTrackInfoResults);

            } catch (Exception e) {

                Log.e(LOG_TAG, e.getMessage());
            }
        }

        private String getLargeThumbnail(List<Image> images) {
            if ((images == null) || images.isEmpty()) {
                return null;
            }

            for (Image i : images) {
                if (i.width == LARGE_THUMBNAIL_WIDTH) {
                    return i.url;
                }
            }
            // Choose the largest image, which is the first one
            return images.get(0).url;
        }

        private String getSmallThumbnail(List<Image> images) {
            if ((images == null) || images.isEmpty()) {
                return null;
            }

            for (Image i : images) {
                if (i.width == SMALL_THUMBNAIL_WIDTH) {
                    return i.url;
                }
            }
            // Choose the smallest image, which is the last one
            int last = images.size() - 1;
            return images.get(last).url;
        }
    }
}
