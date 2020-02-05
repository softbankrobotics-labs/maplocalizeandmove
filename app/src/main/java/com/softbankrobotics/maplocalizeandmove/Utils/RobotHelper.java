package com.softbankrobotics.maplocalizeandmove.Utils;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.geometry.TransformTime;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.power.FlapSensor;
import com.aldebaran.qi.sdk.object.power.Power;

public class RobotHelper {
    private static final String TAG = "MSI_RobotHelper";
    public Holder holder; // for holding autonomous abilities when required
    public GoToHelper goToHelper;
    public LocalizeAndMapHelper localizeAndMapHelper;
    private Actuation actuation; // Store the Actuation service.
    private Mapping mapping; // Store the Mapping service.
    private QiContext qiContext; // The QiContext provided by the QiSDK.
    private FlapSensor chargingFlap;

    public RobotHelper() {
        goToHelper = new GoToHelper();
        localizeAndMapHelper = new LocalizeAndMapHelper();
    }

    public void onRobotFocusGained(QiContext qc) {
        qiContext = qc;
        actuation = qiContext.getActuation();
        mapping = qiContext.getMapping();
        goToHelper.onRobotFocusGained(qiContext);
        localizeAndMapHelper.onRobotFocusGained(qiContext);
        Power power = qiContext.getPower();
        chargingFlap = power.getChargingFlap();
    }

    public void onRobotFocusLost() {
        // Remove the QiContext.
        qiContext = null;
        goToHelper.onRobotFocusLost();
        localizeAndMapHelper.onRobotFocusLost();
    }

    public boolean getFlapState() {
        boolean flapState = chargingFlap.async().getState().getValue().getOpen();
        Log.d(TAG, "getFlapState: Is opened ? :" + flapState);
        return flapState;
    }

    public Future<Void> holdAbilities(boolean withBackgroundMovement) {
        // Build the holder for the abilities.

        if (holder != null) releaseAbilities();

        if (withBackgroundMovement) {
            holder = HolderBuilder.with(qiContext)
                    .withAutonomousAbilities(
                            AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                            AutonomousAbilitiesType.BASIC_AWARENESS//,
                            //AutonomousAbilitiesType.AUTONOMOUS_BLINKING
                    )
                    .build();
        } else {
            holder = HolderBuilder.with(qiContext)
                    .withAutonomousAbilities(
                            AutonomousAbilitiesType.BASIC_AWARENESS
                    )
                    .build();
        }

        // Hold the abilities asynchronously.
        return holder.async().hold();
    }

    public Future<Void> releaseAbilities() {
        // Release the holder asynchronously.
        try {
            return holder.async().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Frame getMapFrame() {
        return mapping.async().mapFrame().getValue();
    }

    public Future<AttachedFrame> createAttachedFrameFromCurrentPosition() {
        // Get the robot frame asynchronously.
        return actuation.async()
                .robotFrame()
                .andThenApply(robotFrame -> {
                    Frame mapFrame = getMapFrame();

                    // Transform between the current robot location (robotFrame) and the mapFrame
                    TransformTime transformTime = robotFrame.computeTransform(mapFrame);

                    // Create an AttachedFrame representing the current robot frame relatively to the MapFrame
                    return mapFrame.makeAttachedFrame(transformTime.getTransform());
                });
    }
}
