package nanodegree.reshmi.com.spotify;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by annupinju on 8/24/2015.
 */
public class Utils {

    public static boolean hasMusicPlayed(Context context){

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        return sharedPref.getBoolean(context.getString(R.string.has_played_music),false);
    }

    public static void setMusicPlayed(Context context, boolean hasMusicPlayed){

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(context.getString(R.string.has_played_music), hasMusicPlayed);
        editor.commit();
    }
}
