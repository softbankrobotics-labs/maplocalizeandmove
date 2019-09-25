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

import com.airbnb.lottie.LottieAnimationView;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;
import com.softbankrobotics.maplocalizeandmove.Utils.LocalizeAndMapHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.Popup;

import java.util.concurrent.TimeUnit;

public class LocalizeRobotFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "MSI_LocalizeRobot";
    private MainActivity ma;
    private Popup localizedPopup;

    /**
     * inflates the layout associated with this fragment
     * if an application theme is set it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_localize_robot;
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
        LocalizeRobotFragment currentFragment = (LocalizeRobotFragment) ma.getFragment();
        Button back_buton = view.findViewById(R.id.back_button);
        back_buton.setOnClickListener((v) -> {
            ma.stopLocalizing();
            ma.setFragment(new ProductionFragment(), true);
            ma.robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();

        });
        Button stopLocalize = view.findViewById(R.id.button_stop_save);
        stopLocalize.setOnClickListener((v) -> ma.stopLocalizing());

        Button retry_Button = view.findViewById(R.id.button_retry);
        LottieAnimationView localizing_at_home = view.findViewById(R.id.localizing_at_home);
        ImageView localizing_error = view.findViewById(R.id.localizing_error);

        localizedPopup = new Popup(R.layout.popup_localized, this, ma);
        Button close_button = localizedPopup.inflator.findViewById(R.id.close_button);
        close_button.setOnClickListener((v) -> {
            localizedPopup.dialog.hide();
            ma.setFragment(new ProductionFragment(), true);
        });

        ma.robotHelper.localizeAndMapHelper.addOnFinishedLocalizingListener(result -> {
            ma.robotIsLocalized.set(result == LocalizeAndMapHelper.LocalizationStatus.LOCALIZED);
            ma.runOnUiThread(() -> {
                if (result == LocalizeAndMapHelper.LocalizationStatus.LOCALIZED) {
                    localizedPopup.dialog.show();
                    localizedPopup.dialog.getWindow().setAttributes(localizedPopup.lp);
                    FutureUtils.wait((long) 5, TimeUnit.SECONDS)
                            .thenConsume(aUselessFutureB -> ma.runOnUiThread(() -> {
                                if (currentFragment.isVisible()) {
                                    ma.setFragment(new ProductionFragment(), true);
                                    localizedPopup.dialog.hide();
                                }
                            }));
                    ma.robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();
                } else if (result == LocalizeAndMapHelper.LocalizationStatus.MAP_MISSING) {
                    noMapToLoad();
                    ma.robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();
                } else if (result == LocalizeAndMapHelper.LocalizationStatus.FAILED) {
                    stopLocalize.setVisibility(View.GONE);
                    localizing_at_home.setVisibility(View.GONE);
                    localizing_error.setVisibility(View.VISIBLE);
                    retry_Button.setVisibility(View.VISIBLE);
                    retry_Button.setOnClickListener((v) -> {
                        ma.startLocalizing();
                        retry_Button.setVisibility(View.GONE);
                        localizing_error.setVisibility(View.GONE);
                        stopLocalize.setVisibility(View.VISIBLE);
                        localizing_at_home.setVisibility(View.VISIBLE);
                    });
                } else {
                    Log.d(TAG, "onViewCreated: Unable to localize in Map");
                }
            });
        });

        ma.startLocalizing();
    }

    private void noMapToLoad() {
        Popup noMapToLoad = new Popup(R.layout.popup_no_map_to_load, this, ma);
        Button close = noMapToLoad.inflator.findViewById(R.id.close_button);
        close.setOnClickListener((v) -> {
            noMapToLoad.dialog.hide();
            ma.setFragment(new SetupFragment(), true);
        });
        noMapToLoad.dialog.show();
    }

}
