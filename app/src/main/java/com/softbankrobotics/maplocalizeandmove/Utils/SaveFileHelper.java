package com.softbankrobotics.maplocalizeandmove.Utils;

import android.util.Log;

import com.aldebaran.qi.sdk.object.streamablebuffer.StreamableBuffer;
import com.aldebaran.qi.sdk.object.streamablebuffer.StreamableBufferFactory;
import com.aldebaran.qi.sdk.util.StreamableBufferUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class SaveFileHelper {

    private static String TAG = "MSI_SaveFileHelper";

    /**
     * Get the locations from file.
     *
     * @param filesDirectoryPath
     * @param LocationsFileName
     * @return the HashMap of the locations
     */
    public Map<String, Vector2theta> getLocationsFromFile(String filesDirectoryPath, String LocationsFileName) {
        Map<String, Vector2theta> vectors = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        File f = null;
        try {
            f = new File(filesDirectoryPath, LocationsFileName);
            fis = new FileInputStream(f);
            ois = new ObjectInputStream(fis);
            String points = (String) ois.readObject();
            Type collectionType = new TypeToken<Map<String, Vector2theta>>() {
            }.getType();
            Gson gson = new Gson();
            vectors = gson.fromJson(points, collectionType);

        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return vectors;
    }

    /**
     * Save the list of Location to file.
     *
     * @param filesDirectoryPath The directory where to save the Locations
     * @param locationsFileName  The name of the file in which it saves the Locations
     * @param locationsToBackup  Map of Locations to save
     */
    public void saveLocationsToFile(String filesDirectoryPath, String locationsFileName, TreeMap<String, Vector2theta> locationsToBackup) {

        Gson gson = new Gson();
        String points = gson.toJson(locationsToBackup);

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        // Backup list into a file
        try {
            File fileDirectory = new File(filesDirectoryPath, "");
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs();
            }
            File file = new File(fileDirectory, locationsFileName);
            fos = new FileOutputStream(file);
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

    /**
     * Save the map data as StreamableBuffer in file.
     *
     * @param filesDirectoryPath The directory where to save the map
     * @param fileName           The name of the file in which it saves the map
     * @param data               The map to save
     */
    public void writeStreamableBufferToFile(String filesDirectoryPath, String fileName, StreamableBuffer data) {
        FileOutputStream fos = null;
        try {
            Log.d(TAG, "writeMapDataToFile: started");

            File fileDirectory = new File(filesDirectoryPath, "");
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs();
            }
            File file = new File(fileDirectory, fileName);
            fos = new FileOutputStream(file);
            StreamableBufferUtil.copyToStream(data, fos);
        } catch (IOException e) {
            Log.d("Exception", "File write failed: " + e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "writeMapDataToFile:  Finished");
    }


    /**
     * Get the map data as StreamableBuffer from file.
     *
     * @param filesDirectoryPath The directory from which to load the map
     * @param fileName           The name of file from which to load the map
     * @return A StreamableBuffer of map data
     */
    public StreamableBuffer readStreamableBufferFromFile(String filesDirectoryPath, String fileName) {

        StreamableBuffer data = null;
        File f = null;

        try {
            f = new File(filesDirectoryPath, fileName);
            if (f.length() == 0)
                return null;
            data = fromFile(f);
            return data;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Build the map data from file as StreamableBuffer.
     *
     * @param file The name of file from which to load the map
     * @return A StreamableBuffer of map data
     */
    private StreamableBuffer fromFile(File file) {
        return StreamableBufferFactory.fromFunction(file.length(), (offset, size) -> {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                byte[] byteArray = new byte[size.intValue()];
                randomAccessFile.seek(offset);
                randomAccessFile.read(byteArray);
                return ByteBuffer.wrap(byteArray);
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                return null;
            }
        });
    }


}
