package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;

public class ProductionFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "MSI_ScreenTwoFragment";
    private MainActivity ma;
    /*private TextView  qiVariableValue;*/

    /**
     * inflates the layout associated with this fragment
     * if an application theme is set it will be applied to this fragment.
     */

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_production;
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
        view.findViewById(R.id.back_button).setOnClickListener((v) ->
                ma.setFragment(new MainFragment(), true));
        Button startLocalizeButton =  view.findViewById(R.id.button_start_localize);

        startLocalizeButton.setOnClickListener((v) ->
                ma.setFragment(new LocalizeRobotFragment(), false));
        Button goToFrameButton = view.findViewById(R.id.goToFrame);
        goToFrameButton.setOnClickListener((v) -> ma.setFragment(new GoToFrameFragment(), false));

        if (ma.robotIsLocalized.get()){
            goToFrameButton.setEnabled(true);
            goToFrameButton.setAlpha(1);
            goToFrameButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_icn_goto_frame, 0, 0);
            startLocalizeButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_icn_localize_robot_burgermenu_oklocation, 0, 0);
        }

}

    /*public void setTextQiVariableValue(String value){
        ma.runOnUiThread(() -> qiVariableValue.setText(value));
    }*/
}
