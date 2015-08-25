package nanodegree.reshmi.com.spotify;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import model.TrackInfo;

/**
 * Created by annupinju on 8/12/2015.
 */
public class ArtistSearchActivity extends AppCompatActivity implements ArtistSearchFragment.OnListItemClickListener, TopTenTracksFragment.OnTrackSelectedListener {

    private String LOG_TAG = ArtistSearchActivity.class.getSimpleName();
    ArtistSearchFragment mArtistSearchFragment;
    boolean mTwoPane = false;
    boolean mShowNowPlaying = false;
    LocalBroadcastManager mLocalBroadcastManager;
    Bundle mExtras = null;
    ShareActionProvider mShareActionProvider = null;

    public static String TOP_TEN_FRAGMENT_TAG = "toptentracks";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(MusicPlayerService.PLAYBACK_PROGRESS_BROADCAST_EVENT)
                    || (intent.getAction().equals(MusicPlayerService.PLAYBACK_PAUSE_BROADCAST_EVENT))) {
                mShowNowPlaying = true;
                invalidateOptionsMenu();
                mExtras = intent.getExtras();
            } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT)) {
                mShowNowPlaying = false;
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_search_layout);

        mArtistSearchFragment = (ArtistSearchFragment) getFragmentManager().findFragmentById(R.id.artist_search_fragment);
        mArtistSearchFragment.handleIntent(getIntent());

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        View topTenTracksView = findViewById(R.id.top_ten_tracks_container);
        mTwoPane = topTenTracksView != null && topTenTracksView.getVisibility() == View.VISIBLE;

        if (mTwoPane) {

            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.top_ten_tracks_container, new TopTenTracksFragment(), TOP_TEN_FRAGMENT_TAG)
                        .commit();

            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mArtistSearchFragment instanceof ArtistSearchFragment) {
            mArtistSearchFragment.handleIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_PROGRESS_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_PAUSE_BROADCAST_EVENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    public void onItemClick(String artistName, String artistId) {

        if (mTwoPane) {
            //Replace the top ten tracks fragment on the right pane
            TopTenTracksFragment topTenTracksFragment = TopTenTracksFragment.newInstance(artistId);

            getFragmentManager().beginTransaction()
                    .replace(R.id.top_ten_tracks_container, topTenTracksFragment, TOP_TEN_FRAGMENT_TAG)
                    .commit();

            setUpActionBarSubtitle(artistName);
        } else {
            //Launch the TopTenTracksActivity
            Intent intent = new Intent(this, TopTenTracksActivity.class);
            intent.putExtra(ArtistSearchFragment.ARTIST_ID, artistId);
            intent.putExtra(ArtistSearchFragment.ARTIST_NAME, artistName);
            startActivity(intent);
        }
    }

    private void setUpActionBarSubtitle(String artistName) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(artistName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem nowPlayingItem = menu.findItem(R.id.action_now_playing);
        MenuItem shareItem = menu.findItem(R.id.action_share_url);
        if(shareItem != null){

            mShareActionProvider = (ShareActionProvider)MenuItemCompat.getActionProvider(shareItem);
            // Set the share Intent
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(getDefaultIntent());
            }
        }

        if (mShowNowPlaying) {
            nowPlayingItem.setVisible(true);
            shareItem.setVisible(true);
        } else {
            nowPlayingItem.setVisible(false);
            shareItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_now_playing) {

            if (mExtras != null) {
                ArrayList<TrackInfo> list = mExtras.getParcelableArrayList(MusicPlayerService.PLAY_LIST_EXTRA);
                int selectedIndex = mExtras.getInt(MusicPlayerService.SELECTED_INDEX_EXTRA);
                String artistName = mExtras.getString(MusicPlayerService.ARTIST_NAME_EXTRA);
                TrackInfo nowPlayingTrack = mExtras.getParcelable(MusicPlayerService.NOW_PLAYING_EXTRA);

                if (mTwoPane) {
                    // The device is using a large layout, so show the MusicPlayer fragment as a dialog
                    FragmentManager fragmentManager = getFragmentManager();
                    MusicPlayerFragment newFragment = MusicPlayerFragment.newInstance(list, selectedIndex, artistName, nowPlayingTrack);
                    newFragment.show(fragmentManager, "dialog");
                } else {
                    Intent intent = new Intent(this, MusicPlayerActivity.class);
                    intent.putExtra(TopTenTracksFragment.TRACK_LIST, list);
                    intent.putExtra(TopTenTracksFragment.SELECTED_TRACK_INDEX, selectedIndex);
                    intent.putExtra(TopTenTracksFragment.ARTIST_NAME, artistName);
                    intent.putExtra(TopTenTracksFragment.NOW_PLAYING_TRACK, nowPlayingTrack);
                    startActivity(intent);
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectTrack(ArrayList<TrackInfo> trackInfoResults, int position, String artistName) {

        if (mTwoPane) {
            // The device is using a large layout, so show the MusicPlayer fragment as a dialog
            FragmentManager fragmentManager = getFragmentManager();
            MusicPlayerFragment newFragment = MusicPlayerFragment.newInstance(trackInfoResults, position, artistName, null);
            newFragment.show(fragmentManager, "dialog");
        }
    }

    private Intent getDefaultIntent( ){

        Intent intent = null;
        try {

            TrackInfo nowPlayingTrack = mExtras.getParcelable(MusicPlayerService.NOW_PLAYING_EXTRA);

            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT,nowPlayingTrack.getExternalUrl());
        }
        catch (Exception e){
            Log.e(LOG_TAG, e.getMessage() );
        }

        return intent;
    }

}
