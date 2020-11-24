package com.softbankrobotics.maplocalizeandmove.Utils;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.actuation.OrientationPolicy;
import com.aldebaran.qi.sdk.object.actuation.PathPlanningPolicy;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.util.FutureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * This helper simplifies the use of the GoTo action.
 * It will hide the build of a Goto, the type of frame to provide, and the retries in case of
 * failure to reach the final destination.
 * </p><br/><p>
 * <strong>Usage:</strong><br/>
 * 1) Create an instance in "onCreate"<br/>
 * 2) Call onRobotFocusGained in "onRobotFocusGained"<br/>
 * 3) Call onRobotFocusLost in "onRobotFocusLost"<br/>
 * 4) Call goTo whenever you want the robot to move!<br/>
 * </p>
 */
public class GoToHelper {
    private static final String TAG = "MSI_GoToHelper";
    private static final int MAXTRIES = 15;
    private QiContext qiContext; // The QiContext provided by the QiSDK.
    private int tryCounter; // Counter to remember how many times the GoTo was tried already
    private final List<onStartedMovingListener> startedListeners;
    private final List<onFinishedMovingListener> finishedListeners;
    private Future<Void> currentGoToAction;

    /**
     * Constructor: call me in your `onCreate`.
     */
    GoToHelper() {
        startedListeners = new ArrayList<>();
        finishedListeners = new ArrayList<>();
    }

    /**
     * Call me in your `onRobotFocusGained`.
     *
     * @param qc the qiContext provided to your Activity
     */
    void onRobotFocusGained(QiContext qc) {
        // record the qicontext as it will be required for all actions.
        qiContext = qc;
    }

    /**
     * Call me in your `onRobotFocusLost`.
     */
    void onRobotFocusLost() {
        // Remove the QiContext as it's no longer working anyway.
        checkAndCancelCurrentGoto();
        qiContext = null;
    }

    /**
     * Call this function for the robot to go back home.
     * This requires the robot to be localized.
     * The robot will try up to 5 times to reach the destination.
     *
     * @return Future of the GoTo
     */
    public Future<Void> goToMapFrame(boolean goToStraight, boolean goToMaxSpeed, OrientationPolicy orientationPolicy) {
        // Helper not to have to find the mapFrame yourself
        return qiContext.getMapping()
                .async()
                // ...get the mapFrame
                .mapFrame()
                // ...and go to it!
                .andThenConsume(frame -> goTo(frame, goToStraight, goToMaxSpeed, orientationPolicy));
    }

    /**
     * Call this function for the robot to go back to the charging station.
     * Pepper will go 75cm in front of Charging Station.
     * This requires the robot to be localized.
     * The robot will try up to 15 times to reach the destination.
     *
     * @return Future of the GoTo
     */
    public Future<Void> goToChargingStation(AttachedFrame chargingStationAttachedFrame, boolean goToStraight, boolean goToMaxSpeed, OrientationPolicy orientationPolicy) {
        Mapping mapping = qiContext.getMapping();

        try {
            if (isDocked(chargingStationAttachedFrame).getValue()) {
                Log.d(TAG, "goToChargingStation: Already docked");
            } else {
                Transform transform = TransformBuilder.create().from2DTransform(0.75, 0.0, 3.14159);
                FreeFrame targetFrame = mapping.makeFreeFrame();
                targetFrame.update(chargingStationAttachedFrame.frame(), transform, 0L);

                return goTo(targetFrame.frame(), goToStraight, goToMaxSpeed, orientationPolicy);
            }
        } catch (Exception e) {
            Log.d(TAG, "goToChargingStation Exception: " + e.toString());
            raiseFinishedMoving(GoToStatus.FAILED);
        }
        return Future.of(null);
    }

