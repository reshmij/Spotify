package nanodegree.reshmi.com.spotify;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
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
 * Created by annupinju on 6/30/2015.
 */
public class TopTenTracksActivity extends AppCompatActivity {

    GetArtistTopTracksTask mGetTopTracksTask = null;
    ArtistTopTracksListAdapter mAdapter = null;
    ListView mTopTenTrackList = null;
    ArrayList<TrackInfo> mTrackInfoResults = new ArrayList<>();

    public static final int SMALL_THUMBNAIL_WIDTH = 200; //240px
    public static final int LARGE_THUMBNAIL_WIDTH = 640; //640px

    private static final String LOG_TAG = TopTenTracksActivity.class.getSimpleName();
    private static final String TRACK_LIST = "trackList";
    private static final String TRACK_LIST_POS = "trackListPosition";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_top_ten_tracks);

        // Set up the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.top_ten_tracks);
        actionBar.setSubtitle(getIntent().getStringExtra(MainActivity.ARTIST_NAME));
        actionBar.setDisplayHomeAsUpEnabled(true);

        int selectPosition = -1;
        if(savedInstanceState != null){
            //Get the last saved list of Top 10 tracts
            mTrackInfoResults = savedInstanceState.getParcelableArrayList(TRACK_LIST);
            selectPosition = savedInstanceState.getInt(TRACK_LIST_POS);
        }

        // Get a handle to the list view
        mTopTenTrackList = (ListView) findViewById(R.id.list_view_top_ten_tracks);

        // Set list adapter
        mAdapter = new ArtistTopTracksListAdapter(this, mTrackInfoResults);
        mTopTenTrackList.setAdapter(mAdapter);

        if(selectPosition >= 0){
            mTopTenTrackList.setSelection(selectPosition);
        }

        if(savedInstanceState == null){
            //Make a remote call to fetch the top tracks, if there is no saved instance
            String artistId = getIntent().getStringExtra(MainActivity.ARTIST_ID);
            mGetTopTracksTask = new GetArtistTopTracksTask();
            mGetTopTracksTask.execute(artistId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Cancel any async task
        if (mGetTopTracksTask != null && !mGetTopTracksTask.isCancelled()) {
            mGetTopTracksTask.cancel(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save an instance of the Top 10 tracks list
        outState.putParcelableArrayList(TRACK_LIST, mTrackInfoResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // App icon in action bar clicked; navigate to home
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    Toast.makeText(TopTenTracksActivity.this, getResources().getString(R.string.tracks_not_found), Toast.LENGTH_SHORT).show();
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


