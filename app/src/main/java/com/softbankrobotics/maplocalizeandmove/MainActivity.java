package com.softbankrobotics.maplocalizeandmove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.locale.Language;
import com.aldebaran.qi.sdk.object.locale.Locale;
import com.aldebaran.qi.sdk.object.locale.Region;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.softbankrobotics.maplocalizeandmove.Fragments.GoToFrameFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.LoadingFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.LocalizeAndMapFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.MainFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SaveLocationsFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SetupFragment;
import com.softbankrobotics.maplocalizeandmove.Fragments.SplashFragment;
import com.softbankrobotics.maplocalizeandmove.Utils.LocalizeAndMapHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.RobotHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.SaveFileHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.Vector2theta;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks, Serializable {

    private static final String TAG = "MSI_MapLocalizeAndMove";
    // Store the saved locations.
    public Map<String, AttachedFrame> savedLocations = new HashMap<>();
    // Robot related QiSDK functions are in the helper
    public RobotHelper robotHelper;
    public AtomicBoolean robotIsLocalized = new AtomicBoolean(false);
    public boolean goToRandom = false;
    public boolean goToMaxSpeed = false;
    public boolean goToStraight = false;
    private AtomicBoolean load_location_success = new AtomicBoolean(false);
    private SaveFileHelper saveFileHelper;
    private FragmentManager fragmentManager;
    private QiContext qiContext;
    private String nextLocation;
    private int level;
    private String currentFragment;
    private BroadcastReceiver batteryLevelReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        this.fragmentManager = getSupportFragmentManager();
        QiSDK.register(this, this);
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.OVERLAY);
        setContentView(R.layout.activity_main);

        robotHelper = new RobotHelper();
        saveFileHelper = new SaveFileHelper();

        Log.d(TAG, "onCreate: finished");
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.d(TAG, "onRobotFocusedGained");
        this.qiContext = qiContext;
        robotHelper.onRobotFocusGained(qiContext);

        runOnUiThread(() -> setFragment(new MainFragment(), false));
    }

    @Override
    public void onRobotFocusLost() {
        Log.d(TAG, "onRobotFocusLost: ");
        this.qiContext = null;
        if (robotHelper.holder != null) {
            robotHelper.releaseAbilities();
        }
        robotHelper.onRobotFocusLost();
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.d(TAG, "onRobotFocusRefused");
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
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
     * Updates the value of the qiVariable
     * @param variableName the name of the variable
     * @param value the value that needs to be set
     */

    /**
     * Change the fragment displayed by the placeholder in the main activity, and goes to the
     * bookmark init in the topic assigned to this fragment
     *
     * @param fragment the fragment to display
     */

    public void setFragment(Fragment fragment, Boolean back) {
        currentFragment = fragment.getClass().getSimpleName();
        if (!(fragment instanceof LoadingFragment) && !(fragment instanceof SplashFragment)) {
            //Do some stuff here when arriving on a new Fragment except LoadingFragment and SplashFragment
        }
        Log.d(TAG, "Transaction for fragment : " + fragment.getClass().getSimpleName());
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

    public android.support.v4.app.Fragment getFragment() {
        return fragmentManager.findFragmentByTag("currentFragment");
    }

    public void goToLocation(final String location) {
        if (!currentFragment.equalsIgnoreCase("GoToFrameFragment")) {
            setFragment(new GoToFrameFragment(), false);
        }
        Log.d(TAG, "goToLocation: " + location);
        if (!askToCloseIfFlapIsOpened()) {
            say("Let's go to " + location + "!!");
            robotHelper.holdAbilities(false);

            GoToFrameFragment goToFrameFragment = (GoToFrameFragment) getFragment();
            goToFrameFragment.createGoToPopup();
            robotHelper.goToHelper.addOnStartedMovingListener(() -> runOnUiThread(() -> {
                goToFrameFragment.goToPopup.dialog.show();
                goToFrameFragment.goToPopup.dialog.getWindow().setAttributes(goToFrameFragment.goToPopup.lp);
            }));
            robotHelper.goToHelper.addOnFinishedMovingListener((success) -> {
                runOnUiThread(() -> {
                    robotHelper.releaseAbilities();
                    goToFrameFragment.goto_loader.setVisibility(View.GONE);
                    if (success) {
                        goToFrameFragment.check.setVisibility(View.VISIBLE);
                        goToFrameFragment.goto_text.setVisibility(View.VISIBLE);
                    } else {
                        goToFrameFragment.cross.setVisibility(View.VISIBLE);
                    }
                });
                Future<Void> futureUtils = FutureUtils.wait((long) 6, TimeUnit.SECONDS)
                        .thenConsume(aUselessFuture -> runOnUiThread(() -> {
                            if (goToFrameFragment.goToPopup.dialog.isShowing())
                                goToFrameFragment.goToPopup.dialog.hide();
                        }));

                goToFrameFragment.goToPopup.dialog.setOnDismissListener(arg0 -> futureUtils.cancel(true));

                robotHelper.goToHelper.removeOnStartedMovingListeners();
                robotHelper.goToHelper.removeOnFinishedMovingListeners();
            });
            if (location.equalsIgnoreCase("mapFrame")) {
                robotHelper.goToHelper.goToMapFrame(goToStraight, goToMaxSpeed);
            } else {
                // Get the FreeFrame from the saved locations.
                robotHelper.goToHelper.goTo(savedLocations.get(location), goToStraight, goToMaxSpeed);
            }
        }
    }


    public boolean askToCloseIfFlapIsOpened() {
        boolean isFlapopened = robotHelper.getFlapState();
        if (isFlapopened)
            say("Please close my hatch then start the action you want");
        return isFlapopened;
    }

    /**
     * GoTo a random location automatically every 2 minutes
     *
     * @param setGoToRandom "true" to activate, "false" to deactivate
     */
    public void goToRandomLocation(Boolean setGoToRandom) {
        Log.d(TAG, "goToRandomLocation: goToRandom : " + setGoToRandom);
        goToRandom = setGoToRandom;
        if (goToRandom) {
            robotHelper.goToHelper.addOnFinishedMovingListener((success) -> {
                if (goToRandom) {
                    FutureUtils.wait((long) 2, TimeUnit.MINUTES)
                            .thenConsume(aUselessFuture -> {
                                if (goToRandom) {
                                    Looper.prepare();
                                    goToRandomLocation(goToRandom);
                                }
                            });
                }
            });
            nextLocation = pickRandomLocation();
            goToLocation(pickRandomLocation());
        }
    }

    /**
     * Get a random location from the saved locations list
     *
     * @return the name of a location
     */
    private String pickRandomLocation() {
        List<String> keysAsArray = new ArrayList<>(savedLocations.keySet());
        keysAsArray.add("mapFrame");
        Random r = new Random();
        String location = keysAsArray.get(r.nextInt(keysAsArray.size()));
        if (!location.equals(nextLocation)) {
            return location;
        } else return pickRandomLocation();
    }

    /**
     * Save the current location of Pepper
     *
     * @param location the name of the location
     * @return
     */
    public Future<Void> saveLocation(final String location) {
        // Get the robot frame asynchronously.
        Log.d(TAG, "saveLocation: Saving Location");
        return robotHelper.createAttachedFrameFromCurrentPosition()
                .andThenConsume(attachedFrame -> savedLocations.put(location, attachedFrame));
    }

    /**
     * Load Locations from file
     *
     * @return "true" if success, "false" otherwise.
     */
    public Future<Boolean> loadLocations() {
        return FutureUtils.futureOf((f) -> {
            // Read file into a temporary hashmap
            File file = new File(getFilesDir(), "hashmap.ser");
            if (file.exists()) {

                Map<String, Vector2theta> vectors = saveFileHelper.getLocationsFromFile(getApplicationContext());

                // Clear current savedLocations
                savedLocations = new HashMap<>();
                Frame mapFrame = robotHelper.getMapFrame();

                // Build frames from the vectors
                for (Map.Entry<String, Vector2theta> entry : vectors.entrySet()) {
                    // Create a transform from the vector2theta
                    Transform t = entry.getValue().createTransform();
                    Log.d(TAG, "loadLocations: " + entry.getKey());

                    // Create an AttachedFrame representing the current robot frame relatively to the MapFrame
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
     * Save Locations to file
     */
    public void backupLocations() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            Map<String, Vector2theta> locationsToBackup = new HashMap<>();
            Frame mapFrame = robotHelper.getMapFrame();

            for (Map.Entry<String, AttachedFrame> entry : savedLocations.entrySet()) {
                // get location of the frame
                AttachedFrame destination = entry.getValue();
                Frame frame = destination.async().frame().getValue();

                // create a serializable vector2theta
                Vector2theta vector = Vector2theta.betweenFrames(mapFrame, frame);

                // add to backup list
                locationsToBackup.put(entry.getKey(), vector);
            }

            saveFileHelper.saveLocationsToFile(getApplicationContext(), locationsToBackup);
            runOnUiThread(() -> {
                SaveLocationsFragment saveLocationsFragment = (SaveLocationsFragment) getFragment();
                saveLocationsFragment.progressBar.setVisibility(View.GONE);
                saveLocationsFragment.locationsSaved.setVisibility(View.VISIBLE);
                saveLocationsFragment.saving_text.setText("Saved");
                saveLocationsFragment.locationsListModified = false;
                FutureUtils.wait((long) 5, TimeUnit.SECONDS)
                        .thenConsume(aUselessFutureB -> runOnUiThread(() -> {
                            if (saveLocationsFragment.isVisible()) {
                                saveLocationsFragment.savingLocationsPopup.dialog.hide();
                            }
                        }));
            });
        });
    }

    /**
     * Launch the function localizeAndMap that allow the user to teach a map to Pepper.
     */
    public void startMapping() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // 20s for the user to leave and the remaining obstacles to be forgotten by the memory
            Log.d(TAG, "startLocalizeAndMap");
            LocalizeAndMapFragment localizeAndMapFragment = (LocalizeAndMapFragment) getFragment();
            robotHelper.holdAbilities(true).andThenConsume(aVoid -> robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid1 -> {
                //Todo Put back 20 seconds sleep before starting the map in order to free the temporary obstacles
                say(getString(R.string.stay_away_mapping)).andThenConsume(aVoid2 -> FutureUtils.wait((long) 5, TimeUnit.SECONDS)
                        .thenConsume(aUselessFuture -> {
                            robotHelper.localizeAndMapHelper.addOnFinishedMappingListener(success -> {
                                robotHelper.releaseAbilities();
                                if (success) {
                                    Log.d(TAG, "stopMappingAndBackupMap: start saving map!");
                                    String mapData = robotHelper.localizeAndMapHelper.getMap();
                                    saveFileHelper.writeStringToFile(getApplicationContext(), mapData, "mapData.txt");

                                    runOnUiThread(() -> {
                                        localizeAndMapFragment.progressBar.setVisibility(View.GONE);
                                        localizeAndMapFragment.mapSaved.setVisibility(View.VISIBLE);
                                        localizeAndMapFragment.saving_text.setText("Saved");
                                    });
                                    FutureUtils.wait((long) 5, TimeUnit.SECONDS)
                                            .thenConsume(aUselessFutureB -> runOnUiThread(() -> {
                                                if (localizeAndMapFragment.isVisible()) {
                                                    localizeAndMapFragment.savingMapPopup.dialog.hide();
                                                    setFragment(new SetupFragment(), true);
                                                }
                                            }));
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
                                            FutureUtils.wait((long) 5, TimeUnit.SECONDS)
                                                    .thenConsume(aUselessFutureB -> runOnUiThread(() -> localizeAndMapFragment.savingMapPopup.dialog.hide()));
                                        }
                                    });
                                }
                            });
                            robotHelper.localizeAndMapHelper.localizeAndMap();
                            runOnUiThread(() -> localizeAndMapFragment.onIntialMappingFinished());
                        }));
            }));
        });
    }

    /**
     * Stop the mapping phase and save the map in file
     */
    public void stopMappingAndBackupMap() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> robotHelper.localizeAndMapHelper.stopCurrentAction().thenConsume(f -> Log.d(TAG, "stopMappingAndBackupMap: stopped")));
    }

    /**
     * Start the Localize function, that allow Pepper to localize itself in a map that have been already saved
     */
    public void startLocalizing() {
        say(getString(R.string.stay_away_localizing));
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (robotHelper.localizeAndMapHelper.getMap().isEmpty()) {
                String map = saveFileHelper.readStringFromFile(getApplicationContext(), "mapData.txt");
                if (map.equalsIgnoreCase("File not found") || map.equalsIgnoreCase("Can not read file")) {
                    Log.d(TAG, "startLocalizing: No Map Available");
                    robotHelper.localizeAndMapHelper.raiseFinishedLocalizing(LocalizeAndMapHelper.LocalizationStatus.MAP_MISSING);
                } else {
                    Log.d(TAG, "startLocalizing: get and set map");
                    robotHelper.localizeAndMapHelper.setMap(map);

                    robotHelper.holdAbilities(true).andThenConsume((useless) ->
                            robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid ->
                            robotHelper.localizeAndMapHelper.localize()));
                }
            } else {
                robotHelper.holdAbilities(true).andThenConsume((useless) ->
                        robotHelper.localizeAndMapHelper.animationToLookInFront().andThenConsume(aVoid ->
                        robotHelper.localizeAndMapHelper.localize()));
            }
        });
    }

    /**
     * Cancel a Localize action
     */
    public void stopLocalizing() {
        robotHelper.localizeAndMapHelper.stopCurrentAction();
    }

    /**
     * Make Pepper say a sentence
     *
     * @param text to be said by Pepper
     * @return
     */
    public Future<Void> say(final String text) {
        Future<Say> sayFuture = SayBuilder.with(qiContext)
                .withText(text)
                .withLocale(new Locale(Language.ENGLISH, Region.UNITED_STATES))
                .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                .buildAsync();

        return sayFuture.andThenCompose(say -> {
            //Log.d(TAG, "Say started");
            return say.async().run();
        });
    }

    /**
     * Display regularly the current battery level in logs
     */
    public void checkBatteryLevel() {
        Log.d(TAG, "checkBatteryLevel: started");

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctxt, Intent intent) {
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                Log.d(TAG, "checkBatteryLevel: Battery : " + level);
            }
        };
        getBaseContext().registerReceiver(batteryLevelReceiver, ifilter);
        Log.d(TAG, "checkBatteryLevel: finished");
    }
}

