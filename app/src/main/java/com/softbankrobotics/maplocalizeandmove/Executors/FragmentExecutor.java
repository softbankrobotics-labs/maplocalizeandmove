package com.softbankrobotics.maplocalizeandmove.Executors;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;
import com.softbankrobotics.maplocalizeandmove.Fragments.MainFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SetupFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.ProductionFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SplashFragment;
import com.softbankrobotics.maplocalizeandmove.MainActivity;

import java.util.List;

/**
 * FragmentExecutor sets the fragment to be displayed in the placeholder of the main activity
 * This executor is added to the Chat(see main activity)
 * Triggered in qiChat as follow : ^execute( FragmentExecutor, frag_XXXX )
 */

public class FragmentExecutor extends BaseQiChatExecutor {
    private final MainActivity ma;
    private String TAG = "MSI_FragmentExecutor";

    public FragmentExecutor(QiContext qiContext, MainActivity mainActivity) {
        super(qiContext);
        this.ma = mainActivity;
    }

    @Override
    public void runWith(List<String> params) {
        String fragmentName;
        String optionalData; //use this if you need to pass on data when setting the fragment.
        if (params == null || params.isEmpty()) {
            return;
        }else{
            fragmentName = params.get(0);
            if(params.size() == 2){
                optionalData = params.get(1);
            }
        }
        Fragment fragment;
        Log.d(TAG,"fragmentName :" + fragmentName);
        switch (fragmentName){
            case ("frag_main"):
                fragment = new MainFragment();
                break;
            case ("frag_setup"):
                fragment = new SetupFragment();
                break;
            case ("frag_production"):
                fragment = new ProductionFragment();
                break;
            case ("frag_splash_screen"):
                fragment = new SplashFragment();
                break;
            default:
                fragment = new MainFragment();
        }
        ma.setFragment(fragment, false);
    }

    @Override
    public void stop() {

    }
}