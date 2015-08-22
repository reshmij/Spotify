package nanodegree.reshmi.com.spotify;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

import model.TrackInfo;
import retrofit.http.HEAD;

/**
 * Created by annupinju on 8/12/2015.
 */
public class ArtistSearchActivity extends AppCompatActivity implements ArtistSearchFragment.OnListItemClickListener, TopTenTracksFragment.OnTrackSelectedListener {

    ArtistSearchFragment mArtistSearchFragment;
    boolean mTwoPane = false;

    public static String TOP_TEN_FRAGMENT_TAG = "toptentracks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_search_layout);

        mArtistSearchFragment = (ArtistSearchFragment) getFragmentManager().findFragmentById(R.id.artist_search_fragment);
        mArtistSearchFragment.handleIntent(getIntent());

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
        return true;
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectTrack(ArrayList<TrackInfo> trackInfoResults, int position, String artistName ){

        if (mTwoPane) {
            // The device is using a large layout, so show the MusicPlayer fragment as a dialog
            FragmentManager fragmentManager = getFragmentManager();
            MusicPlayerFragment newFragment = MusicPlayerFragment.newInstance(trackInfoResults, position, artistName);
            newFragment.show(fragmentManager, "dialog");
        }
    }
}