    /**
     * Know if Pepper is docked on its charging station.
     *
     * @return True if docked, false otherwise
     *
     * Note : The first function isDocked() that is greyed out should be the normal way to know if
     * Pepper is docked or not. Unfortunately, mapping.chargingStationFrame() relies only on
     * Odometry (all the time) and not on VSLAM (when localized in a map).
     * As soon as Pepper is navigating, the position of the frame is not reliable.
     */
    /*public boolean isDocked() {
        Actuation actuation = qiContext.getActuation();
        Mapping mapping = qiContext.getMapping();
        Frame chargingStationFrame;
        try {
            chargingStationFrame = mapping.chargingStationFrame();
            Frame robotFrame = actuation.robotFrame();
            Vector3 stationPositionWrtRobot = chargingStationFrame.computeTransform(robotFrame).getTransform().getTranslation();
            Log.d(TAG, "Station position w.r.t. Robot: stationPositionWrtRobot.x:" + stationPositionWrtRobot.getX() + " , stationPositionWrtRobot.y : " + stationPositionWrtRobot.getY()); // should be [0,0] if pepper is docked!
            if ((stationPositionWrtRobot.getX() == 0 && stationPositionWrtRobot.getY() == 0)) {
                Log.d(TAG, "goToChargingStation: Already docked");
                return true;
            } else {
                Log.d(TAG, "goToChargingStation: Not docked");
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "goToChargingStation Exception: " + e.toString());
        }
        return false;
    }*/
    public Future<Boolean> isDocked(AttachedFrame stationAttachedFrame) {
        return qiContext.getActuationAsync().andThenCompose(actuation -> qiContext.getMappingAsync().andThenCompose(mapping -> {
            Frame robotFrame = actuation.robotFrame();
            //The code below in the Try can be used only if Pepper has been on its Charging Station once since its boot.
            // Cause : chargingStationFrame not in robot's Memory
            try {
                Frame chargingStationFrame;
                chargingStationFrame = mapping.chargingStationFrame();
                Vector3 stationPositionInMemorynWrtRobot = chargingStationFrame.computeTransform(robotFrame).getTransform().getTranslation();
                Log.d(TAG, "abs:stationPositioInMemorynWrtRobot.x:" + Math.abs(stationPositionInMemorynWrtRobot.getX()) + " , abs:stationPositioInMemorynWrtRobot.y : " + Math.abs(stationPositionInMemorynWrtRobot.getY())); // should be [0,0] if pepper is docked!
                if (stationPositionInMemorynWrtRobot.getX() == 0.0 && stationPositionInMemorynWrtRobot.getY() == 0.0) {
                    Log.d(TAG, "isDocked ? : Already docked");
                    return Future.of(true);
                } else if (stationAttachedFrame == null && Math.abs(stationPositionInMemorynWrtRobot.getX()) <= 0.4 && Math.abs(stationPositionInMemorynWrtRobot.getY()) <= 0.4) {
                    Log.d(TAG, "isDocked ? : seams Already docked");
                    return Future.of(true);
                }
            } catch (Exception e) {
                Log.d(TAG, "isDocked ? Exception: " + e.toString());
            }

            //The code below in the Try can be used only if Pepper has been localized once since its boot.
            // Cause : Cannot compute current Robot's position related to mapFrame of the map.
            try {
                Vector3 stationPositionWrtRobot = stationAttachedFrame.frame().computeTransform(robotFrame).getTransform().getTranslation();
                Log.d(TAG, "abs:stationPositionWrtRobot.x:" + Math.abs(stationPositionWrtRobot.getX()) + " , abs:stationPositionWrtRobot.y : " + Math.abs(stationPositionWrtRobot.getY())); // should be [0,0] if pepper is docked!
                if (Math.abs(stationPositionWrtRobot.getX()) <= 0.4 && Math.abs(stationPositionWrtRobot.getY()) <= 0.4) {
                    Log.d(TAG, "isDocked ? : Already docked");
                    return Future.of(true);
                } else {
                    Log.d(TAG, "isDocked ? : Not docked");
                    return Future.of(false);
                }
            } catch (Exception e) {
                Log.d(TAG, "isDocked ? Exception: " + e.toString());
                Log.d(TAG, "isDocked ? : Not docked");
                return Future.of(false);
            }
        }));
    }

    /**
     * Call this function for the robot to go to the provided AttachedFrame.
     * The robot will try up to 15 times to reach the destination.
     *
     * @return Future of the GoTo
     */
    public Future<Void> goTo(AttachedFrame attachedFrame, boolean goToStraight, boolean goToMaxSpeed, OrientationPolicy orientationPolicy) {
        // Helper not to have to extract the frame from the attachedFrame yourself
        return attachedFrame
                .async()
                // ...extract the Frame from the AttachedFrame
                .frame()
                // ...and go to it.
                .andThenCompose(frame -> goTo(frame, goToStraight, goToMaxSpeed, orientationPolicy));
    }

