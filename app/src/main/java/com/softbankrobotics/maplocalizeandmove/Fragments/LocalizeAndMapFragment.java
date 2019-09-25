package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;
import com.softbankrobotics.maplocalizeandmove.Utils.Popup;

import java.util.Timer;
import java.util.TimerTask;

public class LocalizeAndMapFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "MSI_ScreenOneFragment";
    public ImageView mapSaved;
    public Button stop_button;
    public Button retry;
    public TextView saving_text;
    public ImageView mapping_error;
    public ProgressBar progressBar;
    public Popup savingMapPopup;
    public LottieAnimationView  icn_360_load;
    public Timer timer;
    int slideNumber = 1;
    private MainActivity ma;
    private View localView;


    /**
     * inflates the layout associated with this fragment
     * if an application theme is set it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_localize_and_map;
        this.ma = (MainActivity) getActivity();
        if (ma != null) {
            Integer themeId = ma.getThemeId();
            if (themeId != null) {
                final Context contextThemeWrapper = new ContextThemeWrapper(ma, themeId);
                LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
                return localInflater.inflate(fragmentId, container, false);
            } else {
                return inflater.inflate(fragmentId, container, false);
            }
        } else {
            Log.e(TAG, "could not get mainActivity, can't create fragment");
            return null;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        localView = view;
        mapping_error = localView.findViewById(R.id.mapping_error);
        localView.findViewById(R.id.back_button).setOnClickListener((v) -> {
            if (timer != null) timer.cancel();
            ma.robotHelper.localizeAndMapHelper.removeOnFinishedMappingListeners();
            ma.robotHelper.localizeAndMapHelper.stopCurrentAction();
            ma.setFragment(new SetupFragment(), true);
        });
        stop_button = localView.findViewById(R.id.button_stop_save);
        stop_button.setOnClickListener((v) -> {
            if (timer != null) timer.cancel();
            savingMap();
            ma.stopMappingAndBackupMap();
            ma.robotIsLocalized.set(false);
        });

        retry = localView.findViewById(R.id.button_retry);
        retry.setOnClickListener((v) -> {
            ma.startMapping();
            if (timer != null) timer.cancel();
            icn_360_load.setVisibility(View.VISIBLE);
            stop_button.setVisibility(View.VISIBLE);
            retry.setVisibility(View.GONE);
            mapping_error.setVisibility(View.GONE);
        });

        ma.startMapping();
    }

    private void savingMap() {

        savingMapPopup = new Popup(R.layout.popup_map_saved, this, ma);
        saving_text = savingMapPopup.inflator.findViewById(R.id.saving_text);
        progressBar = savingMapPopup.inflator.findViewById(R.id.progressbar);
        mapSaved = savingMapPopup.inflator.findViewById(R.id.saved);
        Button close_button = savingMapPopup.inflator.findViewById(R.id.close_button);
        close_button.setOnClickListener((v) -> {
            savingMapPopup.dialog.hide();
            ma.setFragment(new SetupFragment(), true);
        });
        savingMapPopup.dialog.show();
        savingMapPopup.dialog.getWindow().setAttributes(savingMapPopup.lp);
    }

    public void onIntialMappingFinished() {
        icn_360_load = localView.findViewById(R.id.icn_360_load);
        ImageView ic_360_map = localView.findViewById(R.id.ic_360_map);
        ImageView warning = localView.findViewById(R.id.warning);
        TextView warning_text = localView.findViewById(R.id.warning_text);
        ImageView mapping = localView.findViewById(R.id.mapping);
        TextView mapping_text = localView.findViewById(R.id.mapping_text);
        ImageView dot_follow_three = localView.findViewById(R.id.dot_follow_three);
        ImageView trap = localView.findViewById(R.id.trap);
        TextView trap_text = localView.findViewById(R.id.trap_text);
        ImageView dot_follow_one = localView.findViewById(R.id.dot_follow_one);
        ImageView push = localView.findViewById(R.id.push);
        TextView push_text = localView.findViewById(R.id.push_text);
        ImageView dot_follow_two = localView.findViewById(R.id.dot_follow_two);

        trap.setVisibility(View.GONE);
        trap_text.setVisibility(View.GONE);
        dot_follow_one.setVisibility(View.GONE);

        push.setVisibility(View.GONE);
        push_text.setVisibility(View.GONE);
        dot_follow_two.setVisibility(View.GONE);

        mapping.setVisibility(View.GONE);
        mapping_text.setVisibility(View.GONE);
        dot_follow_three.setVisibility(View.GONE);

        ic_360_map.setVisibility(View.VISIBLE);
        icn_360_load.setVisibility(View.VISIBLE);
        warning.setVisibility(View.VISIBLE);
        warning_text.setVisibility(View.VISIBLE);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                ma.runOnUiThread(() -> {
                    stop_button.setVisibility(View.VISIBLE);
                    icn_360_load.setVisibility(View.GONE);
                    ic_360_map.setVisibility(View.GONE);
                    warning.setVisibility(View.GONE);
                    warning_text.setVisibility(View.GONE);
                    if (slideNumber == 1) {
                        slideNumber = 2;
                        mapping.setVisibility(View.GONE);
                        mapping_text.setVisibility(View.GONE);
                        dot_follow_three.setVisibility(View.GONE);
                        trap.setVisibility(View.VISIBLE);
                        trap_text.setVisibility(View.VISIBLE);
                        dot_follow_one.setVisibility(View.VISIBLE);
                    } else if (slideNumber == 2) {
                        slideNumber = 3;
                        trap.setVisibility(View.GONE);
                        trap_text.setVisibility(View.GONE);
                        dot_follow_one.setVisibility(View.GONE);
                        push.setVisibility(View.VISIBLE);
                        push_text.setVisibility(View.VISIBLE);
                        dot_follow_two.setVisibility(View.VISIBLE);
                    } else {
                        slideNumber = 1;
                        push.setVisibility(View.GONE);
                        push_text.setVisibility(View.GONE);
                        dot_follow_two.setVisibility(View.GONE);
                        mapping.setVisibility(View.VISIBLE);
                        mapping_text.setVisibility(View.VISIBLE);
                        dot_follow_three.setVisibility(View.VISIBLE);
                    }
                });
            }
        }, 24000, 10000);
    }
}
