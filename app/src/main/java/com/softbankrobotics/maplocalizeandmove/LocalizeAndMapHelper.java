package com.softbankrobotics.maplocalizeandmove;

import android.util.Log;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.ExplorationMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.LocalizationStatus;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LocalizeAndMap;
import java.util.ArrayList;
import java.util.List;

/***
 * <p>
 * This helper simplifies the use of the Localize and LocalizeAndMap actions.
 * It will hide the build of those actions, and make sure to keep you updated when the robot
 * is mapping, localizing. And make sure you don't run both at the same time.
 * </p><br/><p>
 * <strong>Usage:</strong><br/>
 * 1) Create an instance in "onCreate"<br/>
 * 2) Call onRobotFocusGained in "onRobotFocusGained"<br/>
 * 3) Call onRobotFocusLost in "onRobotFocusLost"<br/>
 * 4) Call localize or localizeAndMap whenever you want the robot to locate itself.<br/>
 * </p>
 */
class LocalizeAndMapHelper {


    private QiContext qiContext; // The QiContext provided by the QiSDK.
    private static final String TAG = "LocalizeAndMapHelper";

    private Future<Void> currentlyRunningLocalize;
    private LocalizeAndMap currentLocalizeAndMap;
    private String currentExplorationMap;

    private List<onStartedLocalizingListener> startedLocalizingListeners;
    private List<onFinishedLocalizingListener> finishedLocalizingListeners;
    private List<onStartedMappingListener> startedMappingListeners;
    private List<onFinishedMappingListener> finishedMappingListeners;
    private Localize builtLocalize;

    /**
     * Constructor: call me in your `onCreate`
     */
    LocalizeAndMapHelper() {
        startedLocalizingListeners = new ArrayList<>();
        finishedLocalizingListeners = new ArrayList<>();
        startedMappingListeners = new ArrayList<>();
        finishedMappingListeners = new ArrayList<>();
        currentExplorationMap = "";
        builtLocalize = null;
    }

    /**
     * Call me in your `onRobotFocusGained`
     * @param qc the qiContext provided to your Activity
     */
    void onRobotFocusGained(QiContext qc) {
        qiContext = qc;
    }

    /**
     * Call me in your `onRobotFocusLost`
     */
    void onRobotFocusLost() {
        // Remove the QiContext.
        qiContext = null;
    }

    /**
     * Force-feed the map in case you saved it into a file.
     * @param map the map you previously saved from `getMap`
     */
    void setMap(String map) {
        builtLocalize = null;
        currentExplorationMap = map;
    }

    /**
     * Dump and extract the current map from the current mapping.
     * @return the map as a String, for you to backup into a file.
     */
    String getMap() {
        return currentExplorationMap;
    }

    /**
     * checks if a previous localize or localizeAndMap was running and cancels it. This is
     * for making sure you don't run both at the same time (not possible on Pepper)
     * @return Future that will complete when the action is cancelled.
     */
    private Future<Void> checkAndCancelCurrentLocalize() {
        if (currentlyRunningLocalize == null) return Future.of(null);
        currentlyRunningLocalize.requestCancellation();
        return currentlyRunningLocalize;
    }

    /**
     * start localizing: the robot will look around and try to find out where it is.<br/>
     * <strong>important:</strong> your need to load a Map before being able to localize, so don't
     * forget to call setMap or run a localizeAndMap.
     * @return Future that will complete when you stop localize
     */
    Future<Void> localize() {
        raiseStartedLocalizing();
        return checkAndCancelCurrentLocalize()
                .thenApply(aUselessFuture -> {
                    if(builtLocalize != null) {
                        return builtLocalize;
                    } else {
                        ExplorationMap explorationMap = ExplorationMapBuilder.with(qiContext).withMapString(currentExplorationMap).build();
                        builtLocalize = LocalizeBuilder.with(qiContext).withMap(explorationMap).build();
                        return builtLocalize;
                    }
                })
                .andThenCompose(localize -> {
                    builtLocalize = localize;
                    Log.d(TAG, "localize built successfully, running...");
                    localize.addOnStatusChangedListener(status -> checkStatusAndRaiseLocalized(status));
                    currentlyRunningLocalize = localize.async().run();
                    return currentlyRunningLocalize;
                })
                .thenConsume(finishedLocalize -> {
                    if(finishedLocalize.hasError()) {
                        Log.w(TAG, "Failed to localize in map", finishedLocalize.getError());
                        raiseFinishedLocalizing(false);
                    } else {
                        Log.d(TAG, "localize finished.");
                        raiseFinishedLocalizing(false);
                    }
                });
    }