    /**
     * Call this function for the robot to go to the provided Frame.
     * The robot will try up to 15 times to reach the destination.
     *
     * @return Future of the GoTo
     */
    private Future<Void> goTo(Frame frame, boolean goToStraight, boolean goToMaxSpeed, OrientationPolicy orientationPolicy) {
        // This function builds and executes GoTo asynchronously.
        // reset the counter
        tryCounter = MAXTRIES;
        // raise UI listeners flag : the robot starts moving!
        raiseStartedMoving();

        PathPlanningPolicy pathPlanningPolicy;
        if (goToStraight) {
            pathPlanningPolicy = PathPlanningPolicy.STRAIGHT_LINES_ONLY;
            Log.d(TAG, "goTo: Straight");
        } else {
            pathPlanningPolicy = PathPlanningPolicy.GET_AROUND_OBSTACLES;
            Log.d(TAG, "goTo: Around Obstacles");
        }

        float speed;
        if (goToMaxSpeed) speed = 0.55f;
        else speed = 0.40f;
        // Build the GoTo
        return GoToBuilder.with(qiContext)
                .withFrame(frame)
                .withPathPlanningPolicy(pathPlanningPolicy)
                .withFinalOrientationPolicy(orientationPolicy)
                .withMaxSpeed(speed)
                .buildAsync()
                .andThenConsume(goToAction -> tryGoTo(goToAction));
    }

    /**
     * Run a GoTo and retry up to 15 times.
     * This method should never be called directly, call `goTo` instead.
     *
     * @param goToAction the pre-built GoTo action.
     */
    private void tryGoTo(GoTo goToAction) {
        // This function runs the GoTo asynchronously, then checks the success.
        currentGoToAction = goToAction.async().run()
                .thenConsume(goToResult -> {

                    if (goToResult.isSuccess()) {
                        Log.d(TAG, "GoTo successful");
                        raiseFinishedMoving(GoToStatus.FINISHED);

                    } else if (goToResult.isCancelled()) {
                        Log.d(TAG, "GoTo cancelled");
                        raiseFinishedMoving(GoToStatus.CANCELLED);
                    } else if (goToResult.hasError() && tryCounter > 0) {
                        tryCounter--;
                        Log.d(TAG, "Move ended with error: ", goToResult.getError());
                        Log.d(TAG, "Retrying " + tryCounter + " times.");
                        FutureUtils.wait((long) 1500, TimeUnit.MILLISECONDS)
                                .thenConsume(aUselessVoid -> tryGoTo(goToAction));
                    } else {
                        raiseFinishedMoving(GoToStatus.FAILED);
                        Log.d(TAG, "Move finished, but the robot did not reach destination.");
                    }
                });
    }


    /**
     * Cancel the current goTo if running
     *
     * @return Future of the currentGoTo
     */
    public Future<Void> checkAndCancelCurrentGoto() {
        tryCounter = 0;
        if (currentGoToAction == null) {
            return Future.of(null);
        }
        currentGoToAction.requestCancellation();
        //currentGoToAction.cancel(true);
        Log.d(TAG, "checkAndCancelCurrentGoto");
        return currentGoToAction;
    }

    /**
     * Little helper for the UI to subscribe to the current state of GoTo.
     * This has nothing to do with the robot, but is for helping in the MainActivity to enable
     * or disable functions during a move. For example: you should disable the possibility of
     * calling GoTo during another GoTo move.
     */
    public void addOnStartedMovingListener(onStartedMovingListener f) {
        startedListeners.add(f);
    }

    public void addOnFinishedMovingListener(onFinishedMovingListener f) {
        finishedListeners.add(f);
    }

    public void removeOnStartedMovingListeners() {
        startedListeners.clear();
    }

    public void removeOnFinishedMovingListeners() {
        finishedListeners.clear();
    }

    public void raiseFinishedMoving(GoToStatus success) {
        for (onFinishedMovingListener f : finishedListeners) {
            f.onFinishedMoving(success);
        }
    }

    private void raiseStartedMoving() {
        for (onStartedMovingListener f : startedListeners) {
            f.onStartedMoving();
        }
    }

    public interface onStartedMovingListener {
        void onStartedMoving();
    }

    public interface onFinishedMovingListener {
        void onFinishedMoving(GoToStatus success);
    }

    public enum GoToStatus {
        FAILED,
        CANCELLED,
        FINISHED
    }
}
