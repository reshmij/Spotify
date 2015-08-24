package nanodegree.reshmi.com.spotify;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import model.TrackInfo;


/**
 * Created by annupinju on 8/20/2015.
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

    public static final String IC_ACTION_PLAY = "playMusic";
    public static final String IC_ACTION_PLAY_PREV = "playPrev";
    public static final String IC_ACTION_PLAY_NEXT = "playNext";
    public static final String IC_ACTION_PLAY_NEW_TRACK = "playNew";
    public static final String IC_ACTION_SEEK = "seekMusic";

    public static final String PLAYBACK_COMPLETE_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_COMPLETE";
    public static final String PLAYBACK_ERROR_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_ERROR";
    public static final String PLAYBACK_PROGRESS_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_PROGRESS_BROADCAST_EVENT";
    public static final String PLAYBACK_PAUSE_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_PAUSE_BROADCAST_EVENT";
    public static final String PLAYBACK_RESUME_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_START_BROADCAST_EVENT";
    public static final String PLAYBACK_NEW_TRACK_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_NEW_TRACK_BROADCAST_EVENT";

    public static final String PLAY_LIST_EXTRA = "play_list_extra";
    public static final String SELECTED_INDEX_EXTRA = "selected_index_extra";
    public static final String NOW_PLAYING_EXTRA = "now_playing_extra";
    public static final String TRACK_EXTRA = "now_playing_extra";
    public static final String CURRENT_POSITION_EXTRA = "current_playback_pos";
    public static final String TRACK_DURATION_EXTRA = "track_duration";
    public static final String IS_NEW_PLAYLIST = "is_new_playlist";
    public static final int SEEK_BAR_UPDATE_INTERVAL = 500;//in milliseconds


    private static final String LOG_TAG = MusicPlayerService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0x55;

    enum PlaybackStates {
        RESET,
        PLAYING,
        PAUSED,
        STOPPED
    }

    ;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private Handler handler = new Handler();
    private ArrayList<TrackInfo> mPlayList;
    int mCurrentTrackIndex;
    PlaybackStates mPlaybackState = PlaybackStates.RESET;
    TrackInfo mNowPlayingTrack = null;

    @Override
    public void onCreate() {
        setListeners();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = (intent != null) ? intent.getAction() : null;
        if (action == null) {
            //Do nothing
        } else if (action.equals(IC_ACTION_PLAY)) {

            //handlePlayback(intent);

            switch (mPlaybackState) {

                case RESET: {
                    if (mMediaPlayer != null) {

                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }

                        Bundle extras = intent.getExtras();
                        if (extras != null) {
                            ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                            int selectedIndex = extras.getInt(CURRENT_POSITION_EXTRA);

                            setPlayList(list);
                            setSelectedTrackIndex(selectedIndex);

                            preparePlayer();

                        }
                    }
                }
                break;

                case PLAYING:
                    pauseAudio();
                    break;

                case STOPPED:
                    restartAudio();
                    break;

                case PAUSED:
                    resumeAudio();
                    break;

            }

        } else if (action.equals(IC_ACTION_PLAY_NEXT)) {
            playNext();
        } else if (action.equals(IC_ACTION_PLAY_PREV)) {
            playPrev();
        } else if (action.equals(IC_ACTION_SEEK)) {
            int position = intent.getIntExtra(MusicPlayerService.CURRENT_POSITION_EXTRA,0);
            seekTo(position);
        } else if(action.equals(IC_ACTION_PLAY_NEW_TRACK)){

            if (mMediaPlayer != null) {

                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }

                Bundle extras = intent.getExtras();
                if (extras != null) {
                    ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                    int selectedIndex = extras.getInt(CURRENT_POSITION_EXTRA);

                    setPlayList(list);
                    setSelectedTrackIndex(selectedIndex);

                    preparePlayer();

                }
            }
        }

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cancelNotification();
        cancelSeekBarUpdateHandler();
        mNowPlayingTrack = null;

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void setListeners() {
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {

        Toast.makeText(this, R.string.playback_error, Toast.LENGTH_SHORT).show();
        Log.e(LOG_TAG, "Error Code:" + what + " extra:" + extra);
        mMediaPlayer.reset();
        mPlaybackState = PlaybackStates.RESET;
        broadcastEvent(PLAYBACK_ERROR_BROADCAST_EVENT, null);
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playAudio();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopAudio();
        stopSelf();
        cancelSeekBarUpdateHandler();
        //Broadcast this event so UI can be updated ( Pause button -> Play button)
        broadcastEvent(PLAYBACK_COMPLETE_BROADCAST_EVENT, null);

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        setupSeekBarUpdateHandler();
    }

    public void setPlayList(ArrayList<TrackInfo> list) {
        mPlayList = list;
    }

    public void setSelectedTrackIndex(int currentPosition) {
        mCurrentTrackIndex = currentPosition;
    }

    private void handlePlayback(Intent intent ){
        switch (mPlaybackState) {

            case RESET: {
                if (mMediaPlayer != null) {

                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                    }

                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                        int selectedIndex = extras.getInt(CURRENT_POSITION_EXTRA);

                        setPlayList(list);
                        setSelectedTrackIndex(selectedIndex);

                        preparePlayer();

                    }
                }
            }
            break;

            case PLAYING:
                Bundle extras = intent.getExtras();
                if(extras == null){
                    pauseAudio();
                }
                else{
                    ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                    int selectedIndex = extras.getInt(CURRENT_POSITION_EXTRA);

                    setPlayList(list);
                    setSelectedTrackIndex(selectedIndex);

                    preparePlayer();
                }
                break;

            case STOPPED:
                restartAudio();
                break;

            case PAUSED:
                resumeAudio();
                break;

        }

    }

    public void playNext() {
        try {
            int next = getNextTrackIndex();
            setCurrentTrackIndex(next);
            preparePlayer();
        }catch(Exception e){
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    public void playPrev() {
        try {
            int prev = getPrevTrackIndex();
            setCurrentTrackIndex(prev);
            preparePlayer();
        }catch(Exception e){

        }
    }

    private int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    private void setCurrentTrackIndex(int pos) {
        mCurrentTrackIndex = pos;
    }

    private int getNextTrackIndex() {
        int next = getCurrentTrackIndex() + 1;
        int last = mPlayList.size() - 1;
        next = (next > last) ? last : next;
        return next;
    }

    private int getPrevTrackIndex() {
        int prev = getCurrentTrackIndex() - 1;
        prev = (prev < 0) ? 0 : prev;
        return prev;
    }

    private TrackInfo getCurrentTrack() {
        return mPlayList.get(mCurrentTrackIndex);
    }

    public boolean isMusicPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void preparePlayer() {

        TrackInfo track = getCurrentTrack();
        String url = track.getTrackUrl();

        mMediaPlayer.reset();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
    }

    public void playAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();

            mNowPlayingTrack = mPlayList.get(mCurrentTrackIndex);
            Bundle args = new Bundle();
            args.putParcelable(NOW_PLAYING_EXTRA, mNowPlayingTrack);
            args.putInt(CURRENT_POSITION_EXTRA, mCurrentTrackIndex);
            broadcastEvent(PLAYBACK_NEW_TRACK_BROADCAST_EVENT, args);
            setupSeekBarUpdateHandler();
            initNotification();
            mPlaybackState = PlaybackStates.PLAYING;
        }
    }

    public void pauseAudio() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();

            broadcastEvent(PLAYBACK_PAUSE_BROADCAST_EVENT, null);
            mPlaybackState = PlaybackStates.PAUSED;
        }
    }

    public void stopAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            stopForeground(true);
            mPlaybackState = PlaybackStates.STOPPED;
        }
    }

    public void resumeAudio() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mPlaybackState = PlaybackStates.PLAYING;
            broadcastEvent(PLAYBACK_RESUME_BROADCAST_EVENT, null);
        }
    }

    public void restartAudio() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.prepareAsync();
        }
    }

    public void seekTo(int progress) {
        try {

            if (mMediaPlayer != null) {
                cancelSeekBarUpdateHandler();
                mMediaPlayer.seekTo(progress);
            }
        }
        catch(Exception e){
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    void initNotification() {

        String songName = "test";
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), ArtistSearchActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification();
        notification.tickerText = getResources().getString(R.string.ticker_text) + songName;
        notification.icon = android.R.drawable.presence_audio_online;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(getApplicationContext(), getApplicationInfo().loadLabel(getPackageManager()),
                getResources().getString(R.string.ticker_text) + songName, pi);
        startForeground(NOTIFICATION_ID, notification);
    }


    void cancelNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void broadcastEvent(String event, Bundle extras) {
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(event);
        if (extras != null) {
            intent.putExtras(extras);
        }
        broadcaster.sendBroadcast(intent);
    }

    private void setupSeekBarUpdateHandler() {
        handler.removeCallbacks(updateSeekBarUI);
        handler.postDelayed(updateSeekBarUI, SEEK_BAR_UPDATE_INTERVAL);
    }

    private void cancelSeekBarUpdateHandler() {
        handler.removeCallbacks(updateSeekBarUI);
    }

    private final Runnable updateSeekBarUI = new Runnable() {
        @Override
        public void run() {
            dispatchPlaybackProgress();
            handler.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    private void dispatchPlaybackProgress() {

        if (mMediaPlayer.isPlaying()) {
            int curPosition = mMediaPlayer.getCurrentPosition();
            int trackDuration = mMediaPlayer.getDuration();

            Bundle extras = new Bundle();
            extras.putInt(CURRENT_POSITION_EXTRA, curPosition);
            extras.putInt(TRACK_DURATION_EXTRA, trackDuration);
            extras.putParcelable(NOW_PLAYING_EXTRA, mPlayList.get(mCurrentTrackIndex));
            broadcastEvent(PLAYBACK_PROGRESS_BROADCAST_EVENT, extras);
        }
    }
}