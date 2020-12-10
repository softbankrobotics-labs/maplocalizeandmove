package com.softbankrobotics.maplocalizeandmove;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.autonomousrecharge.AutonomousRecharge;
import com.aldebaran.qi.sdk.autonomousrecharge.AutonomousRechargeListeners;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.OrientationPolicy;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.streamablebuffer.StreamableBuffer;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.softbankrobotics.maplocalizeandmove.Fragments.GoToFrameFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.LoadingFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.LocalizeAndMapFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.LocalizeRobotFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.MainFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SaveLocationsFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SplashFragment;
import com.softbankrobotics.maplocalizeandmove.Utils.LocalizeAndMapHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.RobotHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.SaveFileHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.Vector2theta;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks, Serializable {

    private static final String TAG = "MSI_MapLocalizeAndMove";
    // Path to the directory where to save the Map and the Points of interest File.
    public static final String filesDirectoryPath = "/sdcard/Maps";
    public static final String mapFileName = "mapData.txt";
    private static final String locationsFileName = "points.json";
    // Permissions
    private static final String RECHARGE_PERMISSION = "com.softbankrobotics.permission.AUTO_RECHARGE"; // recharge permission
    private static final int MULTIPLE_PERMISSIONS = 2;
    // Store the saved locations.
    public TreeMap<String, AttachedFrame> savedLocations = new TreeMap<>();
    // Robot related QiSDK functions are in the helper.
    public RobotHelper robotHelper;
    public SaveFileHelper saveFileHelper;
    // Save some variables in the MainActivity to keep them after a onResume().
    public boolean goToRandomRunning = false;
    public boolean goToRouteRunning = false;
    public boolean goToRandomWasActivated = false;
    public boolean goToRouteWasActivated = false;
    public boolean goToMaxSpeed = false;
    public boolean goToStraight = false;
    public List<String> locationsToGoTo = null;
    public AtomicBoolean robotIsLocalized = new AtomicBoolean(false);
    private final AtomicBoolean load_location_success = new AtomicBoolean(false);
    public boolean withExistingMap = false;
    private Future<Void> goToRandomFuture;
    private FragmentManager fragmentManager;
    private String nextLocation;
    private int batteryLevel;
    private BroadcastReceiver batteryLevelReceiver;
    private GoToFrameFragment goToFrameFragment;
    private String currentFragment;
    public QiContext qiContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        this.fragmentManager = getSupportFragmentManager();
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.OVERLAY);
        setContentView(R.layout.activity_main);

        robotHelper = new RobotHelper();
        saveFileHelper = new SaveFileHelper();

        if (ContextCompat.checkSelfPermission(this, RECHARGE_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
            QiSDK.register(this, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, RECHARGE_PERMISSION}, MULTIPLE_PERMISSIONS);
        }
        Log.d(TAG, "onCreate: finished");
    }

    /**
     * As WRITE_EXTERNAL_STORAGE permission is mandatory to save a map,
     * the application will be closed if the user denies this permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissionsList, int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsResults = "";
                    int i = 0;
                    for (String per : permissionsList) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            permissionsResults += "\n" + per + " : PERMISSION_DENIED";
                        } else permissionsResults += "\n" + per + " : PERMISSION_GRANTED";
                        i++;
                    }
                    Log.d(TAG, "onRequestPermissionsResult : " + permissionsResults);
                }
                if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    QiSDK.register(this, this);
                } else finishAndRemoveTask();
            }
        }
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.d(TAG, "onRobotFocusedGained");
        this.qiContext = qiContext;
        robotHelper.onRobotFocusGained(qiContext);

        AutonomousRecharge.registerReceiver(qiContext);
        AutonomousRecharge.addOnDockingSoonListener(new DockingSoonListener());

        if (goToRandomWasActivated || goToRouteWasActivated) {
            if (savedLocations.containsKey("ChargingStation")) {
                if (!robotHelper.goToHelper.isDocked(savedLocations.get("ChargingStation")).getValue()) {
                    resumeGoTo();
                } else runOnUiThread(() -> setFragment(new MainFragment(), false));
            } else resumeGoTo();
        } else {
            runOnUiThread(() -> setFragment(new MainFragment(), false));
        }
    }

    @Override
    public void onRobotFocusLost() {
        Log.d(TAG, "onRobotFocusLost");

        if (goToRandomRunning && !goToRandomWasActivated) {
            goToRandomWasActivated = true;
            goToRandomLocation(false);
        }

        if (goToRouteRunning) {
            goToRouteWasActivated = true;
        }
        robotHelper.onRobotFocusLost();

        AutonomousRecharge.unregisterReceiver();
        AutonomousRecharge.removeAllOnDockingSoonListeners();
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.d(TAG, "onRobotFocusRefused : " + reason);
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        robotIsLocalized.set(false);
        getBaseContext().unregisterReceiver(batteryLevelReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: started");
        super.onResume();
        this.setFragment(new LoadingFragment(), false);
        checkBatteryLevel();
        Log.d(TAG, "onResume: finished");
    }

    /**
     * Change the fragment displayed by the placeholder in the main activity, and goes to the
     * bookmark init in the topic assigned to this fragment.
     *
     * @param fragment the fragment to display
     */

    public void setFragment(Fragment fragment, Boolean back) {
        Log.d(TAG, "Transaction for fragment : " + fragment.getClass().getSimpleName());
        currentFragment = fragment.getClass().getSimpleName();
        if (!(fragment instanceof LoadingFragment) && !(fragment instanceof SplashFragment)) {
            //Do some stuff here when arriving on a new Fragment except LoadingFragment and SplashFragment.
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (back) {
            transaction.setCustomAnimations(R.anim.enter_fade_in_left, R.anim.exit_fade_out_right, R.anim.enter_fade_in_right, R.anim.exit_fade_out_left);
        } else
            transaction.setCustomAnimations(R.anim.enter_fade_in_right, R.anim.exit_fade_out_left, R.anim.enter_fade_in_left, R.anim.exit_fade_out_right);

        transaction.replace(R.id.placeholder, fragment, "currentFragment");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public Integer getThemeId() {
        try {
            return getPackageManager().getActivityInfo(getComponentName(), 0).getThemeResource();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Fragment getFragment() {
        return fragmentManager.findFragmentByTag("currentFragment");
    }

    /**
     * If goToRandomWasActivated or goToRouteWasActivated when the app gains the robotFocus,
     * it will restart the localize to allow Pepper to go back to its task.
     */
    private void resumeGoTo() {
        if (!robotIsLocalized.get()) {
            if (!currentFragment.equalsIgnoreCase("LocalizeRobotFragment")) {
                setFragment(new LocalizeRobotFragment(), false);
            }
        }
    }

    /**
     * Add a DockingSoonListener to be notified by the application Autonomous Recharge when there
     * are 5 minutes OR 3% of battery left, before docking starts by itself.
     */
    private class DockingSoonListener implements AutonomousRechargeListeners.OnDockingSoonListener {
        @Override
        public void onDockingSoon(@NotNull QiContext qiContext) {
            Log.d(TAG, "onDockingSoon: Signal received");
            if (robotIsLocalized.get()) {
                Log.d(TAG, "onDockingSoon: robotIsLocalized, going to the charging station");

                if (!currentFragment.equalsIgnoreCase("GoToFrameFragment")) {
                    setFragment(new GoToFrameFragment(), false);
                }
                GoToFrameFragment goToFrameFragment = (GoToFrameFragment) getFragment();

                if (goToRouteRunning && !locationsToGoTo.contains("ChargingStation")) {
                    locationsToGoTo.add("ChargingStation");
                    Log.d(TAG, "onDockingSoon: GoToRouteRunning");
                } else {
                    if (goToRandomRunning) {
                        goToRandomLocation(false);
                        goToRandomWasActivated = true;
                        Log.d(TAG, "onDockingSoon: goToRandomRunning");
                    }
                    robotHelper.goToHelper.checkAndCancelCurrentGoto().thenConsume(aVoid -> {
                        goToFrameFragment.goToLocation("ChargingStation", OrientationPolicy.ALIGN_X);
                    });
                }
            } else {
                robotHelper.say(getString(R.string.need_to_go_to_charge));
                Log.d(TAG, "onDockingSoon: Robot is not localized, cannot go to charging station");
            }
        }
    }

    /**
     * Start Autonomous Recharge Application to dock on Charging Station.
     * Requires Autonomous Recharge Application to be installed first.
     *
     * @param useNaoqiSavedChargingStationFrame If Pepper has been on its Charging Station since its boot, a frame called ChargingStationFrame
     *                                          is saved in NAOqi's memory. The position of this frame relatively to the RobotFrame is relying
     *                                          on odometry ONLY. This means: the more the robot is navigating, the less the position of the
     *                                          ChargingStationFrame is accurate.
     *                                          true : to use the ChargingStationFrame
     *                                          false : to make Pepper scan its environment (with its mouth camera to find the Charging Station
     *                                          as soon as Autonomous Recharge application is launched.
     */
    public void dockOnChargingStation(boolean useNaoqiSavedChargingStationFrame) {
        if (ContextCompat.checkSelfPermission(this, RECHARGE_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Start docking");
            AutonomousRecharge.startDockingActivity(this, useNaoqiSavedChargingStationFrame);
        } else ActivityCompat.requestPermissions(this, new String[]{RECHARGE_PERMISSION}, 1);
    }

    /**
     * Start Autonomous Recharge Application to undock from Charging Station.
     * Requires Autonomous Recharge Application to be installed first.
     */
    public void undockFromChargingStation() {
        if (ContextCompat.checkSelfPermission(this, RECHARGE_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Start undocking");
            AutonomousRecharge.startUndockingActivity(this);
        } else ActivityCompat.requestPermissions(this, new String[]{RECHARGE_PERMISSION}, 1);
    }

    /**
     * Send the robot to the desired position.
     *
     * @param location the name of the location
     */
    public void goToLocation(final String location, OrientationPolicy orientationPolicy) {
        Log.d(TAG, "goToLocation: " + location);
        robotHelper.say("Let's go to " + location + "!!");
        robotHelper.goToHelper.checkAndCancelCurrentGoto().thenConsume(aVoid -> {
            robotHelper.holdAbilities(true);

            if (location.equalsIgnoreCase("mapFrame")) {
                robotHelper.goToHelper.goToMapFrame(goToStraight, goToMaxSpeed, orientationPolicy);
            } else if (location.equalsIgnoreCase("ChargingStation")) {
                robotHelper.goToHelper.goToChargingStation(savedLocations.get(location), goToStraight, goToMaxSpeed, orientationPolicy);
            } else {
                // Get the FreeFrame from the saved locations.
                robotHelper.goToHelper.goTo(savedLocations.get(location), goToStraight, goToMaxSpeed, orientationPolicy);
            }
        });
    }

    /**
     * GoTo a random location automatically every 2 minutes.
     *
     * @param setGoToRandom "true" to activate, "false" to deactivate
     */
    public void goToRandomLocation(Boolean setGoToRandom) {
        Log.d(TAG, "goToRandomLocation: goToRandom : " + setGoToRandom);
        goToRandomRunning = setGoToRandom;
        if (goToRandomRunning) {
            robotHelper.goToHelper.addOnFinishedMovingListener((goToStatus) -> {
                if (goToRandomRunning) {
                    goToRandomFuture = FutureUtils.wait(15, TimeUnit.SECONDS)
                            .andThenConsume(aUselessFuture -> {
                                goToRandomLocation(goToRandomRunning);
                            });
                }
            });
            nextLocation = pickRandomLocation();
            goToFrameFragment = (GoToFrameFragment) getFragment();
            goToFrameFragment.goToLocation(nextLocation, OrientationPolicy.ALIGN_X);
        } else {
            try {
                goToRandomFuture.thenConsume(voidFuture -> Log.d(TAG, "goToRandomFuture: cancelled"));
                goToRandomFuture.cancel(true);
            } catch (Exception e) {
                Log.d(TAG, "goToRandomLocation: error: " + e.toString());
            }
        }
    }

    /**
     * Get a random location from the saved locations list.
     *
     * @return the name of a location
     */
    private String pickRandomLocation() {
        List<String> keysAsArray = new ArrayList<>(savedLocations.keySet());
        keysAsArray.add("mapFrame");
        Random r = new Random();
        String location = keysAsArray.get(r.nextInt(keysAsArray.size()));
        if (!location.equals(nextLocation) && !location.equals("ChargingStation")) {
            return location;
        } else return pickRandomLocation();
    }

    /**
     * Save the current location of Pepper.
     *
     * @param location the name of the location
     * @return Future<Void> "success" if the location is saved successfully
     */
    public Future<Void> saveLocation(final String location) {
        // Get the robot frame asynchronously.
        Log.d(TAG, "saveLocation: Start saving this location");
        return robotHelper.createAttachedFrameFromCurrentPosition()
                .andThenConsume(attachedFrame -> savedLocations.put(location, attachedFrame));
    }

    /**
     * Load Locations from file.
     *
     * @return "true" if success, "false" otherwise.
     */
    public Future<Boolean> loadLocations() {
        return FutureUtils.futureOf((f) -> {
            // Read file into a temporary hashmap.
            File file = new File(filesDirectoryPath, locationsFileName);
            if (file.exists()) {
                Map<String, Vector2theta> vectors = saveFileHelper.getLocationsFromFile(filesDirectoryPath, locationsFileName);

                // Clear current savedLocations.
                savedLocations = new TreeMap<>();
                Frame mapFrame = robotHelper.getMapFrame();

                // Build frames from the vectors.
                for (Map.Entry<String, Vector2theta> entry : vectors.entrySet()) {
                    // Create a transform from the vector2theta.
                    Transform t = entry.getValue().createTransform();
                    Log.d(TAG, "loadLocations: " + entry.getKey());

                    // Create an AttachedFrame representing the current robot frame relatively to the MapFrame.
                    AttachedFrame attachedFrame = mapFrame.async().makeAttachedFrame(t).getValue();

                    // Store the FreeFrame.
                    savedLocations.put(entry.getKey(), attachedFrame);
                    load_location_success.set(true);
                }

                Log.d(TAG, "loadLocations: Done");
                if (load_location_success.get()) return Future.of(true);
                else throw new Exception("Empty file");
            } else {
                throw new Exception("No file");
            }
        });
    }

    /**
     * Save Locations to file.
     */
    public void backupLocations() {
        Log.d(TAG, "backupLocations: Started");
        TreeMap<String, Vector2theta> locationsToBackup = new TreeMap<>();
        Frame mapFrame = robotHelper.getMapFrame();

        for (Map.Entry<String, AttachedFrame> entry : savedLocations.entrySet()) {
            // get location of the frame.
            AttachedFrame destination = entry.getValue();
            Frame frame = destination.async().frame().getValue();

            // create a serializable vector2theta.
            Vector2theta vector = Vector2theta.betweenFrames(mapFrame, frame);

            // add to backup list.
            locationsToBackup.put(entry.getKey(), vector);
        }

        saveFileHelper.saveLocationsToFile(filesDirectoryPath, locationsFileName, locationsToBackup);
        runOnUiThread(() -> {
            SaveLocationsFragment saveLocationsFragment = (SaveLocationsFragment) getFragment();
            saveLocationsFragment.progressBar.setVisibility(View.GONE);
            saveLocationsFragment.locationsSaved.setVisibility(View.VISIBLE);
            saveLocationsFragment.saving_text.setText("Saved");
            saveLocationsFragment.locationsListModified = false;
            FutureUtils.wait(3, TimeUnit.SECONDS)
                    .thenConsume(aUselessFutureB -> runOnUiThread(() -> {
                        if (saveLocationsFragment.isVisible()) {
                            saveLocationsFragment.savingLocationsPopup.dialog.hide();
                        }
                    }));
        });
    }

    /**
     * Launch the function localizeAndMap that allows the user to build a map on Pepper.
     */
    public void startMapping(boolean withExistingMap) {
        // 20s for the user to leave and the remaining obstacles to be forgotten by the memory.
        Log.d(TAG, "startLocalizeAndMap");
        this.withExistingMap = withExistingMap;
        LocalizeAndMapFragment localizeAndMapFragment = (LocalizeAndMapFragment) getFragment();
        robotHelper.holdAbilities(true).andThenConsume(aVoid -> robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid1 -> {
            // Put back 20 seconds wait (line below) before starting the map in order to free the temporary obstacles in memory
            robotHelper.say(getString(R.string.stay_away_mapping)).andThenConsume(aVoid2 -> FutureUtils.wait(2, TimeUnit.SECONDS)
                    .thenConsume(aUselessFuture -> {
                        if (withExistingMap) {
                            robotHelper.localizeAndMapHelper.addOnFinishedLocalizingListener(result -> {
                                if (result == LocalizeAndMapHelper.LocalizationStatus.LOCALIZED) {
                                    robotHelper.localizeAndMapHelper.animationToLookInFront();
                                    Log.d(TAG, "startMapping: withExistingMap, localized, animationToLookInFront()");
                                }
                            });
                        }

                        robotHelper.localizeAndMapHelper.addOnFinishedMappingListener(success -> {
                            robotHelper.releaseAbilities();
                            if (success) {
                                Log.d(TAG, "stopMappingAndBackupMap: start saving map!");
                                runOnUiThread(() -> localizeAndMapFragment.saving_text.setText(R.string.saving));
                                StreamableBuffer mapData = robotHelper.localizeAndMapHelper.getStreamableMap();
                                saveFileHelper.writeStreamableBufferToFile(filesDirectoryPath, mapFileName, mapData);

                                runOnUiThread(() -> {
                                    localizeAndMapFragment.progressBar.setVisibility(View.GONE);
                                    localizeAndMapFragment.mapSaved.setVisibility(View.VISIBLE);
                                    localizeAndMapFragment.saving_text.setText(R.string.saved);
                                });

                                FutureUtils.wait(3, TimeUnit.SECONDS)
                                        .thenConsume(aUselessFutureB -> {
                                            runOnUiThread(() -> {
                                                localizeAndMapFragment.mapSaved.setVisibility(View.GONE);
                                                localizeAndMapFragment.progressBar.setVisibility(View.VISIBLE);
                                                localizeAndMapFragment.saving_text.setText("Building ExplorationMap Image");
                                                robotHelper.localizeAndMapHelper.getExplorationMapBitmap().andThenConsume(explorationMapBitmap -> runOnUiThread(() -> {
                                                    Log.d(TAG, "startMapping: explorationMapBitmap built");
                                                    localizeAndMapFragment.displayMap.setImageBitmap(explorationMapBitmap);
                                                    localizeAndMapFragment.saving_text.setVisibility(View.GONE);
                                                    localizeAndMapFragment.progressBar.setVisibility(View.GONE);
                                                    localizeAndMapFragment.displayMap.setVisibility(View.VISIBLE);
                                                    localizeAndMapFragment.close_button.setVisibility(View.VISIBLE);
                                                }));
                                            });
                                        });

                                robotHelper.localizeAndMapHelper.removeOnFinishedMappingListeners();
                            } else {
                                Log.d(TAG, "startMapping: finished with error");
                                runOnUiThread(() -> {
                                    localizeAndMapFragment.stop_button.setVisibility(View.GONE);
                                    localizeAndMapFragment.icn_360_load.setVisibility(View.GONE);
                                    localizeAndMapFragment.retry.setVisibility(View.VISIBLE);
                                    localizeAndMapFragment.mapping_error.setVisibility(View.VISIBLE);
                                    if (localizeAndMapFragment.timer != null)
                                        localizeAndMapFragment.timer.cancel();
                                    if (localizeAndMapFragment.savingMapPopup != null) {
                                        if (localizeAndMapFragment.savingMapPopup.dialog.isShowing())
                                            localizeAndMapFragment.saving_text.setText("Saving error, please retry");
                                        FutureUtils.wait(5, TimeUnit.SECONDS)
                                                .thenConsume(aUselessFutureB -> runOnUiThread(() -> localizeAndMapFragment.savingMapPopup.dialog.hide()));
                                    }
                                });
                            }
                            robotHelper.localizeAndMapHelper.removeOnFinishedLocalizingListeners();
                        });
                        robotHelper.localizeAndMapHelper.localizeAndMap(withExistingMap);
                        runOnUiThread(() -> localizeAndMapFragment.onIntialMappingFinished());
                    }));
        }));
    }

    /**
     * Stop the mapping phase and save the map in file.
     */
    public void stopMappingAndBackupMap() {
        robotHelper.localizeAndMapHelper.stopCurrentAction().thenConsume(f -> Log.d(TAG, "stopMappingAndBackupMap: stopped"));
    }

    /**
     * Start the Localize function that allows Pepper to localize itself in a map
     * that has been already saved.
     */
    public void startLocalizing() {
        robotHelper.say(getString(R.string.stay_away_localizing));
        if (robotHelper.localizeAndMapHelper.getStreamableMap() == null) {
            StreamableBuffer mapData = saveFileHelper.readStreamableBufferFromFile(filesDirectoryPath, mapFileName);
            if (mapData == null) {
                Log.d(TAG, "startLocalizing: No Map Available");
                robotHelper.localizeAndMapHelper.raiseFinishedLocalizing(LocalizeAndMapHelper.LocalizationStatus.MAP_MISSING);
            } else {
                Log.d(TAG, "startLocalizing: get and set map");

                robotHelper.localizeAndMapHelper.setStreamableMap(mapData);

                robotHelper.holdAbilities(true).andThenConsume((useless) ->
                        robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid ->
                                robotHelper.localizeAndMapHelper.localize()));
            }
        } else {
            robotHelper.holdAbilities(true).andThenConsume((useless) ->
                    robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid ->
                            robotHelper.localizeAndMapHelper.localize()));
        }
    }

    /**
     * Cancel a Localize action.
     */
    public void stopLocalizing() {
        robotHelper.localizeAndMapHelper.stopCurrentAction();
    }

    /**
     * Display regularly the current battery level in logs.
     */
    public void checkBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctxt, Intent intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                Log.d(TAG, "checkBatteryLevel: Battery : " + batteryLevel);
            }
        };
        getBaseContext().registerReceiver(batteryLevelReceiver, ifilter);
    }
}

