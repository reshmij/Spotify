package nanodegree.reshmi.com.spotify;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import model.TrackInfo;

/**
 * Created by annupinju on 8/15/2015.
 */
public class MusicPlayerFragment extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String LOG_TAG = MusicPlayerFragment.class.getSimpleName();
    public static final int INVALID_LENGTH = -1;
    public static final int INVALID_POSITION = -1;

    ArrayList<TrackInfo> mPlayList = new ArrayList<>();
    int mCurrentTrackIndex = -1;
    String mArtistName = null;

    ImageButton mBtnPrev;
    ImageButton mBtnPlayPause;
    ImageButton mBtnNext;
    TextView mAlbumNameTextView;
    TextView mArtistNameTextView;
    TextView mSongNameTextView;
    ImageView mAlbumThumbnail;
    SeekBar mSeekBar;

    Intent mServiceIntent = null;
    LocalBroadcastManager mLocalBroadcastManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    public static MusicPlayerFragment newInstance(ArrayList<TrackInfo> trackInfoResults, int position, String artistName, TrackInfo nowPlaying) {
        MusicPlayerFragment f = new MusicPlayerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(TopTenTracksFragment.TRACK_LIST, trackInfoResults);
        args.putInt(TopTenTracksFragment.SELECTED_TRACK_INDEX, position);
        args.putString(TopTenTracksFragment.ARTIST_NAME, artistName);

        if(nowPlaying!=null){
            args.putParcelable(TopTenTracksFragment.NOW_PLAYING_TRACK, nowPlaying);
        }
        f.setArguments(args);
        return f;
    }

    public static MusicPlayerFragment newInstance(Bundle args) {
        MusicPlayerFragment f = new MusicPlayerFragment();
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate");
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        mServiceIntent = new Intent(getActivity(), MusicPlayerService.class);
        getActivity().startService(mServiceIntent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        TrackInfo nowPlayingTrack = null;

        //Inflate the view
        View rootView = inflater.inflate(R.layout.music_player_fragment, container, false);

        //Get a handle to the widgets
        mBtnPrev = (ImageButton) rootView.findViewById(R.id.btn_media_prev);
        mBtnPlayPause = (ImageButton) rootView.findViewById(R.id.btn_media_play_pause);
        mBtnNext = (ImageButton) rootView.findViewById(R.id.btn_media_next);
        mAlbumNameTextView = (TextView) rootView.findViewById(R.id.text_player_album_name);
        mArtistNameTextView = (TextView) rootView.findViewById(R.id.text_player_artist_name);
        mSongNameTextView = (TextView) rootView.findViewById(R.id.text_player_song_name);
        mAlbumThumbnail = (ImageView) rootView.findViewById(R.id.image_player_thumbnail);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar_player);

        //Set listeners
        mBtnPrev.setOnClickListener(this);
        mBtnPlayPause.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);

        //Get arguments
        if(savedInstanceState == null) {
            Bundle args = getArguments();
            mPlayList = args.getParcelableArrayList(TopTenTracksFragment.TRACK_LIST);
            mArtistName = args.getString(TopTenTracksFragment.ARTIST_NAME);
            mArtistNameTextView.setText(mArtistName);
            mCurrentTrackIndex = args.getInt(TopTenTracksFragment.SELECTED_TRACK_INDEX);
            nowPlayingTrack = args.getParcelable(TopTenTracksFragment.NOW_PLAYING_TRACK);
        }
        else{
            mPlayList = savedInstanceState.getParcelableArrayList(TopTenTracksFragment.TRACK_LIST);
            mArtistName = savedInstanceState.getString(TopTenTracksFragment.ARTIST_NAME);
            mArtistNameTextView.setText(mArtistName);
            mCurrentTrackIndex = savedInstanceState.getInt(TopTenTracksFragment.SELECTED_TRACK_INDEX);
        }

        Log.d(LOG_TAG, "OnCreateView: ArtistName" + mArtistName);
        Log.d(LOG_TAG, "OnCreateView: PlayList");
        for (TrackInfo t : mPlayList) {
            Log.d(LOG_TAG, "OnCreateView " + t.getTrackName());
        }
        Log.d(LOG_TAG, "OnCreateView Current index " + mCurrentTrackIndex + " = " + mPlayList.get(mCurrentTrackIndex).getTrackName());

        updateUI(mPlayList.get(mCurrentTrackIndex));

        if(savedInstanceState==null){

            if(nowPlayingTrack==null) {

                Bundle extras = new Bundle();
                extras.putParcelableArrayList(MusicPlayerService.PLAY_LIST_EXTRA, mPlayList);
                extras.putInt(MusicPlayerService.CURRENT_POSITION_EXTRA, mCurrentTrackIndex);
                extras.putParcelable(MusicPlayerService.TRACK_EXTRA, mPlayList.get(mCurrentTrackIndex));
                extras.putString(MusicPlayerService.ARTIST_NAME_EXTRA, mArtistName);
                startIntent(MusicPlayerService.IC_ACTION_PLAY_NEW_TRACK, extras);
            }
        }

        return rootView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(TopTenTracksFragment.SELECTED_TRACK_INDEX, mCurrentTrackIndex);
        outState.putParcelableArrayList(TopTenTracksFragment.TRACK_LIST, mPlayList);
        outState.putString(TopTenTracksFragment.ARTIST_NAME,mArtistName);

        Log.d(LOG_TAG, "OnSaveInstance: ArtistName" + mArtistName);
        Log.d(LOG_TAG, "OnSaveInstance: PlayList");
        for (TrackInfo t : mPlayList) {
            Log.d(LOG_TAG, "OnSaveInstance " + t.getTrackName());
        }
        Log.d(LOG_TAG, "Current index " + mCurrentTrackIndex + " = " + mPlayList.get(mCurrentTrackIndex).getTrackName());
    }


    @Override
    public void onStart() {
        super.onStart();
        if(getShowsDialog()){

            int layout_width = getResources().getDimensionPixelSize(R.dimen.popup_width);
            int layout_height = getResources().getDimensionPixelSize(R.dimen.popup_height);

            getDialog().getWindow().setLayout(layout_width,layout_height);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "onResume");
        //Register the broadcast receiver
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_ERROR_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_PROGRESS_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_PAUSE_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_RESUME_BROADCAST_EVENT));
        mLocalBroadcastManager.registerReceiver((mReceiver), new IntentFilter(MusicPlayerService.PLAYBACK_NEW_TRACK_BROADCAST_EVENT));
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    private void updateUI(TrackInfo trackInfo) {
        Log.d(LOG_TAG, "updateUI");
        mAlbumNameTextView.setText(trackInfo.getAlbumName());
        mSongNameTextView.setText(trackInfo.getTrackName());
        String imageUrl = trackInfo.getGetAlbumThumbnailLrgUrl();
        if ((imageUrl != null) && (!imageUrl.isEmpty())) {
            Picasso.with(getActivity()).load(imageUrl).into(mAlbumThumbnail);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btn_media_prev:
                startIntent(MusicPlayerService.IC_ACTION_PLAY_PREV);
                break;

            case R.id.btn_media_play_pause:
                Bundle extras = new Bundle();
                extras.putParcelable(MusicPlayerService.TRACK_EXTRA, mPlayList.get(mCurrentTrackIndex));
                startIntent(MusicPlayerService.IC_ACTION_PLAY, extras);
                break;

            case R.id.btn_media_next:
                startIntent(MusicPlayerService.IC_ACTION_PLAY_NEXT);
                break;

            default:
                break;
        }
    }


    void setImageToPlayButton() {

        mBtnPlayPause.setImageResource(android.R.drawable.ic_media_play);
    }

    void setImageToPauseButton() {
        mBtnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    void handleIntent(Intent intent) {
        if (intent.getAction().equals(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT)) {
            Log.d(LOG_TAG, "PLAYBACK_COMPLETE_BROADCAST_EVENT");

            mSeekBar.setProgress(mSeekBar.getMax());
            setImageToPlayButton();

        } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_PROGRESS_BROADCAST_EVENT)) {

            int trackLength = intent.getIntExtra(MusicPlayerService.TRACK_DURATION_EXTRA, 0);
            int cur = intent.getIntExtra(MusicPlayerService.CURRENT_POSITION_EXTRA, 0);

            mSeekBar.setMax(trackLength);
            mSeekBar.setProgress(cur);

            setImageToPauseButton();

        } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_PAUSE_BROADCAST_EVENT)) {

            int trackLength = intent.getIntExtra(MusicPlayerService.TRACK_DURATION_EXTRA, 0);
            int cur = intent.getIntExtra(MusicPlayerService.CURRENT_POSITION_EXTRA, 0);

            mSeekBar.setMax(trackLength);
            mSeekBar.setProgress(cur);

            setImageToPlayButton();

        } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_ERROR_BROADCAST_EVENT)) {

            setImageToPlayButton();

        } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_RESUME_BROADCAST_EVENT)) {

            setImageToPauseButton();

        } else if (intent.getAction().equals(MusicPlayerService.PLAYBACK_NEW_TRACK_BROADCAST_EVENT)) {

            setImageToPauseButton();

            TrackInfo nowPlaying = intent.getParcelableExtra(MusicPlayerService.NOW_PLAYING_EXTRA);
            int newIndex = intent.getIntExtra(MusicPlayerService.CURRENT_POSITION_EXTRA,-1);
            updateUI(nowPlaying);
            mCurrentTrackIndex = newIndex;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (fromUser) {
            Bundle args = new Bundle();
            args.putInt(MusicPlayerService.CURRENT_POSITION_EXTRA,progress);
            startIntent(MusicPlayerService.IC_ACTION_SEEK, args);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    void startIntent(String action, Bundle extras){
        mServiceIntent.setAction(action);
        if(extras!=null){
            mServiceIntent.putExtras(extras);
        }
        getActivity().startService(mServiceIntent);
    }

    void startIntent(String action){
        mServiceIntent.setAction(action);
        getActivity().startService(mServiceIntent);
    }
}
