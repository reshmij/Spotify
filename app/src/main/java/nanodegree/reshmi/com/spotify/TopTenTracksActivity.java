package nanodegree.reshmi.com.spotify;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
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
public class TopTenTracksActivity extends AppCompatActivity implements TopTenTracksFragment.OnTrackSelectedListener {

    private String LOG_TAG = TopTenTracksActivity.class.getSimpleName();

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

        // Set up the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.top_ten_tracks);
        actionBar.setSubtitle(getIntent().getStringExtra(ArtistSearchFragment.ARTIST_NAME));
        actionBar.setDisplayHomeAsUpEnabled(true);

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

    @Override
    public void onSelectTrack() {
        // If this callback is called, it implies this is a single pane layout
        //So show the Music Player Fragment embedded within an activity
        Intent intent = new Intent(this, MusicPlayerActivity.class);
        startActivity(intent);
    }
}
