# MapLocalizeAndMove
**Article and code updated on 24/03/2020**

Navigation tutorial for Pepper running on Android.

This version is compatible with Pepper running Naoqi OS 2.9.4 and more.

## Navigation using the qisdk

All customers are longing to see Pepper move around their premises. Here is an overview of everything the [QiSDK](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/index.html) provides today for creating a successful navigation experience with the robot. 

Creating a navigation use case needs to be done in 2 steps: Setup-time, when the robot learns its environment in order to get a usable map, and Production-time, when the robot navigates using this map. Those 2 steps can be done in the same app or in two different apps, but the goal is to have to do the setup once only, and then only run production. 

**Note:** Navigation maps and Points of Interest (_PoI_) are specific to one app. In Android, if you want to share data between several apps you need to [do it explicitely](https://developer.android.com/training/secure-file-sharing/). This means that by default, the map and the points you create in one app are not usable in another. The whole process needs to be restarted. Keep this in mind when you save the information if you need several apps to access them. 

### Setup time

During setup, you need to teach your robot the map of the room(s) it will have to navigate in, as well as all the points of interest in this room. This is done manually, pushing the robot around the room to show it the path it is able to drive through. The goal is to create an [ExplorationMap](https://qisdk.softbankrobotics.com/sdk/doc/qisdk/index.html?com/aldebaran/qi/sdk/object/actuation/ExplorationMap.html) object.

#### 1) Mapping the environment

As of API level 6, there is no possibility to provide a blueprint of your premises to Pepper so it can locate itself in it. The navigation engine only supports maps that are created by Pepper, in the same API version. This step should be executed when the place is empty, otherwise people or others will be recorded in the map as obstacles.

##### 1.1) Define and learn the right "base starting point" named MapFrame

The MapFrame is where you plan to start the app everyday. This position will be learnt very precisely by Pepper and will represent the "zero", the origin of your map. The robot will be able to relocate very precisely at this position, hence getting a good initial position to start navigating in the morning! 

Make sure the charging hatch is *closed*, make sure there is no one around the robot (they would be recognized as permanent obstacles) for at least 15s. Then start [LocalizeAndMap](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize_and_map.html). This will make the robot look around, effectively taking 360° pictures that will be used to recognize its location and orientation later on. 
<pre><code class="java">
// Build the action.
LocalizeAndMap localizeAndMap = LocalizeAndMapBuilder.with(qiContext).build();

// Run the action asynchronously.
Future<Void> localizingAndMapping = localizeAndMap.async().run();
</code></pre>

##### 1.2) Show the robot around

Once the mapping of the origin of the map (MapFrame) is finished, open the charging hatch. Place yourself *behind* the robot and slowly push it around the premises. 

###### 1.8a robots
During this step, the robot uses its cameras, odometry and its laser sensors to map its close environment. If you don't stay behind the robot(don't walk on the side of Pepper), it will see you as an obstacle and block this way in the final map. Also ideally hold the tracking so the head stays in the front of the robot, and move slowly so Pepper has the time to record more information from the lasers. For better result, try not to make loops but rather straight paths or radial movements.
During this step, the robot will also learn the paths to navigate through the map. Pepper will always prefer the path you used during this mapping phase when moving. 
Last, if you know some places are key positions where the robot may need to relocalize, then stop and make a 360° at that place, so the robot takes pictures all around and not only in front.

###### 1.8 robots (better performances)
During this step, the robot uses its stereo cameras. They work as two smartphones cameras but in grayscale (no color). It does Visual SLAM, constant relocalization with image comparison and odometry to map its environment.
A visual landmark detection is performed, we try to match contrasted corners. Pictures are not saved, only mathematical data associated to it.

1. Try to maximize the amount of texture in the environment:
* Good :  posters, plants, random stuff
* Bad : empty white wall

2. You should try to avoid blur during LocalizeAndMap and minimize it during Localize actions. In order to do that:
* For lighting conditions we recommend 300lux minimum and 500lux or more as nice too have criteria.
* Try to have a moving speed of the robot of 0.3m.s-1 during LocalizeAndMap (or less).

3. Try to avoid obstructions of the cameras during LocalizeAndMap. Try avoiding people standing in front of the robot during LocalizeAndMap. If you push the robot, push it from the back.

4. Make the robot move (pushing or joystick) along the desired navigation path (route) during the Localize and map. It will help localizing during production mode as the robot sees the same pictures then.
* Keep head angles at 0 degrees (pitch an yaw) during the LocalizeAndMap. It helps seeing the same pictures in production mode (better than looking at the floor or the ceiling).
* During localizeAndMap, make the robot move as if it was a car and the tablet is the front of the car. It helps taking pictures in front of the robot (then the robot will navigate this way).

5. Try to come back exactly at the starting position before finishing (cancel) the LocalizeAndMap. It helps improving the consistency of the map.

*It is now possible to make loops in your app.*  And this is even Better.

Also ideally hold the tracking so the head stays in the front of the robot, and move slowly so the pictures are of good quality and Pepper can record more of them. *It is now possible to make loops in your app.*  And this is even Better.

**When doing the map, it is necessary to go along the robot path in both directions to able the robot to take pictures of its environnement in both directions. Then, when it will navigate, it will be able to localize itself in both directions.
Always go back to the MapFrame when you are done mapping before stopping the LocalizeAndMap action and saving the map.**

              -->
    A X------------------X B
              <--


 
##### 1.3) Save the map


Congratulations, Pepper knows your premises! You can now stop mapping and save the map: 
<pre><code class="java">
// Stop the action.
localizingAndMapping.requestCancellation();

// After having moved the robot around, dump the current description of the explored environment.
ExplorationMap explorationMap = localizeAndMap.dumpMap();

// Serialize the ExplorationMap data.
String mapData = explorationMap.serialize();

// write the String into a file
/* choose where and how to save */
</code></pre>

#### 2) Learn the Points of Interest

In this map, we now need to teach Pepper some specific locations it will later need to go to. Here we will only use [Localize](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize.html) which does not modify the map anymore, so it is safe to pass through Pepper's lasers.

##### 2.1) Localize the robot

Let's reset the location of the robot. For this, place the robot close to the MapFrame (where you started the [LocalizeAndMap](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize_and_map.html earlier). Then launch a [Localize](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize.html).

<pre><code class="java">
// Build the action.
Localize localize = LocalizeBuilder.with(qiContext)
                                   .withMap(explorationMap)
                                   .build();
// Run the action asynchronously.
localize.async().run();
</code></pre>

The robot will look around to find its right location in the maps you built earlier. Once this is done, the [LocalizationStatus](https://qisdk.softbankrobotics.com/sdk/doc/qisdk/index.html?com/aldebaran/qi/sdk/object/actuation/LocalizationStatus.html) will be LOCALIZED. To make sure you get notified when the robot is ready you can use the code: 

<pre><code class="java">
// Add a listener to get the map when localized.
localize.addOnStatusChangedListener(localizationStatus -> {
    if (localizationStatus == LocalizationStatus.LOCALIZED) {
        /* robot is ready! */
    }
});
</code></pre>


##### 2.2) Take the robot to the points of interests

Open the hatch and push the robot to the different position you want to teach. Using its odometry, cameras, lasers and previously learnt map, the robot will know where it is compared to the origin of the map(MapFrame), at all time. Once you get to those position, name and record them (into a hashmap for instance) using the code below. A good working example for this part can be found [here](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/tuto/goto_world_tutorial.html).
<pre><code class="java">
private Map<String, AttachedFrame> savedLocations = new HashMap<>();

void saveLocation(final String locationName) {
    // Get the robot frame asynchronously.
    qiContext.getActuation().async()
        .robotFrame()
        .andThenApply(robotFrame -> {
            // Get the mapFrame, origin of the map, so all positions are relative to this one.
            Frame mapFrame = qiContext.getMapping().mapFrame();

            // Transform between the current robot location (robotFrame) and the mapFrame
            TransformTime transformTime = robotFrame.computeTransform(mapFrame);

            // Create an AttachedFrame representing the current robot frame relatively to the MapFrame
            return mapFrame.makeAttachedFrame(transformTime.getTransform());
        })
        .andThenConsume(attachedFrame -> savedLocations.put(location, attachedFrame));
}
</code></pre>

##### 2.3) Save the points of interest

When you have recorded all the [Frames](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/frame.html) you need, save the list in a file you can reopen later. Note that it is not possible to serialize a [Frame](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/frame.html) directly, but rather the translation between two frames (your frame and the mapFrame in our case). So you need to extract the translation component of the [Transorm](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/transform_transformtime.html) and record the two coordinates and the orientation (X, Y, Theta) instead. 


For simplicity, a class to regroup the two coordinates X / Y and the orientation Theta :
<pre><code class="java">
public class Vector2theta implements Parcelable, Serializable {
    private double x, y, theta;

    private Vector2theta(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    /**
     * creates a Vector2theta representing the translation between two frames and angle
     * @param frameOrigin the origin of the translation
     * @param frameDestination the end of the translation
     * @return the Vector2theta to go from frameOrigin to frameDestination
     */
    public static Vector2theta betweenFrames(@NonNull Frame frameDestination, @NonNull Frame frameOrigin) {
        // Compute the transform to go from "frameOrigin" to "frameDestination"
        Transform transform = frameOrigin.async().computeTransform(frameDestination).getValue().getTransform();

        // Extract translation from the transform
        Vector3 translation = transform.getTranslation();
        // Extract quaternion from the transform
        Quaternion quaternion = transform.getRotation();

        // Extract the 2 coordinates from the translation and orientation angle from quaternion
        return new Vector2theta(translation.getX(), translation.getY(), NavUtils.getYawFromQuaternion(quaternion));
    }

    /**
     * Returns a transform representing the translation described by this Vector2theta
     * @return the transform
     */
    public Transform createTransform() {
        // this.theta is the radian angle to appy taht was serialized
        return TransformBuilder.create().from2DTransform(this.x, this.y, this.theta);
    }
    /***************** Add here automatic Parcelable implementaion (hidden for readability) *******************/
}
</code></pre>

To backup the location, we will first transform savedLocation into a HashMap containing serializable objects (Vector2Theta). Then we can write this new Hashmap directly into a file using OutputStream. 

<pre><code class="java">
public void backupLocations() {

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

}
</code></pre>

Write a function  "locationsToBackup" into a file, using "ObjectOutputStream" for example 

<pre><code class="java">
public void saveLocationsToFile(Context applicationContext, Map<String, Vector2theta> locationsToBackup) {

        Gson gson=new Gson();
        String points =gson.toJson(locationsToBackup);

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        // Backup list into a file
        try {
            fos = new FileOutputStream(new File(applicationContext.getFilesDir(), "points.json"));
            oos = new ObjectOutputStream(fos);
            oos.writeObject(points);
            Log.d(TAG, "backupLocations: Done");
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }
</code></pre>



### Production

#### 1) Load the map and find the current location

##### 1.1) Place the robot at its MapFrame (base location)

First, with the charging hatch open, move the robot to a position close to the base position you defined [earlier](https://partner-portal.aldebaran.com/projects/knowledge-base/wiki/Navigation_using_the_qisdk#1-Mapping-the-environment). Using [Localize](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize.html) the robot will use its lasers and its cameras to look around for known features in its environment. It is theoretically able to locate itself anywhere (Only with 1.8 robot since API 6)  within the [ExplorationMap](https://qisdk.softbankrobotics.com/sdk/doc/qisdk/index.html?com/aldebaran/qi/sdk/object/actuation/ExplorationMap.html) you created during setup, but for consistent performances we recommend that you start close to the base location(MapFrame).

##### 1.2) Let the robot locate itself

Close the charging hatch, load the map you created earlier and run a [Localize](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/localize.html) :

<pre><code class="java">
// Get an exploration map.
ExplorationMap explorationMap = ...;

// Build the action.
Localize localize = LocalizeBuilder.with(qiContext)
                                   .withMap(explorationMap)
                                   .build();
// Run the action asynchronously.
localize.async().run();
</code></pre>

+Note:+ If you need, the robot can also readjust automatically and go to its precise base location with a simple [GoTo](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/goto.html action):  
<pre><code class="java">
// Add a listener to get the map when localized.
localize.addOnStatusChangedListener(localizationStatus -> {
    if (localizationStatus == LocalizationStatus.LOCALIZED) {
        // Retrieve the map frame and go to it.
        movement = qiContext.getMapping().async().mapFrame()
                .andThenCompose(mapFrame -> GoToBuilder.with(qiContext).withFrame(mapFrame).buildAsync())
                .andThenCompose(goTo -> goTo.async().run())
                .thenConsume(future -> {
                    if (future.isSuccess()) {
                        Log.d(TAG, "Map frame reached successfully");
                    } else if (future.hasError()) {
                        Log.e(TAG, "Error while going to map frame", future.getError());
                    }
                });
    }
});
</code></pre>

#### 2) Move around

##### 2.1) Reload known locations

First, load and deserialize the list of PoI into the map of Frames. This uses the same Vector2Theta as defined earlier. 
<pre><code class="java">
private void loadLocations() {
	
        return FutureUtils.futureOf((f) -> {
            // Read file into a temporary hashmap
            File file = new File(getFilesDir(), "points.json");
            if (file.exists()) {
                // Read file into a temporary hashmap, for example using "ObjectInputStream"
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
}
</code></pre>

##### 2.2) Navigate

Whenever you need the robot to go somewhere, you need to build the [Frame](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/frame.html) back from the translation. The helper [Transform.from2DTranslation(double x, double y, double thetaRad)](https://qisdk.softbankrobotics.com/sdk/doc/qisdk/index.html?com/aldebaran/qi/sdk/builder/TransformBuilder.html) will help you. Then, just use the [Frame](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/frame.html) to build and run a [GoTo](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/goto.html) action
<pre><code class="java">
// Store the GoTo action.
private GoTo goTo;

void goToLocation(final String location) {
    // Get the FreeFrame from the saved locations.
    FreeFrame freeFrame = savedLocations.get(location);

    // Extract the Frame asynchronously.
    Future<Frame> frameFuture = freeFrame.async().frame();
    frameFuture.andThenCompose(frame -> {
        // Create a GoTo action.
        goTo = GoToBuilder.with(qiContext)
                .withFrame(frame)
                .build();

        // Display text when the GoTo action starts.
        goTo.addOnStartedListener(() -> Log.i(TAG, "Moving..."));

        // Execute the GoTo action asynchronously.
        return goTo.async().run();
    }).thenConsume(future -> {
        if (future.isSuccess()) {
            Log.i(TAG, "Location reached: " + location);
        } else if (future.hasError()) {
            Log.e(TAG, "Go to location error", future.getError());
        }
    });
}
</code></pre>

**Note**: If you need the robot to go back to its base location, you can provide the [mapFrame](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/frame.html#map-frame) to a [GoTo](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/movement/reference/goto.html) action:  
<pre><code class="java">
void goHome() {
    Mapping mapping = qiContext.getMapping();
    Frame mapFrame = mapping.mapFrame();
    GoTo goTo = GoToBuilder.with(qiContext).withFrame(mapFrame).build();
    // Display text when the GoTo action starts.
    goTo.addOnStartedListener(() -> Log.i(TAG, "Moving..."));
    goTo.async().run().thenConsume(future -> {
        if (future.isSuccess()) {
            Log.i(TAG, "Location reached: Home");
        } else if (future.hasError()) {
            Log.e(TAG, "Go Home error", future.getError());
        }
    });
}
</code></pre>