package nanodegree.reshmi.com.spotify;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
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
public class TopTenTracksActivity extends AppCompatActivity implements TopTenTracksFragment.OnTrackSelectedListener {

    private String LOG_TAG = TopTenTracksActivity.class.getSimpleName();
    private boolean mShowNowPlaying = false;
    Bundle mExtras = null;
    LocalBroadcastManager mLocalBroadcastManager;
    ShareActionProvider mShareActionProvider = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(MusicPlayerService.PLAYBACK_PROGRESS_BROADCAST_EVENT) ||
                    intent.getAction().equals(MusicPlayerService.PLAYBACK_PAUSE_BROADCAST_EVENT)) {
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten_tracks);

        if (savedInstanceState == null) {

            TopTenTracksFragment topTenTracksFragment = new TopTenTracksFragment();
            Bundle args = getIntent().getExtras();
            if (args != null) topTenTracksFragment.setArguments(args);
            getFragmentManager().beginTransaction().add(R.id.top_ten_tracks_container, (Fragment) topTenTracksFragment).commit();
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Set up the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.top_ten_tracks);
        actionBar.setSubtitle(getIntent().getStringExtra(ArtistSearchFragment.ARTIST_NAME));
        actionBar.setDisplayHomeAsUpEnabled(true);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // App icon in action bar clicked; navigate to home
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.action_now_playing:
                if (mExtras != null) {
                    ArrayList<TrackInfo> list = mExtras.getParcelableArrayList(MusicPlayerService.PLAY_LIST_EXTRA);
                    int selected_index = mExtras.getInt(MusicPlayerService.SELECTED_INDEX_EXTRA);
                    String artistName = mExtras.getString(MusicPlayerService.ARTIST_NAME_EXTRA);
                    TrackInfo nowPlayingTrack = mExtras.getParcelable(MusicPlayerService.NOW_PLAYING_EXTRA);

                    //This activity is created only on a phone, so call MusicPlayerActivity
                    Intent intent = new Intent(this, MusicPlayerActivity.class);
                    intent.putExtra(TopTenTracksFragment.TRACK_LIST, list);
                    intent.putExtra(TopTenTracksFragment.SELECTED_TRACK_INDEX, selected_index);
                    intent.putExtra(TopTenTracksFragment.ARTIST_NAME, artistName);
                    intent.putExtra(TopTenTracksFragment.NOW_PLAYING_TRACK, nowPlayingTrack);
                    startActivity(intent);
                }
                return true;

            case R.id.action_settings:
                {
                    Intent intent = new Intent( this, SetPreferencesActivity.class);
                    startActivity(intent);
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem nowPlayingItem = menu.findItem(R.id.action_now_playing);
        MenuItem shareItem = menu.findItem(R.id.action_share_url);
        if(shareItem != null){

            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
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
        return true;
    }

    @Override
    public void onSelectTrack(ArrayList<TrackInfo> trackInfoResults, int position, String artistName) {
        // If this callback is called, it implies this is a single pane layout
        //So show the Music Player Fragment embedded within an activity
        Intent intent = new Intent(this, MusicPlayerActivity.class);
        intent.putExtra(TopTenTracksFragment.TRACK_LIST, trackInfoResults);
        intent.putExtra(TopTenTracksFragment.SELECTED_TRACK_INDEX, position);
        intent.putExtra(TopTenTracksFragment.ARTIST_NAME, artistName);
        startActivity(intent);
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
