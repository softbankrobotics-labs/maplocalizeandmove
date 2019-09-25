package com.softbankrobotics.maplocalizeandmove.Utils;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
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
    private QiContext qiContext; // The QiContext provided by the QiSDK.
    private int tryCounter; // Counter to remember how many times the GoTo was tried already
    private List<onStartedMovingListener> startedListeners;
    private List<onFinishedMovingListener> finishedListeners;
    private static String TAG = "MSI_GoToHelper";
    private static int MAXTRIES = 5;

    /**
     * Constructor: call me in your `onCreate`
     */
    GoToHelper() {
        startedListeners = new ArrayList<>();
        finishedListeners = new ArrayList<>();
    }

    /**
     * Call me in your `onRobotFocusGained`
     * @param qc the qiContext provided to your Activity
     */
    void onRobotFocusGained(QiContext qc) {
        // record the qicontext as it will be required for all actions.
        qiContext = qc;
    }

    /**
     * Call me in your `onRobotFocusLost`
     */
    void onRobotFocusLost() {
        // Remove the QiContext as it's no longer working anyway.
        qiContext = null;
    }

    /**
     * Call this function for the robot to go back home.
     * This requires the robot to be localized.
     * The robot will try up to 5 times to reach the destination.
     * @return Future of the GoTo
     */
    public Future<Void> goToMapFrame() {
        // Helper not to have to find the mapFrame yourself
        return qiContext.getMapping()
                .async()
                // ...get the mapFrame
                .mapFrame()
                // ...and go to it!
                .andThenConsume(frame -> goTo(frame));
    }

    /**
     * Call this function for the robot to go to the provided AttachedFrame.
     * The robot will try up to 5 times to reach the destination.
     * @return Future of the GoTo
     */
    public Future<Void> goTo(AttachedFrame attachedFrame) {
        // Helper not to have to extract the frame from the attachedFrame yourself
        return attachedFrame
                .async()
                // ...extract the Frame from the AttachedFrame
                .frame()
                // ...and go to it.
                .andThenConsume(frame -> goTo(frame));
    }

    /**
     * Call this function for the robot to go to the provided Frame.
     * The robot will try up to 5 times to reach the destination.
     * @return Future of the GoTo
     */
    public Future<Void> goTo(Frame frame) {
        // This function builds and executes GoTo asynchronously.
        // reset the counter
        tryCounter = MAXTRIES;
        // raise UI listeners flag : the robot starts moving!
        raiseStartedMoving();
        // Build the GoTo
        return GoToBuilder.with(qiContext)
                .withFrame(frame)
                .buildAsync()
                .andThenConsume(goTo -> tryGoTo(goTo));
    }

    /**
     * Run a GoTo and retry up to 5 times.
     * This method should never be called directly, call `goTo` instead.
     * @param goTo the pre-built GoTo action.
     */
    private void tryGoTo(GoTo goTo) {
        // This function runs the GoTo asynchronously, then checks the success.
        goTo.async()
                .run()
                .thenConsume(goToResult -> {

                    /*
                     * In case GoTo is a success, we call once again, just to ensure it is precisely at destination.
                     * In case of error, we retry up to 5 times before really giving up.
                     */

                    if (goToResult.isSuccess()) {
                        Log.d(TAG, "Move successful, just retrying once to confirm.");
                        FutureUtils.wait((long) 1500, TimeUnit.MILLISECONDS)
                                .thenConsume(aUselessFuture -> goTo.async().run())
                                .thenConsume(aUselessFuture -> raiseFinishedMoving(true))
                                .thenConsume(aUselessFuture -> Log.d(TAG, "GoTo Finished!"));

                    } else if (goToResult.hasError() && tryCounter > 0) {
                        tryCounter--;
                        Log.d(TAG, "Move ended with error: ", goToResult.getError());
                        Log.d(TAG, "Retrying " + tryCounter + " times.");
                        FutureUtils.wait((long) 1500, TimeUnit.MILLISECONDS)
                                .thenConsume(aUselessVoid -> tryGoTo(goTo));

                    } else {
                        raiseFinishedMoving(false);
                        Log.d(TAG, "Move finished, but the robot did not reach destination.");
                    }
                });
    }

    /**
     * Little helper for the UI to subscribe to the current state of GoTo
     * This has nothing to do with the robot, but is for helping in the MainActivity to enable
     * or disable functions during a move. For example: you should disable the possibility of GoTo
     * during another GoTo move.
     */
    public interface onStartedMovingListener {
        void onStartedMoving();
    }

    public interface onFinishedMovingListener {
        void onFinishedMoving(boolean success);
    }

    public void addOnStartedMovingListener(onStartedMovingListener f) {
        startedListeners.add(f);
    }

    public void addOnFinishedMovingListener(onFinishedMovingListener f) {
        finishedListeners.add(f);
    }

    private void raiseFinishedMoving(boolean success) {
        for (onFinishedMovingListener f: finishedListeners){
            f.onFinishedMoving(success);
        }
    }

    private void raiseStartedMoving() {
        for (onStartedMovingListener f: startedListeners){
            f.onStartedMoving();
        }
    }
}
