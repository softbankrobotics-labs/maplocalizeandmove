package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;
import com.softbankrobotics.maplocalizeandmove.Utils.LocalizeAndMapHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.Popup;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SaveLocationsFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "MSI_SaveLocations";
    public ImageView locationsSaved;
    public ProgressBar progressBar;
    public TextView saving_text;
    public Popup savingLocationsPopup;
    public boolean locationsListModified;
    private MainActivity ma;
    private View localView;
    private AlertDialog tutorialPopupDialog;

    /**
     * inflates the layout associated with this fragment
     * if an application theme is set it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {

        int fragmentId = R.layout.fragment_save_locations;
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
        locationsListModified = false;
        localView = view;

        localView.findViewById(R.id.back_button).setOnClickListener((v) -> {
            if (locationsListModified) displayPopupConfirmation();
            else ma.setFragment(new SetupFragment(), true);

        });

        localView.findViewById(R.id.button_backup_location).setOnClickListener((v) -> {
            ma.backupLocations();
            displaypPopupLocationsSaved();
        });
        localView.findViewById(R.id.button_add_location).setOnClickListener((v) -> popupTutorialSaveAttachedFrame());

        displayLocations();
    }

    private void displayPopupConfirmation() {
        Popup displayPopup = new Popup(R.layout.popup_backup_locations_confirmation, this, ma);


        displayPopup.inflator.findViewById(R.id.button_cancel).setOnClickListener(view -> {
            displayPopup.dialog.hide();
        });
        displayPopup.inflator.findViewById(R.id.button_yes).setOnClickListener(view -> {
            displayPopup.dialog.hide();
            ma.setFragment(new SetupFragment(), true);
        });
        displayPopup.dialog.show();
        displayPopup.dialog.getWindow().setAttributes(displayPopup.lp);
    }

    private void popupTutorialSaveAttachedFrame() {
        Popup tutorialPopup = new Popup(R.layout.popup_tutorial_save_attached_frame, this, ma);
        tutorialPopupDialog = tutorialPopup.dialog;

        Button close = tutorialPopup.inflator.findViewById(R.id.close_button);
        close.setOnClickListener((v) -> {
            tutorialPopup.dialog.hide();
            //TODO Cancel localize if running
        });

        Button button_retry = tutorialPopup.inflator.findViewById(R.id.button_retry);

        ImageView mapframe_position = tutorialPopup.inflator.findViewById(R.id.mapframe_position);
        TextView map_frame_text = tutorialPopup.inflator.findViewById(R.id.map_frame_text);
        Button button_at_home = tutorialPopup.inflator.findViewById(R.id.button_at_home);

        ImageView localizing_at_home = tutorialPopup.inflator.findViewById(R.id.localizing_at_home);
        LottieAnimationView localizing_at_homeBis = tutorialPopup.inflator.findViewById(R.id.localizing_at_homeBis);
        TextView localizing_text = tutorialPopup.inflator.findViewById(R.id.localizing_text);
        ImageView localizing_error = tutorialPopup.inflator.findViewById(R.id.localizing_error);

        ImageView map_position = tutorialPopup.inflator.findViewById(R.id.map_position);
        TextView map_position_text = tutorialPopup.inflator.findViewById(R.id.map_position_text);
        Button button_stop_save = tutorialPopup.inflator.findViewById(R.id.button_stop_save);

        EditText position_name = tutorialPopup.inflator.findViewById(R.id.position_name);
        TextView save_position_text = tutorialPopup.inflator.findViewById(R.id.save_position_text);
        ImageView save_position_image = tutorialPopup.inflator.findViewById(R.id.save_position_image);
        Button button_yes = tutorialPopup.inflator.findViewById(R.id.button_yes);

        LinearLayoutCompat dot_follow_one = tutorialPopup.inflator.findViewById(R.id.dot_follow_one);
        LinearLayoutCompat dot_follow_two = tutorialPopup.inflator.findViewById(R.id.dot_follow_two);
        LinearLayoutCompat dot_follow_three = tutorialPopup.inflator.findViewById(R.id.dot_follow_three);
        LinearLayoutCompat dot_follow_four = tutorialPopup.inflator.findViewById(R.id.dot_follow_four);


        button_at_home.setOnClickListener(uselessV -> {
            mapframe_position.setVisibility(View.GONE);
            map_frame_text.setVisibility(View.GONE);
            button_at_home.setVisibility(View.GONE);
            dot_follow_one.setVisibility(View.GONE);

            localizing_at_home.setVisibility(View.VISIBLE);
            localizing_at_homeBis.setVisibility(View.VISIBLE);
            localizing_text.setVisibility(View.VISIBLE);
            dot_follow_two.setVisibility(View.VISIBLE);

            ma.robotHelper.localizeAndMapHelper.addOnFinishedLocalizingListener(result -> {
                ma.robotIsLocalized.set(result == LocalizeAndMapHelper.LocalizationStatus.LOCALIZED);
                ma.runOnUiThread(() -> {
                    if (result == LocalizeAndMapHelper.LocalizationStatus.LOCALIZED) {
                        localizing_at_home.setVisibility(View.GONE);
                        localizing_at_homeBis.setVisibility(View.GONE);
                        localizing_text.setVisibility(View.GONE);
                        button_retry.setVisibility(View.GONE);
                        dot_follow_two.setVisibility(View.GONE);

                        map_position.setVisibility(View.VISIBLE);
                        map_position_text.setVisibility(View.VISIBLE);
                        button_stop_save.setVisibility(View.VISIBLE);
                        dot_follow_three.setVisibility(View.VISIBLE);

                        ma.robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();
                    } else if (result == LocalizeAndMapHelper.LocalizationStatus.MAP_MISSING) {
                        noMapToLoad();
                        ma.robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();
                    } else if (result == LocalizeAndMapHelper.LocalizationStatus.FAILED) {
                        button_retry.setVisibility(View.VISIBLE);
                        localizing_at_homeBis.setVisibility(View.GONE);
                        localizing_error.setVisibility(View.VISIBLE);
                        button_retry.setOnClickListener(view -> {
                            ma.startLocalizing();
                            button_retry.setVisibility(View.GONE);
                            localizing_at_homeBis.setVisibility(View.VISIBLE);
                            localizing_error.setVisibility(View.GONE);
                        });
                        Log.d(TAG, "popupTutorialSaveAttachedFrame: Unable to localize in Map");
                    }
                });
            });
            ma.startLocalizing();
        });


        button_stop_save.setOnClickListener((v) -> {
            map_position.setVisibility(View.GONE);
            map_position_text.setVisibility(View.GONE);
            button_stop_save.setVisibility(View.GONE);
            dot_follow_three.setVisibility(View.GONE);

            position_name.setVisibility(View.VISIBLE);
            save_position_text.setVisibility(View.VISIBLE);
            save_position_image.setVisibility(View.VISIBLE);
            button_yes.setVisibility(View.VISIBLE);
            dot_follow_four.setVisibility(View.VISIBLE);

            tutorialPopup.inflator.findViewById(R.id.button_yes).setOnClickListener((w) -> {
                if (!position_name.getText().toString().equalsIgnoreCase("")) {
                    ma.saveLocation(position_name.getText().toString()).andThenConsume(voidFuture -> {
                        Log.d(TAG, "popupTutorialSaveAttachedFrame: ButtonYes");
                        ma.runOnUiThread(() -> {
                            tutorialPopup.dialog.hide();
                            locationsListModified = true;
                            displayLocations();
                        });
                    });
                }
            });
        });
        tutorialPopup.dialog.show();
        tutorialPopup.dialog.getWindow().setAttributes(tutorialPopup.lp);
    }

    private void displayLocations() {
        Log.d(TAG, "displayLocations: started");
        if (ma.savedLocations.isEmpty()) {
            try {
                ma.loadLocations().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (ma.savedLocations.isEmpty()) noLocationsToLoad();
        else {

            LayoutInflater inflaterPopup = this.getLayoutInflater();
            final LinearLayout linearLayout = localView.findViewById(R.id.container);
            linearLayout.removeAllViews();
            Iterator it = ma.savedLocations.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                LinearLayout v = (LinearLayout) inflaterPopup.inflate(R.layout.item_location_list, null);
                TextView locationName = (TextView) v.getChildAt(1);
                locationName.setText(pair.getKey().toString());

                ImageView separationList = new ImageView(this.getContext());
                separationList.setImageResource(R.drawable.ic_separation_list);
                Button deleteButton = (Button) v.getChildAt(2);
                deleteButton.setOnClickListener((useless) -> {
                    Log.d(TAG, "displayLocations: " + pair.getKey().toString());
                    linearLayout.removeView(v);
                    linearLayout.removeView(separationList);
                    ma.savedLocations.values().remove(pair.getValue());
                    locationsListModified = true;
                    if (ma.savedLocations.isEmpty()) noLocationsToLoad();
                });
                linearLayout.addView(v);
                linearLayout.addView(separationList);
            }
        }
        Log.d(TAG, "displayLocations: finished");
    }

    private void noLocationsToLoad() {
        Popup noLocationsPopup = new Popup(R.layout.popup_no_locations_to_load, this, ma);
        Button close = noLocationsPopup.inflator.findViewById(R.id.close_button);
        close.setOnClickListener((v) -> noLocationsPopup.dialog.hide());
        noLocationsPopup.dialog.show();
    }

    private void noMapToLoad() {
        tutorialPopupDialog.hide();
        Popup noMapPopup = new Popup(R.layout.popup_no_map_to_load, this, ma);
        Button close = noMapPopup.inflator.findViewById(R.id.close_button);
        close.setOnClickListener((v) -> {
            noMapPopup.dialog.hide();
            ma.setFragment(new SetupFragment(), true);
        });
        noMapPopup.dialog.show();
    }

    public void displaypPopupLocationsSaved() {
        savingLocationsPopup = new Popup(R.layout.popup_map_saved, this, ma);
        Button close_button = savingLocationsPopup.inflator.findViewById(R.id.close_button);
        close_button.setOnClickListener((v) -> {
            savingLocationsPopup.dialog.hide();
        });
        locationsSaved = savingLocationsPopup.inflator.findViewById(R.id.saved);
        progressBar = savingLocationsPopup.inflator.findViewById(R.id.progressbar);
        saving_text = savingLocationsPopup.inflator.findViewById(R.id.saving_text);

        savingLocationsPopup.dialog.show();
        savingLocationsPopup.dialog.getWindow().setAttributes(savingLocationsPopup.lp);
    }
}
