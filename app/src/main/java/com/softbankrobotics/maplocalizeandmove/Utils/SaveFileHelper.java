package com.softbankrobotics.maplocalizeandmove.Utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class SaveFileHelper {

    private static String TAG = "MSI_SaveFileHelper";

    public Map<String, Vector2> getLocationsFromFile(Context applicationContext) {
        Map<String, Vector2> vectors = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(new File(applicationContext.getFilesDir(), "hashmap.ser"));
            ois = new ObjectInputStream(fis);
            vectors = (HashMap) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            //TODO
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

    public void saveLocationsToFile(Context applicationContext, Map<String, Vector2> locationsToBackup) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        // Backup list into a file
        try {
            fos = new FileOutputStream(new File(applicationContext.getFilesDir(), "hashmap.ser"));
            oos = new ObjectOutputStream(fos);
            oos.writeObject(locationsToBackup);
            Log.d(TAG, "backupLocations: Done");
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            //TODO
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

    public void writeStringToFile(Context applicationContext, String data, String fileName) {
        OutputStreamWriter outputStreamWriter = null;
        try {
            Log.d(TAG, "writeMapDataToFile: started");
            outputStreamWriter = new OutputStreamWriter(applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
        } catch (IOException e) {
            Log.d("Exception", "File write failed: " + e.getMessage(), e);
        } finally {
            try {
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
            } catch (IOException e) {

                Log.e(TAG, e.getMessage(), e);
            }
        }
        Log.d(TAG, "writeMapDataToFile:  Finished");
    }

    public String readStringFromFile(Context applicationContext, String fileName) {

        String ret = "";
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader;
        InputStream inputStream = null;

        try {
            inputStream = applicationContext.openFileInput(fileName);

            if (inputStream != null) {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage(), e);
            return "File not found";
        } catch (IOException e) {
            Log.d(TAG, "Can not read file: " + e.getMessage(), e);
            return "Can not read file";
        } finally {
            //TODO
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }

        }
        return ret;
    }

}
