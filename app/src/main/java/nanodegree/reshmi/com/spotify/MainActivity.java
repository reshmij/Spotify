package nanodegree.reshmi.com.spotify;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
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


public class MainActivity extends AppCompatActivity {

    public static final String ARTIST_ID = "artistId";
    public static final String ARTIST_NAME = "artistName";

    private String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String ARTISTS_LIST = "artistsList";
    private static final String ARTIST_QUERY = "artistQuery";
    private static final String LIST_POSITION = "listPosition";

    ArtistSearchListAdapter mAdapter = null;
    SearchArtistFromSpotifyTask mSearchArtistTask = null;
    EditText mSearchEditText = null;
    ListView mListView = null;
    String mLastQuery = null;

    ArrayList<ArtistInfo> mArtistInfoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get a handle to the views
        mListView = (ListView) findViewById(R.id.list_view_artist_search_results);
        mSearchEditText = (EditText) findViewById(R.id.edit_text_artist_search);

        int selectPosition = -1;
        if(savedInstanceState!=null){
            mArtistInfoList = savedInstanceState.getParcelableArrayList(ARTISTS_LIST);
            mLastQuery = savedInstanceState.getString(ARTIST_QUERY);
            selectPosition = savedInstanceState.getInt(LIST_POSITION, mListView.getSelectedItemPosition());
        }

        mAdapter = new ArtistSearchListAdapter(this,mArtistInfoList);
        mListView.setAdapter(mAdapter);
        if(selectPosition>=0){
            mListView.setSelection(selectPosition);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ArtistInfo artist = (ArtistInfo)mAdapter.getItem(position);

                Intent intent = new Intent(MainActivity.this, TopTenTracksActivity.class);
                intent.putExtra(ARTIST_ID,artist.getId());
                intent.putExtra(ARTIST_NAME,artist.getName());

                startActivity(intent);
            }
        });

        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable != null && (editable.length() > 0) ) {

                    String query = editable.toString();

                    if(query.equals(mLastQuery)){
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
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Cancel any async task since we're leaving
        if (mSearchArtistTask != null && !mSearchArtistTask.isCancelled()) {
            mSearchArtistTask.cancel(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the instance of the aerch results and query
        if(mArtistInfoList!=null) {
            outState.putParcelableArrayList(ARTISTS_LIST, mArtistInfoList);
            outState.putString(ARTIST_QUERY, mSearchEditText.getText().toString());
            outState.putInt(LIST_POSITION, mListView.getSelectedItemPosition());
        }
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

    private class SearchArtistFromSpotifyTask extends AsyncTask<String, Void, List<Artist>> {
        @Override
        protected List<Artist> doInBackground(String... strings) {

            List<Artist> artistsList = null;

            try{
                String query = strings[0];

                //Make a remote web call to get the artist search results
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotifyService = api.getService();
                ArtistsPager results = spotifyService.searchArtists(query);
                artistsList = results.artists.items;
            }
            catch( Exception e){
                Log.e(LOG_TAG,e.getMessage());
            }
            return artistsList;
        }

        @Override
        protected void onPostExecute(List<Artist> artists) {
            super.onPostExecute(artists);

            try {

                mArtistInfoList.clear();

                //If search result is empty, clear the list and display a toast
                if (artists.isEmpty()) {
                    mAdapter.clear();
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.artist_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                //Search returned valid results. Extract the required fields from it.
                for (Artist a : artists) {
                    ArtistInfo artistInfo = new ArtistInfo(a.id, a.name, getThumbnail(a.images));
                    mArtistInfoList.add(artistInfo);
                }

                mAdapter.removeAll(); // Remove the existing data in the list
                mAdapter.addAll(mArtistInfoList);
            }
            catch (Exception e )
            {
                Log.e(LOG_TAG,e.getMessage());
            }
        }

        private String getThumbnail(List<Image> images){

            //Method to get the smallest image in the list, which is the last one
            if((images == null)|| images.isEmpty()) {
                return null;
            }

            // Choose the smallest image
            int last = images.size() - 1;
            return images.get(last).url;
        }
    }
}
