package nanodegree.reshmi.com.spotify;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import model.TrackInfo;

/**
 * Created by annupinju on 8/15/2015.
 */
public class MusicPlayerFragment extends DialogFragment implements View.OnClickListener {

    public enum PlaybackStates {
        RESET, STARTED, STOPPED, PAUSED, RUNNING
    }

    public static String TRACK_URL = "track_url";
    private static String LOG_TAG = MusicPlayerFragment.class.getSimpleName();
    private static String CURRENT_PLAYBACK_STATE = "current_playback_state";
    private static String CURRENT_TRACK_INDEX = "current_track_index";
    Intent mServiceIntent = null;
    ArrayList<TrackInfo> mPlayList = new ArrayList<>();
    int mCurrentTrackIndex = -1;

    PlaybackStates mCurrentState = PlaybackStates.RESET;
    String mArtistName = null;

    ImageButton mBtnPrev;
    ImageButton mBtnPlayPause;
    ImageButton mBtnNext;
    TextView mAlbumNameTextView;
    TextView mArtistNameTextView;
    TextView mSongNameTextView;
    ImageView mAlbumThumbnail;

    BroadcastReceiver mReceiver;

    public static MusicPlayerFragment newInstance(ArrayList<TrackInfo> trackInfoResults, int position, String artistName) {
        MusicPlayerFragment f = new MusicPlayerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(TopTenTracksFragment.TRACK_LIST, trackInfoResults);
        args.putInt(TopTenTracksFragment.TRACK_LIST_POS, position);
        args.putString(TopTenTracksFragment.ARTIST_NAME, artistName);
        f.setArguments(args);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(LOG_TAG,"onCreateView");
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

        //Set listeners
        mBtnPrev.setOnClickListener(this);
        mBtnPlayPause.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);

        //Create a service to play music
        mServiceIntent = new Intent(getActivity(), MusicPlayerService.class);

        //Initialize member variables
        Bundle args = getArguments();
        mPlayList = args.getParcelableArrayList(TopTenTracksFragment.TRACK_LIST);
        mArtistName = args.getString(TopTenTracksFragment.ARTIST_NAME);
        if(savedInstanceState==null) {
            mCurrentTrackIndex = args.getInt(TopTenTracksFragment.TRACK_LIST_POS);
        }
        else{
            mCurrentState = (PlaybackStates)savedInstanceState.getSerializable(CURRENT_PLAYBACK_STATE);
            mCurrentTrackIndex = (int)savedInstanceState.getSerializable(CURRENT_TRACK_INDEX);
        }

        //Update UI with artist name. This will not change during the lifecycle of this fragment instance.
        mArtistNameTextView.setText(mArtistName);

