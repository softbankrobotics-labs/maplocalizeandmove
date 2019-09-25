package com.softbankrobotics.maplocalizeandmove.Utils;

import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.softbankrobotics.maplocalizeandmove.MainActivity;

public class CountDownNoInteraction extends CountDownTimer {

    private String TAG = "MSI_NoInteraction";
    private Fragment fragment;
    private MainActivity mainActivity;

    public CountDownNoInteraction(MainActivity mainActivity, Fragment fragmentToSet, long millisUtilEnd, long countDownInterval){
        super(millisUtilEnd, countDownInterval);
        this.fragment = fragmentToSet;
        this.mainActivity = mainActivity;
    }


    @Override
    public void onTick(long millisUntilFinished) {
        //Log.d(TAG,"Millis until end : " + millisUntilFinished);
    }

    @Override
    public void onFinish() {
        Log.d(TAG,"Timer Finished");
        //mainActivity.setFragment(fragment);
    }

    public void reset(){
        Log.d(TAG,"Timer Reset");
        super.cancel();
        super.start();
    }
}
