package com.softbankrobotics.maplocalizeandmove.Utils;

import android.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.softbankrobotics.maplocalizeandmove.MainActivity;

public class Popup {
    public AlertDialog dialog;
    public View inflator;
    public WindowManager.LayoutParams lp;

    public Popup(int layout, Fragment saveLocationsFragment, MainActivity ma) {
        LayoutInflater inflaterPopup = saveLocationsFragment.getLayoutInflater();
        inflator = inflaterPopup.inflate(layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(ma);

        setSystemUiVisibilityMode();
        inflator.setOnSystemUiVisibilityChangeListener(visibility -> {
            setSystemUiVisibilityMode(); // Needed to avoid exiting immersive_sticky when keyboard is displayed
        });

        builder.setView(inflator);
        dialog = builder.create();

        lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
    }

    private void setSystemUiVisibilityMode() {

        int options;
        options = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        inflator.setSystemUiVisibility(options);
    }
}