    /**
     * start localizeAndMap: the robot will record its current position and continue mapping its
     * environment. In this mode, any obstacle, such as a human on the way, will be counted as an
     * definitive obstacle in the map, same as a wall.
     * <br/>
     * Use this mode to push the robot around to make it learn its environment. Make sure to well
     * stay behind the robot.
     * @return Future that will complete when you stop localizeAndMap
     */
    Future<Void> localizeAndMap()  {
        raiseStartedMapping();
        return checkAndCancelCurrentLocalize()
                .thenCompose(aUselessVoid -> LocalizeAndMapBuilder.with(qiContext).buildAsync())
                .andThenCompose(localizeAndMap -> {
                    Log.d(TAG, "localizeAndMap built successfully, running...");
                    localizeAndMap.addOnStatusChangedListener(status -> checkStatusAndRaiseLocalized(status));
                    currentLocalizeAndMap = localizeAndMap;
                    currentlyRunningLocalize = localizeAndMap.async().run();
                    return currentlyRunningLocalize;
                })
                .thenConsume(finishedLocalizeAndMap -> {

                    currentLocalizeAndMap.async().dumpMap()
                            .andThenConsume(dumpedMap -> setMap(dumpedMap.serialize()))
                            .andThenConsume(f -> currentLocalizeAndMap = null)
                            .andThenConsume(f->{
                                if (finishedLocalizeAndMap.hasError()) {
                                    Log.w(TAG, "LocalizeAndMap finished with error: ", finishedLocalizeAndMap.getError());
                                    raiseFinishedMapping(false);
                                } else {
                                    Log.d(TAG, "LocalizeAndMap finished with success.");
                                    raiseFinishedMapping(true);
                                }

                            });

                });
    }

    /**
     * Stop the currently running localize or localizeAndMap.
     * @return A Future that will complete when the action is cancelled.
     */
    Future<Void> stopCurrentAction() {
        return checkAndCancelCurrentLocalize();
    }

    /**
     * Callback for localize status changes. When the status is "localized" then it raises
     * the callback for the UI. 
     */
    private void checkStatusAndRaiseLocalized(LocalizationStatus status) {
        if (status == LocalizationStatus.LOCALIZED) {
            Log.d(TAG, "Robot is localized");
            raiseFinishedLocalizing(true);
        }
    }

    /**
     * Little helper for the UI to subscribe to the current state of localize/map
     * This has nothing to do with the robot, but is for helping in the MainActivity to enable
     * or disable functions during an action.
     */
    interface onStartedLocalizingListener {
        void onStartedLocalizing();
    }

    interface onFinishedLocalizingListener {
        void onFinishedLocalizing(boolean success);
    }

    interface onStartedMappingListener {
        void onStartedMapping();
    }

    interface onFinishedMappingListener {
        void onFinishedMapping(boolean success);
    }

    void addOnStartedLocalizingListener(onStartedLocalizingListener f) {
        startedLocalizingListeners.add(f);
    }

    void addOnFinishedLocalizingListener(onFinishedLocalizingListener f) {
        finishedLocalizingListeners.add(f);
    }

    void addOnStartedMappingListener(onStartedMappingListener f) {
        startedMappingListeners.add(f);
    }

    void addOnFinishedMappingListener(onFinishedMappingListener f) {
        finishedMappingListeners.add(f);
    }

    private void raiseStartedLocalizing() {
        for (onStartedLocalizingListener f: startedLocalizingListeners){
            f.onStartedLocalizing();
        }
    }

    private void raiseFinishedLocalizing(boolean success) {
        for (onFinishedLocalizingListener f: finishedLocalizingListeners){
            f.onFinishedLocalizing(success);
        }
    }

    private void raiseStartedMapping() {
        for (onStartedMappingListener f: startedMappingListeners){
            f.onStartedMapping();
        }
    }

    private void raiseFinishedMapping(boolean success) {
        for (onFinishedMappingListener f: finishedMappingListeners){
            f.onFinishedMapping(success);
        }
    }
}
