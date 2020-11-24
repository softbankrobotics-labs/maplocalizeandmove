package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;

public class SetupFragment extends Fragment {

    private static final String TAG = "MSI_SetupFragment";
    private MainActivity ma;

    /**
     * Inflates the layout associated with this fragment
     * If an application theme is set, it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_setup;
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
        view.findViewById(R.id.button_localize_and_map).setOnClickListener((v) -> {
            if (!ma.robotHelper.askToCloseIfFlapIsOpened()) {
                ma.setFragment(new LocalizeAndMapFragment(), false);
            }
        });

        Button menuSaveLocations = view.findViewById(R.id.menu_save_location);
        menuSaveLocations.setOnClickListener((v) ->
                ma.setFragment(new SaveLocationsFragment(), false));

        view.findViewById(R.id.back_button).setOnClickListener((v) ->
                ma.setFragment(new MainFragment(), true));
    }
}
