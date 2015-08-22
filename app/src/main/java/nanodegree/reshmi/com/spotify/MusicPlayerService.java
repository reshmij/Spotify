package nanodegree.reshmi.com.spotify;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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

    public static String IC_ACTION_PLAY = "playMusic";
    public static String IC_ACTION_PAUSE = "pauseMusic";
    public static String IC_ACTION_RESUME = "resumeMusic";
    public static String IC_ACTION_RESTART = "restartMusic";
    public static String IC_ACTION_STOP = "stopMusic";
    public static String PLAYBACK_COMPLETE_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_COMPLETE";
    public static String PLAYBACK_ERROR_BROADCAST_EVENT = "nanodegree.reshmi.com.spotify.PLAYBACK_ERROR";
    private static String LOG_TAG = MusicPlayerService.class.getSimpleName();
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    @Override
    public void onCreate() {
        setListeners();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent.getAction().equals(IC_ACTION_PLAY)) {
            String url = intent.getStringExtra(MusicPlayerFragment.TRACK_URL);
            Log.v(LOG_TAG, url);
            preparePlayer(url);
        } else if (intent.getAction().equals(IC_ACTION_PAUSE)) {
            pauseAudio();
        } else if (intent.getAction().equals(IC_ACTION_RESUME)) {
            resumeAudio();
        } else if (intent.getAction().equals(IC_ACTION_RESTART)) {
            restartAudio();
        } else if (intent.getAction().equals(IC_ACTION_STOP)) {
            stopAudio();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
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

        //Toast.makeText(this, R.string.playback_error, Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Error Code:" + what + " extra:" + extra);
        mMediaPlayer.reset();
        broadcastEvent(PLAYBACK_ERROR_BROADCAST_EVENT);
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

        //Broadcast this event so UI can be updated ( Pause button -> Play button)
        broadcastEvent(PLAYBACK_COMPLETE_BROADCAST_EVENT);
    }

    private void broadcastEvent(String event) {
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(event);
        broadcaster.sendBroadcast(intent);
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    void preparePlayer(String url) {
        mMediaPlayer.reset();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.prepareAsync();
    }

    void playAudio() {
        if(mMediaPlayer!=null) {
            mMediaPlayer.start();
        }
    }

    void pauseAudio() {
        if(mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    void stopAudio() {
        if(mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    void restartAudio() {
        if(mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.prepareAsync();
        }
    }

    void resumeAudio() {
        if(mMediaPlayer!=null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }
}