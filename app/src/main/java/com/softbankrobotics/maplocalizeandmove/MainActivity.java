package com.softbankrobotics.maplocalizeandmove;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.util.FutureUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks, Serializable {

    private static final String TAG = "MapLocalizeAndMove";

    private Button saveAttachedFrameButton;
    private Button startLocalizeAndMapButton;
    private Button stopLocalizeAndMapButton;
    private Button startLocalizeButton;
    private Button stopLocalizeButton;
    private ArrayAdapter<String> spinnerAdapter;

    // Store the selected location.
    private String selectedLocation;

    // Store the saved locations.
    private Map<String, AttachedFrame> savedLocations = new HashMap<>();

    // Robot related QiSDK functions are in the helper
    private RobotHelper robotHelper;
    private SaveFileHelper saveFileHelper;

    private AtomicBoolean robotIsLocalized = new AtomicBoolean(false);
    private ProgressBar progressBar;
    private ImageView checkbox_localize;
    private ImageView checkbox_locations_loaded;
    private ImageView checkbox_localize_and_map;
    private ImageView checkbox_backup;
    private ProgressBar progressbar_localize_and_map;
    //----------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSelectionPage();
        QiSDK.register(this, this);

        robotHelper = new RobotHelper();
        saveFileHelper = new SaveFileHelper();
    }

    /*
     * 3 pages (selection menu, then setup or production pages)
     */

    void loadSelectionPage() {
        setContentView(R.layout.activity_main);

        // Home page buttons
        Button setupButton = findViewById(R.id.setup_button);
        setupButton.setOnClickListener(view -> loadSetupPage());

        Button prodButton = findViewById(R.id.prod_button);
        prodButton.setOnClickListener(v -> loadProductionPage());

    }

    void loadProductionPage() {
        setContentView(R.layout.production_mode);

        Button setupButton = findViewById(R.id.setup_button);
        setupButton.setOnClickListener(view -> loadSetupPage());

        checkbox_localize = findViewById(R.id.checkbox_localize);
        checkbox_locations_loaded = findViewById(R.id.checkbox_locations_loaded);
        progressBar = findViewById(R.id.progressbar);
        ImageView move_result = findViewById(R.id.move_result);
        localizeButton();

        // Go to location on go to button clicked.
        //Setup page buttons
        Button goToButton = findViewById(R.id.goto_button);
        goToButton.setOnClickListener(v -> {
            if (selectedLocation != null) {
                goToLocation(selectedLocation);
            }
        });

        initializeSpinner();

        loadLocationsButton();

        addLocaliseListeners();

        robotHelper.goToHelper.addOnStartedMovingListener(() -> {
            // start loading bar when robot starts moving
            robotHelper.holdAbilities();
            runOnUiThread(() -> {
                move_result.setBackgroundResource(0);
                progressBar.setVisibility(View.VISIBLE);
            });
        });

        robotHelper.goToHelper.addOnFinishedMovingListener(success -> {
            // stop loading bar when robot stops moving
            robotHelper.releaseAbilities();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    move_result.setBackgroundResource(R.drawable.checked);
                } else {
                    move_result.setBackgroundResource(R.drawable.cross);
                }
                FutureUtils.wait((long) 10, TimeUnit.SECONDS)
                        .thenConsume(aUselessFuture -> {
                            runOnUiThread(() -> move_result.setBackgroundResource(0));
                        });
            });
        });
    }

    void loadSetupPage() {
        setContentView(R.layout.setup_mode);

        Button prodButton = findViewById(R.id.prod_button);
        prodButton.setOnClickListener(v -> loadProductionPage());

        initializeSpinner();
        final EditText editText = findViewById(R.id.editText);
        progressBar = findViewById(R.id.progressbar);
        progressbar_localize_and_map = findViewById(R.id.progressbar_localize_and_map);
        checkbox_localize = findViewById(R.id.checkbox_localize);
        checkbox_locations_loaded = findViewById(R.id.checkbox_locations_loaded);
        checkbox_localize_and_map = findViewById(R.id.checkbox_localize_and_map);
        checkbox_backup = findViewById(R.id.checkbox_backup);


        // Start startMapping Action on Button clicked
        startLocalizeAndMapButton = findViewById(R.id.startlocalizeandmap_button);
        startLocalizeAndMapButton.setOnClickListener(view1 -> {
            progressBar.setVisibility(View.VISIBLE);
            startLocalizeAndMapButton.setEnabled(false);
            startMapping();
        });


        // Stop startMapping Action on Button clicked
        stopLocalizeAndMapButton = findViewById(R.id.stoplocalizeandmap_button);
        stopLocalizeAndMapButton.setOnClickListener(v -> {
            stopMappingAndBackupMap();
            stopLocalizeAndMapButton.setEnabled(false);
        });


        localizeButton();

        // Save location on save button clicked.
        saveAttachedFrameButton = findViewById(R.id.save_button);
        saveAttachedFrameButton.setOnClickListener(v -> {
            String location = editText.getText().toString();
            editText.setText("");
            // Save location only if new.
            if (!location.isEmpty() && !savedLocations.containsKey(location)) {
                runOnUiThread(() -> spinnerAdapter.add(location));
                saveLocation(location);
            }
        });


        Button saveLocationsButton = findViewById(R.id.save_locations_button);
        saveLocationsButton.setOnClickListener(v -> backupLocations());

        loadLocationsButton();


        robotHelper.localizeAndMapHelper.addOnStartedMappingListener(() -> runOnUiThread(() -> checkbox_localize_and_map.setBackgroundResource(R.drawable.unchecked)));

        addLocaliseListeners();
    }

    private void localizeButton() {
        startLocalizeButton = findViewById(R.id.startlocalizerobot_button);
        startLocalizeButton.setOnClickListener(v -> startLocalizing());

        stopLocalizeButton = findViewById(R.id.stoplocalizerobot_button);
        stopLocalizeButton.setOnClickListener(v -> stopLocalizing());

    }

    private void loadLocationsButton() {
        Button loadLocationsButton = findViewById(R.id.load_locations_button);
        loadLocationsButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Load Locations Button");
            loadLocations();
        });
    }

    private void addLocaliseListeners() {
        robotHelper.localizeAndMapHelper.addOnStartedLocalizingListener(() -> runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            startLocalizeButton.setVisibility(View.GONE);
            stopLocalizeButton.setVisibility(View.VISIBLE);
        }));

        robotHelper.localizeAndMapHelper.addOnFinishedLocalizingListener(success -> {
            robotIsLocalized.set(success);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (success) {
                    if (saveAttachedFrameButton != null) saveAttachedFrameButton.setEnabled(true);
                    checkbox_localize.setBackgroundResource(R.drawable.checked);
                } else {
                    startLocalizeButton.setVisibility(View.VISIBLE);
                    stopLocalizeButton.setVisibility(View.GONE);
                    checkbox_localize.setBackgroundResource(R.drawable.cross);
                }
            });
        });
    }

    private void initializeSpinner() {
        final Spinner spinner = findViewById(R.id.spinner);
        // Store location on selection.
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocation = (String) parent.getItemAtPosition(position);
                Log.d(TAG, "onItemSelected: " + selectedLocation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedLocation = null;
                Log.d(TAG, "onNothingSelected");
            }
        });

        // Setup spinner adapter.
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    /*
     * Points of Interest
     */

    private void goToLocation(final String location) {
        // Get the FreeFrame from the saved locations.
        if (location.equalsIgnoreCase("mapFrame")) {
            robotHelper.goToHelper.goToMapFrame();
        } else {
            robotHelper.goToHelper.goTo(savedLocations.get(location));
        }
    }

    private void saveLocation(final String location) {
        // Get the robot frame asynchronously.
        robotHelper.createAttachedFrameFromCurrentPosition()
                .andThenConsume(attachedFrame -> savedLocations.put(location, attachedFrame));
    }

    private void loadLocations() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            // Read file into a temporary hashmap
            File file = new File(getFilesDir(), "hashmap.ser");
            if (file.exists()) {
                displayMessage("Loading locations from memory... ");

                Map<String, Vector2> vectors = saveFileHelper.getLocationsFromFile(getApplicationContext());

                // Clear current savedLocations
                savedLocations = new HashMap<>();
                Frame mapFrame = robotHelper.getMapFrame();

                // Build frames from the vectors
                for (Map.Entry<String, Vector2> entry : vectors.entrySet()) {
                    // Create a transform from the vector2
                    Transform t = entry.getValue().createTransform();
                    Log.d(TAG, "loadLocations: " + entry.getKey());

                    // Create an AttachedFrame representing the current robot frame relatively to the MapFrame
                    AttachedFrame attachedFrame = mapFrame.async().makeAttachedFrame(t).getValue();

                    // Store the FreeFrame.
                    runOnUiThread(() -> spinnerAdapter.add(entry.getKey()));
                    savedLocations.put(entry.getKey(), attachedFrame);
                }
                Log.d(TAG, "loadLocations: Done");
                runOnUiThread(() -> {
                    checkbox_locations_loaded.setBackgroundResource(R.drawable.checked);
                    spinnerAdapter.add("mapFrame");
                });

            } else displayMessage("No locations in memory to load. ");
        });
    }

    private void backupLocations() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            Map<String, Vector2> locationsToBackup = new HashMap<>();
            Frame mapFrame = robotHelper.getMapFrame();

            for (Map.Entry<String, AttachedFrame> entry : savedLocations.entrySet()) {
                // get location of the frame
                AttachedFrame destination = entry.getValue();
                Frame frame = destination.async().frame().getValue();

                // create a serializable vector2
                Vector2 vector = Vector2.betweenFrames(mapFrame, frame);

                // add to backup list
                locationsToBackup.put(entry.getKey(), vector);
            }


            saveFileHelper.saveLocationsToFile(getApplicationContext(), locationsToBackup);
            runOnUiThread(() -> {
                checkbox_backup.setBackgroundResource(R.drawable.checked);
            });
        });
    }

    /*
     * the Map you teach the robot
     */

    private void startMapping() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            // 20s for the user to leave and the remaining obstacles to be forgotten
            Log.d(TAG, "startLocalizeAndMapButton");
            displayMessage("Stay away during initial mapping");
            robotHelper.holdAbilities();
            FutureUtils.wait((long) 20, TimeUnit.SECONDS)
                    .thenConsume(aUselessFuture -> {

                        robotHelper.localizeAndMapHelper.localizeAndMap();
                        runOnUiThread(() -> {
                            startLocalizeAndMapButton.setVisibility(View.GONE);
                            startLocalizeAndMapButton.setEnabled(true);
                            stopLocalizeAndMapButton.setVisibility(View.VISIBLE);
                            progressbar_localize_and_map.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        });
                    });
        });
    }

    private void stopMappingAndBackupMap() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            robotHelper.localizeAndMapHelper.addOnFinishedMappingListener(success -> {
                Log.d(TAG, "stopMappingAndBackupMap: start saving map!");
                String mapData = robotHelper.localizeAndMapHelper.getMap();
                saveFileHelper.writeStringToFile(getApplicationContext(), mapData, "mapData.txt");
                robotHelper.releaseAbilities();
                runOnUiThread(() -> {
                    startLocalizeAndMapButton.setVisibility(View.VISIBLE);
                    stopLocalizeAndMapButton.setVisibility(View.GONE);
                    stopLocalizeAndMapButton.setEnabled(true);
                    saveAttachedFrameButton.setEnabled(false);
                    progressbar_localize_and_map.setVisibility(View.GONE);
                    checkbox_localize_and_map.setBackgroundResource(R.drawable.checked);
                    checkbox_localize.setBackgroundResource(R.drawable.unchecked);
                });
            });

            robotHelper.localizeAndMapHelper.stopCurrentAction().thenConsume(f -> {
                //LocalizeAndMap finished.
            });
        });
    }

    private void startLocalizing() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (robotHelper.localizeAndMapHelper.getMap().isEmpty()) {
                String map = saveFileHelper.readStringFromFile(getApplicationContext(), "mapData.txt");
                robotHelper.localizeAndMapHelper.setMap(map);
                Log.d(TAG, "startLocalizing: get and set map");
            }
            robotHelper.holdAbilities();
            robotHelper.localizeAndMapHelper.localize();
        });
    }

    private void stopLocalizing() {
        robotHelper.localizeAndMapHelper.stopCurrentAction();
        runOnUiThread(() -> {
            if (saveAttachedFrameButton != null) saveAttachedFrameButton.setEnabled(false);
        });
        robotHelper.releaseAbilities();
    }

    public void displayMessage(String message) {
        Snackbar.make(findViewById(R.id.load_locations_button), message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }


    /*
     * Robots lifecycle callbacks
     */

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.d(TAG, "Got Robot Focus");
        robotHelper.onRobotFocusGained(qiContext);
    }

    @Override
    public void onRobotFocusLost() {
        Log.w(TAG, "Lost Robot Focus");
        robotHelper.onRobotFocusLost();
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.e(TAG, "Robot focus was refused with reason: " + reason);
    }


}