        //Register a broadcast receiver to receive an event from the Music Player service
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.getAction().equals(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT)){
                    //Playback completed. Change img to 'Play' button
                    setImageToPlayButton();
                    mCurrentState = PlaybackStates.RESET;
                } else if(intent.getAction().equals(MusicPlayerService.PLAYBACK_ERROR_BROADCAST_EVENT)){
                    //Playback error. Change img to 'Play' button
                    setImageToPlayButton();
                    mCurrentState = PlaybackStates.RESET;
                }
            }
        };

        //Update the rest of the UI
        updateUI();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //Register the broadcast receiver
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver((mReceiver),new IntentFilter(MusicPlayerService.PLAYBACK_COMPLETE_BROADCAST_EVENT));
        localBroadcastManager.registerReceiver((mReceiver),new IntentFilter(MusicPlayerService.PLAYBACK_ERROR_BROADCAST_EVENT));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(CURRENT_PLAYBACK_STATE, mCurrentState);
        outState.putSerializable(CURRENT_TRACK_INDEX, mCurrentTrackIndex);
        Log.d(LOG_TAG,"onSaveInstanceState");
    }

    private void updateUI() {
        Log.d(LOG_TAG,"updateUI");
        TrackInfo trackInfo = mPlayList.get(mCurrentTrackIndex);
        mAlbumNameTextView.setText(trackInfo.getAlbumName());
        mSongNameTextView.setText(trackInfo.getTrackName());
        String imageUrl = trackInfo.getGetAlbumThumbnailLrgUrl();
        if ((imageUrl != null) && (!imageUrl.isEmpty())) {
            Picasso.with(getActivity()).load(imageUrl).into(mAlbumThumbnail);
        }
        if(mCurrentState==PlaybackStates.RUNNING){
            setImageToPauseButton();
        }
        else{
            setImageToPlayButton();
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
            case R.id.btn_media_prev: {
                int index = getPrevTrack();
                if (mCurrentState == PlaybackStates.RUNNING) {
                    playAudioTrack(index);
                }
                else{
                    mCurrentState = PlaybackStates.RESET;
                }
                mCurrentTrackIndex = index;
                updateUI();
            }
            break;

            case R.id.btn_media_play_pause:
                playPauseAudioTrack(mCurrentTrackIndex);
                break;

            case R.id.btn_media_next: {
                int index = getNextTrack();
                if (mCurrentState == PlaybackStates.RUNNING) {
                    playAudioTrack(index);
                }
                else{
                    mCurrentState = PlaybackStates.RESET;
                }
                mCurrentTrackIndex = index;
                updateUI();
            }
            break;

            default:
                break;
        }
    }

    void playPauseAudioTrack(int position) {

        switch (mCurrentState) {

            case PAUSED:
                //Resume it
                resumeAudioTrack();
                break;

            case RUNNING:
                // Pause it
                pauseAudioTrack();
                break;

            case STOPPED:
                // Start service
                restartAudioTrack();
                break;

            case RESET:
            default:
                //Play audio track
                playAudioTrack(mCurrentTrackIndex);
                break;
        }

    }

    private void restartAudioTrack() {
        Log.d(LOG_TAG, "restartAudioTrack");
        mServiceIntent.setAction(MusicPlayerService.IC_ACTION_RESTART);
        mServiceIntent.putExtra(TRACK_URL, getTrackUrl(mCurrentTrackIndex));
        try {
            getActivity().startService(mServiceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(getActivity(), R.string.playback_error, Toast.LENGTH_SHORT).show();
        } finally {
            mCurrentState = PlaybackStates.RUNNING;
            //Change button icon to 'pause'
            setImageToPauseButton();
        }
    }

    private void playAudioTrack(int position) {
        Log.d(LOG_TAG, "playAudioTrack");
        //This will reset the media player and start all over again
        mServiceIntent.setAction(MusicPlayerService.IC_ACTION_PLAY);
        mServiceIntent.putExtra(TRACK_URL, getTrackUrl(position));
        try {
            getActivity().startService(mServiceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(getActivity(), R.string.playback_error, Toast.LENGTH_SHORT).show();
        } finally {
            mCurrentState = PlaybackStates.RUNNING;
            //Change button icon to 'pause'
            setImageToPauseButton();
        }
    }

    void pauseAudioTrack() {
        mServiceIntent.setAction(MusicPlayerService.IC_ACTION_PAUSE);

        try {
            getActivity().startService(mServiceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(getActivity(), R.string.playback_error, Toast.LENGTH_SHORT).show();
        } finally {
            mCurrentState = PlaybackStates.PAUSED;
            //Change button icon to 'play'
            setImageToPlayButton();
        }
    }

    void resumeAudioTrack() {
        mServiceIntent.setAction(MusicPlayerService.IC_ACTION_RESUME);

        try {
            getActivity().startService(mServiceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(getActivity(), R.string.playback_error, Toast.LENGTH_SHORT).show();
        } finally {
            mCurrentState = PlaybackStates.RUNNING;
            //Change button icon to 'pause'
            setImageToPauseButton();
        }
    }

    void stopAudioTrack() {
        mServiceIntent.setAction(MusicPlayerService.IC_ACTION_STOP);

        try {
            getActivity().startService(mServiceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(getActivity(), R.string.playback_error, Toast.LENGTH_SHORT).show();
        } finally {
            mCurrentState = PlaybackStates.STOPPED;
            //Change button icon to 'play'
            setImageToPlayButton();
        }
    }

    int getPrevTrack() {
        //Get the prev track index
        int prevTrackIndex = mCurrentTrackIndex - 1;
        int lastTrackIndex = mPlayList.size() - 1;
        //Loop over to the last track if there is no previous track
        prevTrackIndex = (prevTrackIndex >= 0) ? prevTrackIndex : lastTrackIndex;
        return prevTrackIndex;
    }

    int getNextTrack() {
        //Get next track index
        int lastTrackIndex = mPlayList.size() - 1;
        int nextTrackIndex = mCurrentTrackIndex + 1;
        //Loop over to the first track if list is exhausted
        nextTrackIndex = (nextTrackIndex > lastTrackIndex) ? 0 : nextTrackIndex;
        return nextTrackIndex;
    }

    void playPrevTrack() {
        //Get the prev track index
        int prevTrackIndex = mCurrentTrackIndex - 1;
        int lastTrackIndex = mPlayList.size() - 1;
        //Loop over to the last track if there is no previous track
        prevTrackIndex = (prevTrackIndex >= 0) ? prevTrackIndex : lastTrackIndex;
        //Update current state to RESET to reset the state machine to initial state
        mCurrentState = PlaybackStates.RESET;
        //Play prev track
        playAudioTrack(prevTrackIndex);
        //update current index
        mCurrentTrackIndex = prevTrackIndex;
        updateUI();
    }

    void playNextTrack() {
        //Get next track index
        int lastTrackIndex = mPlayList.size() - 1;
        int nextTrackIndex = mCurrentTrackIndex + 1;
        //Loop over to the first track if list is exhausted
        nextTrackIndex = (nextTrackIndex > lastTrackIndex) ? 0 : nextTrackIndex;
        //Update current state to RESET to reset the state machine to initial state
        mCurrentState = PlaybackStates.RESET;
        //Play the next track
        playAudioTrack(nextTrackIndex);

        mCurrentTrackIndex = nextTrackIndex;
        updateUI();
    }

    String getTrackUrl(int position) {
        return mPlayList.get(position).getTrackUrl();
    }

    void setImageToPlayButton() {
        mBtnPlayPause.setImageResource(android.R.drawable.ic_media_play);
    }

    void setImageToPauseButton() {
        mBtnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }
}
