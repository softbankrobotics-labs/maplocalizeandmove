package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;

public class MainFragment extends Fragment {

    private static final String TAG = "MSI_MainFragment";
    private MainActivity ma;

    /**
     * Inflates the layout associated with this fragment
     * If an application theme is set, it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_main;
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
            Log.d(TAG, "could not get mainActivity, can't create fragment");
            return null;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.button_setup_mode).setOnClickListener(
                (v) -> ma.setFragment(new SetupFragment(), false));
        view.findViewById(R.id.button_production_mode).setOnClickListener(
                (v) -> ma.setFragment(new ProductionFragment(), false));
    }

}
