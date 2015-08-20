package nanodegree.reshmi.com.spotify;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Created by annupinju on 8/15/2015.
 */
public class MusicPlayerFragment extends DialogFragment {

    public static String DIALOG_WIDTH = "dialog_width";
    public static String DIALOG_HEIGHT = "dialog_height";

    public static MusicPlayerFragment newInstance(int dialogWidth, int dialogHeight){
        MusicPlayerFragment f = new MusicPlayerFragment();

        // Supply layout dimensions as an argument.
        Bundle args = new Bundle();
        args.putInt(DIALOG_WIDTH, dialogWidth);
        args.putInt(DIALOG_HEIGHT, dialogHeight);
        f.setArguments(args);

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.music_player_fragment, container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        //If the dialog fragment is showing as a dialog, set the dimension of the dialog fragment
        if(getShowsDialog()){
            //setDialogLayout( );
        }
    }

    private void setDialogLayout() {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = getResources().getDisplayMetrics().density;
        float dpHeight = outMetrics.heightPixels / density;
        float dpWidth = outMetrics.widthPixels / density;

        int dialogWidth = Math.round((dpWidth * 75) / 100);

        getDialog().getWindow().setLayout(dialogWidth, dialogWidth);
    }
}
