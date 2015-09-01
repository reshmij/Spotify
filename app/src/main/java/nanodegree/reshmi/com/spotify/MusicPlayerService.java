package nanodegree.reshmi.com.spotify;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import model.TrackInfo;


/**
 * Created by annupinju on 8/20/2015.
 */
public class MusicPlayerService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

    public static final String IC_ACTION_INIT = "initMusic";
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
    public static final String ARTIST_NAME_EXTRA = "artist_name_extra";
    public static final String TRACK_EXTRA = "now_playing_extra";
    public static final String CURRENT_POSITION_EXTRA = "current_playback_pos";
    public static final String TRACK_DURATION_EXTRA = "track_duration";
    public static final int SEEK_BAR_UPDATE_INTERVAL = 500;//in milliseconds


    private static final String LOG_TAG = MusicPlayerService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0x55;


    enum PlaybackStates {
        RESET,
        PLAYING,
        PAUSED,
        STOPPED
    } ;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private Handler handler = new Handler();
    private ArrayList<TrackInfo> mPlayList;
    int mCurrentTrackIndex;
    PlaybackStates mPlaybackState = PlaybackStates.RESET;
    TrackInfo mNowPlayingTrack = null;
    String mArtistName = null;
    int mNowPlayingTrackDuration = -1;
    NotificationCompat.Builder mBuilder=null;


    @Override
    public void onCreate() {
        setListeners();

        if (mMediaPlayer != null) {
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            String action = (intent != null) ? intent.getAction() : null;
            if (action == null) {
                //Do nothing
            } else if (action.equals(IC_ACTION_INIT)) {
                Bundle extras = intent.getExtras();
                ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                int selectedIndex = extras.getInt(SELECTED_INDEX_EXTRA);
                String artistName = extras.getString(ARTIST_NAME_EXTRA);

                setPlayList(list);
                setSelectedTrackIndex(selectedIndex);
                setArtistName(artistName);

            } else if (action.equals(IC_ACTION_PLAY)) {

                switch (mPlaybackState) {

                    case RESET: {
                        if (mMediaPlayer != null) {

                            Bundle extras = intent.getExtras();
                            playNew(extras);
                        }
                    }
                    break;

                    case PLAYING:
                        pauseAudio();
                        break;

                    case STOPPED:
                        preparePlayer();
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
                int position = intent.getIntExtra(MusicPlayerService.CURRENT_POSITION_EXTRA, 0);
                seekTo(position);

            } else if (action.equals(IC_ACTION_PLAY_NEW_TRACK)) {
                Bundle extras = intent.getExtras();
                playNew(extras);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
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

        try {
            cancelNotification();
            cancelSeekBarUpdateHandler();
            mNowPlayingTrack = null;
            mPlaybackState = PlaybackStates.RESET;
            mBuilder = null;

            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void setListeners() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnInfoListener(this);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {

        if (mediaPlayer != null) {
            Toast.makeText(this, R.string.playback_error, Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error Code:" + what + " extra:" + extra);
            mMediaPlayer.reset();
            mPlaybackState = PlaybackStates.RESET;
            broadcastEvent(PLAYBACK_ERROR_BROADCAST_EVENT, null);
        }
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

        int trackDuration = mediaPlayer.getDuration();
        stopAudio();
        cancelSeekBarUpdateHandler();
        //Broadcast this event so UI can be updated ( Pause button -> Play button)
        Bundle extras = new Bundle();
        extras.putInt(TRACK_DURATION_EXTRA,trackDuration);
        broadcastEvent(PLAYBACK_COMPLETE_BROADCAST_EVENT, extras);
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        setupSeekBarUpdateHandler();
    }

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }

    public void setPlayList(ArrayList<TrackInfo> list) {
        mPlayList = list;
    }

    public void setSelectedTrackIndex(int currentPosition) {
        mCurrentTrackIndex = currentPosition;
    }

    public void playNew(Bundle extras) {
        try {
            if (extras != null) {

                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }

                ArrayList<TrackInfo> list = extras.getParcelableArrayList(PLAY_LIST_EXTRA);
                int selectedIndex = extras.getInt(SELECTED_INDEX_EXTRA);
                String artistName = extras.getString(ARTIST_NAME_EXTRA);

                setPlayList(list);
                setSelectedTrackIndex(selectedIndex);
                setArtistName(artistName);

                preparePlayer();

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void playNext() {
        try {
            int next = getNextTrackIndex();
            setCurrentTrackIndex(next);
            preparePlayer();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void playPrev() {
        try {
            int prev = getPrevTrackIndex();
            setCurrentTrackIndex(prev);
            preparePlayer();
        } catch (Exception e) {

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

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.start();

                mNowPlayingTrack = mPlayList.get(mCurrentTrackIndex);
                Bundle args = new Bundle();
                args.putParcelable(NOW_PLAYING_EXTRA, mNowPlayingTrack);
                args.putInt(CURRENT_POSITION_EXTRA, mCurrentTrackIndex);
                args.putString(ARTIST_NAME_EXTRA, mArtistName);
                broadcastEvent(PLAYBACK_NEW_TRACK_BROADCAST_EVENT, args);
                setupSeekBarUpdateHandler();
                buildNotification();
                mPlaybackState = PlaybackStates.PLAYING;
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void pauseAudio() {

        try {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();

                //Update notification
                buildNotification();

                int curPosition = mMediaPlayer.getCurrentPosition();
                int trackDuration = mNowPlayingTrackDuration;

                Bundle extras = new Bundle();
                extras.putInt(CURRENT_POSITION_EXTRA, curPosition);
                extras.putInt(TRACK_DURATION_EXTRA, trackDuration);
                extras.putParcelable(NOW_PLAYING_EXTRA, mPlayList.get(mCurrentTrackIndex));
                extras.putParcelableArrayList(PLAY_LIST_EXTRA, mPlayList);
                extras.putInt(SELECTED_INDEX_EXTRA, mCurrentTrackIndex);
                extras.putString(ARTIST_NAME_EXTRA, mArtistName);

                broadcastEvent(PLAYBACK_PAUSE_BROADCAST_EVENT, extras);
                mPlaybackState = PlaybackStates.PAUSED;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void stopAudio() {

        try {
            if (mMediaPlayer != null) {

                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                stopForeground(true);
                mPlaybackState = PlaybackStates.STOPPED;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void resumeAudio() {

        try {
            if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {

                mMediaPlayer.start();
                mPlaybackState = PlaybackStates.PLAYING;
                broadcastEvent(PLAYBACK_RESUME_BROADCAST_EVENT, null);

                //Update notification
                buildNotification();

            }

        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }


    public void seekTo(int progress) {
        try {

            if (mMediaPlayer != null) {
                cancelSeekBarUpdateHandler();
                mMediaPlayer.seekTo(progress);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    void buildNotification() {

        try {

            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                    new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this);

            mBuilder.setTicker(mNowPlayingTrack.getTrackName())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .setVisibility(getNotificationVisibility())
                    .setContentTitle(mArtistName)
                    .setContentText(mNowPlayingTrack.getTrackName());

            Notification notification = mBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

                if(getNotificationVisibility() == NotificationCompat.VISIBILITY_PUBLIC) {
                    RemoteViews remoteViews = setUpRemoteView(notification);
                    notification.bigContentView = remoteViews;
                }
            }

            startForeground(NOTIFICATION_ID, notification);
        }
        catch(Exception e){
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    RemoteViews setUpRemoteView(Notification notification ){

        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.notification);

        if(!mMediaPlayer.isPlaying()){
            remoteViews.setImageViewResource(R.id.btn_notif_play_pause, android.R.drawable.ic_media_play);
        }
        else {
            remoteViews.setImageViewResource(R.id.btn_notif_play_pause, android.R.drawable.ic_media_pause);
        }

        remoteViews.setTextViewText(R.id.text_notif_artist_name, mArtistName);
        remoteViews.setTextViewText(R.id.text_notif_song_name, mNowPlayingTrack.getTrackName());

        Picasso.with(this)
                .load(mNowPlayingTrack.getGetAlbumThumbnailLrgUrl())
                .into(remoteViews, R.id.image_notif_thumbnail, NOTIFICATION_ID, notification);

        Intent playIntent = new Intent(this,MusicPlayerService.class);
        playIntent.setAction(IC_ACTION_PLAY);
        playIntent.putParcelableArrayListExtra(PLAY_LIST_EXTRA, mPlayList);
        playIntent.putExtra(SELECTED_INDEX_EXTRA, mCurrentTrackIndex);
        playIntent.putExtra(ARTIST_NAME_EXTRA, mArtistName);
        PendingIntent pendingPlayIntent =
                PendingIntent.getService(
                        this,
                        0,
                        playIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_play_pause, pendingPlayIntent);


        Intent prevIntent = new Intent(this,MusicPlayerService.class);
        prevIntent.setAction(IC_ACTION_PLAY_PREV);
        PendingIntent pendingPrevIntent = PendingIntent.getService( this,1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_prev,pendingPrevIntent);


        Intent nextIntent = new Intent(this,MusicPlayerService.class);
        nextIntent.setAction(IC_ACTION_PLAY_NEXT);
        PendingIntent pendingNextIntent = PendingIntent.getService( this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_next,pendingNextIntent);

        return remoteViews;
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
        if(handler!=null) {
            handler.removeCallbacks(updateSeekBarUI);
            handler.postDelayed(updateSeekBarUI, SEEK_BAR_UPDATE_INTERVAL);
        }
    }

    private void cancelSeekBarUpdateHandler() {
        if(handler!=null) {
            handler.removeCallbacks(updateSeekBarUI);
        }
    }

    private final Runnable updateSeekBarUI = new Runnable() {
        @Override
        public void run() {
            dispatchPlaybackProgress();
            handler.postDelayed(this, SEEK_BAR_UPDATE_INTERVAL);
        }
    };

    private void dispatchPlaybackProgress() {

        try {

            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    int curPosition = mMediaPlayer.getCurrentPosition();
                    mNowPlayingTrackDuration = mMediaPlayer.getDuration();

                    Bundle extras = new Bundle();
                    extras.putInt(CURRENT_POSITION_EXTRA, curPosition);
                    extras.putInt(TRACK_DURATION_EXTRA, mNowPlayingTrackDuration);
                    extras.putParcelable(NOW_PLAYING_EXTRA, mPlayList.get(mCurrentTrackIndex));
                    extras.putParcelableArrayList(PLAY_LIST_EXTRA, mPlayList);
                    extras.putInt(SELECTED_INDEX_EXTRA, mCurrentTrackIndex);
                    extras.putString(ARTIST_NAME_EXTRA, mArtistName);
                    broadcastEvent(PLAYBACK_PROGRESS_BROADCAST_EVENT, extras);
                } else {

                    if (mPlaybackState == PlaybackStates.PAUSED) {

                        int curPosition = mMediaPlayer.getCurrentPosition();

                        Bundle extras = new Bundle();
                        extras.putInt(CURRENT_POSITION_EXTRA, curPosition);
                        extras.putInt(TRACK_DURATION_EXTRA, mNowPlayingTrackDuration);
                        extras.putParcelable(NOW_PLAYING_EXTRA, mPlayList.get(mCurrentTrackIndex));
                        extras.putParcelableArrayList(PLAY_LIST_EXTRA, mPlayList);
                        extras.putInt(SELECTED_INDEX_EXTRA, mCurrentTrackIndex);
                        extras.putString(ARTIST_NAME_EXTRA, mArtistName);
                        broadcastEvent(PLAYBACK_PAUSE_BROADCAST_EVENT, extras);
                    }
                }
            }
        }catch(Exception e){
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    int getNotificationVisibility( ){

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean visibility =  sharedPrefs.getBoolean(getString(R.string.notification_preference_key), true);

        int flag = (visibility == true)?NotificationCompat.VISIBILITY_PUBLIC:NotificationCompat.VISIBILITY_PRIVATE;
        return flag;
    }
}