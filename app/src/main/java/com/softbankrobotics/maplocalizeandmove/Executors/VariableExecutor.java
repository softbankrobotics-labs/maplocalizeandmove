package com.softbankrobotics.maplocalizeandmove.Executors;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;
import com.softbankrobotics.maplocalizeandmove.MainActivity;

import java.util.List;

/**
 * VariableExecutor is used when a qiVariable is modified in the qiChat and
 * you want to have feedback on the tablet
 * Triggered in qiChat as follow : ^execute( VariableExecutor, variableName, variableValue)
 */

public class VariableExecutor extends BaseQiChatExecutor {
    private final MainActivity ma;
    private String TAG = "MSI_VariableExecutor";

    public VariableExecutor(QiContext qiContext, MainActivity mainActivity) {
        super(qiContext);
        this.ma = mainActivity;
    }

    @Override
    public void runWith(List<String> params) {
        /*String variableName;
        String variableValue;
        if (params == null || params.isEmpty()) {
            return;
        }else{
            variableName = params.get(0);
            if(params.size() == 2){
                variableValue = params.get(1);
            }else{
                Log.d(TAG, "no value specified for variable : " + variableName);
                return;
            }
        }
        Log.d(TAG,"variableName :" + variableName);
        switch (variableName){
            case ("qiVariable"):
                ProductionFragment productionFragment  = (ProductionFragment) ma.getFragment();
                productionFragment.setTextQiVariableValue(variableValue);
                break;
            default:
                Log.d(TAG, "I don't know this variable");
        }*/
    }

    @Override
    public void stop() {

    }
}