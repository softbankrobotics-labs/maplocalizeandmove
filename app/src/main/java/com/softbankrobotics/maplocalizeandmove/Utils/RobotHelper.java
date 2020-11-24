package com.softbankrobotics.maplocalizeandmove.Utils;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.geometry.TransformTime;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.locale.Language;
import com.aldebaran.qi.sdk.object.locale.Locale;
import com.aldebaran.qi.sdk.object.locale.Region;
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
        if (holder != null) {
            releaseAbilities();
        }
    }

    /**
     * Get the charging flap state.
     *
     * @return Charging flap state: "True" if opened, "False" if closed
     */
    public boolean getFlapState() {
        boolean flapState = chargingFlap.async().getState().getValue().getOpen();
        Log.d(TAG, "getFlapState: Is opened ? :" + flapState);
        return flapState;
    }

    /**
     * Get the charging flap state and ask to close it if opened.
     *
     * @return Charging flap state: "True" if opened, "False" if closed
     */
    public boolean askToCloseIfFlapIsOpened() {
        boolean isFlapOpened = getFlapState();
        if (isFlapOpened)
            say("Please close my charging flap then start the action you want");
        return isFlapOpened;
    }

    /**
     * Hold Autonomous abilities, BasicAwareness and BackgroundMovement if needed
     *
     * @param withBackgroundMovement "true" to hold BackgroundMovement or "false"
     * @return Future of the Holder
     */
    public Future<Void> holdAbilities(boolean withBackgroundMovement) {
        // Build the holder for the abilities.
        return releaseAbilities().thenCompose(voidFuture -> {
            if (withBackgroundMovement) {
                holder = HolderBuilder.with(qiContext)
                        .withAutonomousAbilities(
                                AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                                AutonomousAbilitiesType.BASIC_AWARENESS
                        )
                        .build();
            } else {
                holder = HolderBuilder.with(qiContext)
                        .withAutonomousAbilities(
                                AutonomousAbilitiesType.BASIC_AWARENESS
                        )
                        .build();
            }
            //Log.d(TAG, "holdAbilities");
            // Hold the abilities asynchronously.
            return holder.async().hold();
        });
    }

    /**
     * Release Autonomous abilities.
     *
     * @return Future of the Holder
     */
    public Future<Void> releaseAbilities() {
        // Release the holder asynchronously.
        if (holder != null) {
            //Log.d(TAG, "releaseAbilities");
            return holder.async().release().andThenConsume(aVoid -> {
                holder = null;
            });
        } else {
            //Log.d(TAG, "releaseAbilities: No holder to release");
            return Future.of(null);
        }
    }

    /**
     * Make Pepper say a sentence.
     *
     * @param text to be said by Pepper
     * @return
     */
    public Future<Void> say(final String text) {
        return SayBuilder.with(qiContext)
                .withText(text)
                .withLocale(new Locale(Language.ENGLISH, Region.UNITED_STATES))
                .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                .buildAsync().andThenCompose(say -> {
                    Log.d(TAG, "Say started : " + text);
                    return say.async().run();
                });
    }

    /**
     * Get the Frame of the origin of the map.
     *
     * @return Frame of MapFrame
     */
    public Frame getMapFrame() {
        return mapping.async().mapFrame().getValue();
    }

    /**
     * Get an AttachedFrame of the actual robot position relatively to the MapFrame.
     *
     * @return AttachedFrame of the robot position
     */
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
