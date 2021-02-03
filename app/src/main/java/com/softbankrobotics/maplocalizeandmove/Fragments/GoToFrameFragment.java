package com.softbankrobotics.maplocalizeandmove.Fragments;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.OrientationPolicy;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.softbankrobotics.maplocalizeandmove.MainActivity;
import com.softbankrobotics.maplocalizeandmove.R;
import com.softbankrobotics.maplocalizeandmove.Utils.GoToHelper;
import com.softbankrobotics.maplocalizeandmove.Utils.PointsOfInterestView;
import com.softbankrobotics.maplocalizeandmove.Utils.Popup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GoToFrameFragment extends Fragment {

    private static final String TAG = "MSI_GoToFrameFragment";
    public Popup goToPopup = null;
    public ImageView check;
    public TextView goto_finished_text;
    public ImageView cross;
    public LottieAnimationView goto_loader;
    private TextView goto_text;
    private MainActivity ma;
    private View localView;
    private Button goToRandomButton;
    private Button stopGoToRandomButton;
    private Button close_button;
    private LinearLayout linearLayout;
    private int gotoRouteCounter = 0;
    private boolean goToRouteSuccess = false;
    private boolean goToRouteLoop = false;
    private Frame robotFrame;
    private Frame mapFrame;
    private List<PointF> poiPositions = null;

    /**
     * Inflates the layout associated with this fragment.
     * If an application theme is set, it will be applied to this fragment.
     */
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        int fragmentId = R.layout.fragment_goto_frame;
        this.ma = (MainActivity) getActivity();
        if (ma != null) {
            Integer themeId = ma.getThemeId();
            if (themeId != null) {
                final Context contextThemeWrapper = new ContextThemeWrapper(ma, themeId);
                LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
                return localInflater.inflate(fragmentId, container, false);
            } else {
                return inflater.inflate(fragmentId, container, false);
            }
        } else {
            Log.d(TAG, "could not get mainActivity, can't create fragment");
            return null;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        localView = view;
        view.findViewById(R.id.back_button).setOnClickListener((v) ->
                ma.setFragment(new ProductionFragment(), true));

        goToRandomButton = view.findViewById(R.id.goto_random);
        stopGoToRandomButton = view.findViewById(R.id.stop_goto_random);

        Button goToCharge = view.findViewById(R.id.goto_charge);
        goToCharge.setOnClickListener(v -> {
            if (ma.savedLocations.containsKey("ChargingStation") && !ma.robotHelper.askToCloseIfFlapIsOpened()) {
                goToLocation("ChargingStation", OrientationPolicy.ALIGN_X);
            } else {
                ma.robotHelper.say("There is no point of interest named ChargingStation saved in memory");
                Log.d(TAG, "There is no frame ChargingStation saved in memory: ");
            }
        });

        if (ma.goToRandomWasActivated) {
            Log.d(TAG, " : goToRandomWasActivated");
            gotoRandomButtonActivated();
            ma.goToRandomLocation(true);
            ma.goToRandomWasActivated = false;
        } else {
            stopGoToRandomButtonActivated();
        }

        goToRandomButton.setOnClickListener((v) -> {
            if (!ma.robotHelper.askToCloseIfFlapIsOpened()) {
                gotoRandomButtonActivated();
                ma.goToRandomLocation(true);
            }
        });

        stopGoToRandomButton.setOnClickListener((v) -> {
            stopGoToRandomButtonActivated();
            ma.goToRandomLocation(false);
        });

        Switch goToMaxSpeedSwitch = view.findViewById(R.id.gotomaxspeed);
        goToMaxSpeedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> ma.goToMaxSpeed = isChecked);
        if (ma.goToMaxSpeed) {
            Log.d(TAG, " : goToMaxSpeed");
            goToMaxSpeedSwitch.setChecked(true);
        }

        Switch goToStraightSwitch = view.findViewById(R.id.gotostraight);
        goToStraightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> ma.goToStraight = isChecked);
        if (ma.goToStraight) {
            Log.d(TAG, " : goToStraight");
            goToStraightSwitch.setChecked(true);
        }

        Switch goToRouteSwitch = view.findViewById(R.id.goto_route_loop);
        goToRouteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> goToRouteLoop = isChecked);
        if (ma.goToRouteWasActivated) {
            Log.d(TAG, " : goToRouteWasActivated");
            goToRouteSwitch.setChecked(true);
            goToRoute();
            ma.goToRouteWasActivated = false;
        }

        Button goToRoute = view.findViewById(R.id.goto_route);
        goToRoute.setOnClickListener(v -> {
            if (!ma.robotHelper.askToCloseIfFlapIsOpened()) {
                Log.d(TAG, "onViewCreated: fillListInOrder()");
                fillRouteInOrder();
                goToRoute();
            }
        });
        displayLocations();
    }


    /**
     * If gotoRandomButton was Activated, disable it and enable stopGotoRandomButton.
     */
    private void gotoRandomButtonActivated() {
        goToRandomButton.setEnabled(false);
        goToRandomButton.setAlpha((float) 0.5);
        stopGoToRandomButton.setEnabled(true);
        stopGoToRandomButton.setAlpha(1);
    }

    /**
     * If stopGotoRandomButton was Activated, disable it and enable gotoRandomButton.
     */
    private void stopGoToRandomButtonActivated() {
        stopGoToRandomButton.setEnabled(false);
        stopGoToRandomButton.setAlpha((float) 0.5);
        goToRandomButton.setEnabled(true);
        goToRandomButton.setAlpha(1);
    }


    /**
     * Create a Popup that will be displayed when Pepper is moving.
     */
    public void createGoToPopup() {
        if (goToPopup == null) {
            goToPopup = new Popup(R.layout.popup_goto, this, ma);
            goto_loader = goToPopup.inflator.findViewById(R.id.goto_loader);
            check = goToPopup.inflator.findViewById(R.id.check);
            goto_text = goToPopup.inflator.findViewById(R.id.goto_text);
            goto_finished_text = goToPopup.inflator.findViewById(R.id.goto_finished_text);
            cross = goToPopup.inflator.findViewById(R.id.cross);
            close_button = goToPopup.inflator.findViewById(R.id.close_button);
            close_button.setOnClickListener((v) -> {
                close_button.setVisibility(View.GONE);
                ma.robotHelper.goToHelper.checkAndCancelCurrentGoto();
                Log.d(TAG, "GoToPopup: GoTo Action canceled by user");
            });

            // Retrieve the robot Frame
            robotFrame = (ma.qiContext.getActuationAsync()).getValue().async().robotFrame().getValue();

            // Retrieve the origin of the map Frame
            mapFrame = (ma.qiContext.getMappingAsync()).getValue().async().mapFrame().getValue();

            PointsOfInterestView explorationMapView = goToPopup.inflator.findViewById(R.id.explorationMapViewPopup);

            ma.robotHelper.localizeAndMapHelper.buildStreamableExplorationMap().andThenConsume(value -> {
                poiPositions = new ArrayList<>(ma.savedLocations.size() + 1);
                for (Map.Entry<String, AttachedFrame> stringAttachedFrameEntry : ma.savedLocations.entrySet()) {
                    Transform transform = (((stringAttachedFrameEntry.getValue()).async().frame()).getValue().async().computeTransform(mapFrame)).getValue().getTransform();
                    poiPositions.add(new PointF(((float) transform.getTranslation().getX()), (float) transform.getTranslation().getY()));
                    //Log.d(TAG, "createGoToPopup: transform: "+(((stringAttachedFrameEntry.getValue()).async().frame()).getValue().async().computeTransform(mapFrame)).getValue().getTransform().getTranslation().toString());
                }
                explorationMapView.setExplorationMap(value.getTopGraphicalRepresentation());
                explorationMapView.setMapFramPosition();
                explorationMapView.setPoiPositions(poiPositions);
            }).andThenConsume(value -> {
                int delay = 0;
                int period = 500;  // repeat every sec.
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        // Compute the position of the robot relatively to the map Frame
                        Transform robotPos = robotFrame.computeTransform(mapFrame).getTransform();
                        // Set the position in the ExplorationMapView widget, it will be displayed as a red circle
                        explorationMapView.setRobotPosition(robotPos);
                    }
                }, delay, period);
            });
        } else {
            ma.runOnUiThread(() -> {
                cross.setVisibility(View.GONE);
                check.setVisibility(View.GONE);
                goto_finished_text.setVisibility(View.GONE);
                goto_loader.setVisibility(View.VISIBLE);
                close_button.setVisibility(View.VISIBLE);
            });
        }
    }


    /**
     * Go to the requested Location.
     * If it successfully ends the goTo, and if the Location is ChargingStation, it will
     * start the docking process.
     *
     * @param location
     * @param orientationPolicy
     */
    public void goToLocation(String location, OrientationPolicy orientationPolicy) {
        createGoToPopup();
        ma.runOnUiThread(() ->goto_text.setText("Let's go to " + location + " !"));
        ma.robotHelper.goToHelper.addOnStartedMovingListener(() -> ma.runOnUiThread(() -> {
            goToPopup.dialog.show();
            goToPopup.dialog.getWindow().setAttributes(goToPopup.lp);
            ma.robotHelper.goToHelper.removeOnStartedMovingListeners();
            Log.d(TAG, "goToLocation: removeOnStartedMovingListeners");
        }));
        Log.d(TAG, "goToLocation: addOnStartedMovingListener");
        ma.robotHelper.goToHelper.addOnFinishedMovingListener((goToStatus) -> {
            ma.robotHelper.goToHelper.removeOnFinishedMovingListeners();
            Log.d(TAG, "goToLocation: removeOnFinishedMovingListeners");
            Log.d(TAG, "goToLocation : " + goToStatus);
            if (!location.equalsIgnoreCase("ChargingStation")) {
                ma.robotHelper.releaseAbilities();
            }
            ma.runOnUiThread(() -> {
                if (goToStatus == GoToHelper.GoToStatus.CANCELLED) {
                    if (goToPopup.dialog.isShowing()) {
                        goToPopup.dialog.hide();
                    }
                } else {
                    goto_loader.setVisibility(View.GONE);
                    close_button.setVisibility(View.GONE);
                    if (goToStatus == GoToHelper.GoToStatus.FINISHED) {
                        check.setVisibility(View.VISIBLE);
                        goto_finished_text.setVisibility(View.VISIBLE);
                    } else {
                        cross.setVisibility(View.VISIBLE);
                    }

                    FutureUtils.wait(4, TimeUnit.SECONDS).thenConsume(aUselessFuture -> {
                        ma.runOnUiThread(() -> {
                            if (goToPopup.dialog.isShowing()) {
                                goToPopup.dialog.hide();
                            }
                        });
                        if (goToStatus == GoToHelper.GoToStatus.FINISHED && location.equalsIgnoreCase("ChargingStation")) {
                            ma.dockOnChargingStation(false);
                        }
                    });
                }
            });
        });
        Log.d(TAG, "goToLocation: addOnFinishedMovingListener");
        ma.goToLocation(location, orientationPolicy);
    }

    /**
     * Fill the list locationsToGoTo following the order set with the dropdown.
     */
    private void fillRouteInOrder() {
        Log.d(TAG, "fillRouteInOrder");
        LinearLayout currentLayout;
        ma.locationsToGoTo = new ArrayList<>(ma.savedLocations.size() + 1);
        while (ma.locationsToGoTo.size() < ma.savedLocations.size() + 1)
            ma.locationsToGoTo.add(null);
        for (int i = 0; i < (ma.savedLocations.size() * 2 + 1); i += 2) {
            //Log.d(TAG, "fillRouteInOrderThenGoto: " + i);
            currentLayout = (LinearLayout) linearLayout.getChildAt(i);
            Spinner dropdown = (Spinner) ((RelativeLayout) currentLayout.getChildAt(1)).getChildAt(0);
            int order = Integer.parseInt((String) dropdown.getSelectedItem());
            TextView locationName = (TextView) currentLayout.getChildAt(2);
            //Log.d(TAG, "fillRouteInOrderThenGoto: LocationName : " + locationName.getText() + " : " + order);
            if (order != 0) ma.locationsToGoTo.set(order - 1, (String) locationName.getText());
        }

        for (int j = ma.locationsToGoTo.size() - 1; j >= 0; j--) {
            //Log.d(TAG, "Order: Location name: " + ma.locationsToGoTo.get(j));
            ma.locationsToGoTo.remove(null);
        }
    }

    /**
     * Pepper will go to each Location present in the locationsToGoTo list.
     * If it successfully ends the route, and if the latest Location is ChargingStation, it will
     * start the docking process.
     */
    public void goToRoute() {
        gotoRouteCounter = 0;
        ma.goToRouteRunning = true;
        if (ma.locationsToGoTo.size() != 0) {
            goToRouteSuccess = false;
            createGoToPopup();
            ma.runOnUiThread(() -> goto_text.setText("Let's go to " + ma.locationsToGoTo.get(gotoRouteCounter) + " !"));
            ma.robotHelper.goToHelper.addOnStartedMovingListener(() -> ma.runOnUiThread(() -> {
                goToPopup.dialog.show();
                goToPopup.dialog.getWindow().setAttributes(goToPopup.lp);
                ma.robotHelper.goToHelper.removeOnStartedMovingListeners();
                Log.d(TAG, "goToRoute: removeOnStartedMovingListeners");
            }));
            Log.d(TAG, "goToRoute: addOnStartedMovingListener");
            ma.robotHelper.goToHelper.addOnFinishedMovingListener((goToStatus) -> {
                Log.d(TAG, "goToRoute : " + goToStatus);
                gotoRouteCounter++;
                if (goToStatus == GoToHelper.GoToStatus.FINISHED && gotoRouteCounter < ma.locationsToGoTo.size()) {
                    ma.runOnUiThread(() -> goto_text.setText("Let's go to " + ma.locationsToGoTo.get(gotoRouteCounter) + " !"));
                    if (gotoRouteCounter == ma.locationsToGoTo.size() - 1) {
                        ma.goToLocation(ma.locationsToGoTo.get(gotoRouteCounter), OrientationPolicy.ALIGN_X);
                    } else {
                        ma.goToLocation(ma.locationsToGoTo.get(gotoRouteCounter), OrientationPolicy.FREE_ORIENTATION);
                    }
                } else {
                    ma.robotHelper.goToHelper.removeOnFinishedMovingListeners();
                    Log.d(TAG, "goToRoute: removeOnFinishedMovingListeners");
                    if (!ma.locationsToGoTo.get(gotoRouteCounter - 1).equalsIgnoreCase("ChargingStation")) {
                        ma.robotHelper.releaseAbilities();
                    }
                    ma.runOnUiThread(() -> {
                        if (goToStatus == GoToHelper.GoToStatus.CANCELLED) {
                            goToRouteSuccess = false;
                            ma.goToRouteRunning = false;
                            if (goToPopup.dialog.isShowing()) {
                                goToPopup.dialog.hide();
                            }
                        } else {
                            goto_loader.setVisibility(View.GONE);
                            close_button.setVisibility(View.GONE);
                            if (goToStatus == GoToHelper.GoToStatus.FINISHED) {
                                Log.d(TAG, "goToRoute: ended with success");
                                check.setVisibility(View.VISIBLE);
                                goto_finished_text.setVisibility(View.VISIBLE);
                                goToRouteSuccess = true;
                            } else {
                                Log.d(TAG, "goToRoute: failed to complete route");
                                cross.setVisibility(View.VISIBLE);
                                goToRouteSuccess = false;
                                ma.goToRouteRunning = false;
                            }

                            FutureUtils.wait(4, TimeUnit.SECONDS).thenConsume(aUselessFuture -> {
                                ma.runOnUiThread(() -> {
                                    if (goToPopup.dialog.isShowing()) {
                                        goToPopup.dialog.hide();
                                        Log.d(TAG, "goToRoute:  goToPopup.dialog.hide() 4 secondes");
                                    }
                                });
                                Log.d(TAG, "goToRouteSuccess: " + goToRouteSuccess + ", goToRouteLoop: " + goToRouteLoop);
                                if (goToStatus == GoToHelper.GoToStatus.FINISHED && ma.locationsToGoTo.get(gotoRouteCounter - 1).equalsIgnoreCase("ChargingStation")) {
                                    // To remove "ChargingStation" from locationsToGoTo list, if and only if, it has been added cause of a DOCKING_SOON signal
                                    fillRouteInOrder();
                                    ma.dockOnChargingStation(false);
                                } else if (goToRouteSuccess && goToRouteLoop) {
                                    goToRoute();
                                }
                            });
                        }
                    });
                }
            });
            Log.d(TAG, "goToRoute: addOnFinishedMovingListener");
            ma.goToLocation(ma.locationsToGoTo.get(gotoRouteCounter), OrientationPolicy.FREE_ORIENTATION);
        } else ma.robotHelper.say("Please add at least one location to go to in the Route");
    }


    /**
     * Display on the tablet the locations that are saved in memory in addition to MapFrame.
     */
    private void displayLocations() {
        Log.d(TAG, "displayLocations: started");
        if (ma.savedLocations.isEmpty()) {
            try {
                ma.loadLocations().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (ma.savedLocations.isEmpty()) noLocationsToLoad();
        else {
            LayoutInflater inflater = this.getLayoutInflater();
            linearLayout = localView.findViewById(R.id.container);
            linearLayout.removeAllViews();

            //Adding mapFrame in List
            LinearLayout mapFrameLayout = (LinearLayout) inflater.inflate(R.layout.item_location_list_goto, null);
            Spinner dropdown = (Spinner) ((RelativeLayout) mapFrameLayout.getChildAt(1)).getChildAt(0);
            String[] items = new String[ma.savedLocations.size() + 2];
            for (int i = 0; i < items.length; i++) {
                items[i] = String.valueOf(i);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ma, android.R.layout.simple_spinner_dropdown_item, items);
            dropdown.setAdapter(adapter);
            if (ma.locationsToGoTo != null && ma.locationsToGoTo.contains(getString(R.string.map_frame))) {
                dropdown.setSelection(ma.locationsToGoTo.indexOf(getString(R.string.map_frame)) + 1);
            }

            TextView mapFrameName = (TextView) mapFrameLayout.getChildAt(2);
            mapFrameName.setText(R.string.map_frame);
            ImageView separationList = new ImageView(this.getContext());
            separationList.setImageResource(R.drawable.ic_separation_list);
            Button goToMapFrameButton = (Button) mapFrameLayout.getChildAt(3);
            goToMapFrameButton.setOnClickListener((useless) -> {
                if (!ma.robotHelper.askToCloseIfFlapIsOpened()) {
                    goToLocation("mapFrame", OrientationPolicy.ALIGN_X);
                }
            });
            linearLayout.addView(mapFrameLayout);
            linearLayout.addView(separationList);

            //Adding all locations loaded in list
            for (Map.Entry<String, AttachedFrame> stringAttachedFrameEntry : ma.savedLocations.entrySet()) {
                LinearLayout v = (LinearLayout) inflater.inflate(R.layout.item_location_list_goto, null);
                Spinner dropdownBis = (Spinner) ((RelativeLayout) v.getChildAt(1)).getChildAt(0);
                String[] itemsBis = new String[ma.savedLocations.size() + 2];
                for (int i = 0; i < itemsBis.length; i++) {
                    itemsBis[i] = String.valueOf(i);
                }
                ArrayAdapter<String> adapterBis = new ArrayAdapter<>(ma, android.R.layout.simple_spinner_dropdown_item, itemsBis);
                dropdownBis.setAdapter(adapterBis);
                if (ma.locationsToGoTo != null && ma.locationsToGoTo.contains(((Map.Entry) stringAttachedFrameEntry).getKey().toString())) {
                    dropdownBis.setSelection(ma.locationsToGoTo.indexOf(((Map.Entry) stringAttachedFrameEntry).getKey().toString()) + 1);
                }

                TextView locationName = (TextView) v.getChildAt(2);
                locationName.setText(((Map.Entry) stringAttachedFrameEntry).getKey().toString());
                ImageView separationListbis = new ImageView(this.getContext());
                separationListbis.setImageResource(R.drawable.ic_separation_list);
                Button goToButton = (Button) v.getChildAt(3);
                goToButton.setOnClickListener((useless) -> {
                    if (!ma.robotHelper.askToCloseIfFlapIsOpened()) {
                        goToLocation(((Map.Entry) stringAttachedFrameEntry).getKey().toString(), OrientationPolicy.ALIGN_X);
                    }
                });
                linearLayout.addView(v);
                linearLayout.addView(separationListbis);

            }
        }
        Log.d(TAG, "displayLocations: finished");
    }


    /**
     * If there is no location to load, display a popup to inform the user.
     */
    private void noLocationsToLoad() {
        Popup noLocationsPopup = new Popup(R.layout.popup_no_locations_to_load, this, ma);
        Button close = noLocationsPopup.inflator.findViewById(R.id.close_button);
        close.setOnClickListener((v) -> {
            noLocationsPopup.dialog.hide();
            ma.setFragment(new MainFragment(), true);
        });
        noLocationsPopup.dialog.show();
    }

}
