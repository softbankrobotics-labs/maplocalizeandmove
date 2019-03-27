package com.softbankrobotics.maplocalizeandmove;

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

class RobotHelper {
    private static final String TAG = "RobotHelper";
    private Holder holder; // for holding autonomous abilities when required
    private Actuation actuation; // Store the Actuation service.
    private Mapping mapping; // Store the Mapping service.

    private QiContext qiContext; // The QiContext provided by the QiSDK.
    GoToHelper goToHelper;
    LocalizeAndMapHelper localizeAndMapHelper;

    RobotHelper() {
        goToHelper = new GoToHelper();
        localizeAndMapHelper = new LocalizeAndMapHelper();
    }

    void onRobotFocusGained(QiContext qc) {
        qiContext = qc;
        actuation = qiContext.getActuation();
        mapping = qiContext.getMapping();
        goToHelper.onRobotFocusGained(qiContext);
        localizeAndMapHelper.onRobotFocusGained(qiContext);
    }

    void onRobotFocusLost() {
        // Remove the QiContext.
        qiContext = null;
        goToHelper.onRobotFocusLost();
        localizeAndMapHelper.onRobotFocusLost();
    }

    Future<Void> holdAbilities() {
        // Build the holder for the abilities.
        holder = HolderBuilder.with(qiContext)
                .withAutonomousAbilities(
                        //AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                        AutonomousAbilitiesType.BASIC_AWARENESS//,
                        //AutonomousAbilitiesType.AUTONOMOUS_BLINKING
                )
                .build();

        // Hold the abilities asynchronously.
        return holder.async().hold();
    }

    Future<Void> releaseAbilities() {
        // Release the holder asynchronously.
        try {
            return holder.async().release();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    Frame getMapFrame() {
        return mapping.async().mapFrame().getValue();
    }

    Future<AttachedFrame> createAttachedFrameFromCurrentPosition() {
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
