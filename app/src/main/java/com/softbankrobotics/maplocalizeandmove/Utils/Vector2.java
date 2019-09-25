package com.softbankrobotics.maplocalizeandmove.Utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;

import java.io.Serializable;

public class Vector2 implements Parcelable, Serializable {
    private double x, y;

    private Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * creates a Vector2 representing the translation between two frames
     * @param frameOrigin the origin of the translation
     * @param frameDestination the end of the translation
     * @return the Vector2 to go from frameOrigin to frameDestination
     */
    public static Vector2 betweenFrames(@NonNull Frame frameDestination, @NonNull Frame frameOrigin) {
        // Compute the transform to go from "frameOrigin" to "frameDestination"
        Transform transform = frameOrigin.async().computeTransform(frameDestination).getValue().getTransform();

        // Extract translation from the transform
        Vector3 translation = transform.getTranslation();

        // Extract the 2 coordinates from the translation
        return new Vector2(translation.getX(), translation.getY());
    }

    /**
     * Returns a transform representing the translation described by this Vector2
     * @return the transform
     */
    public Transform createTransform() {
        return TransformBuilder.create().from2DTranslation(this.x, this.y);
    }

    /***************** PARCELABLE REQUIREMENTS *******************/

    private Vector2(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
    }

    public static final Creator<Vector2> CREATOR = new Creator<Vector2>() {
        @Override
        public Vector2 createFromParcel(Parcel in) {
            return new Vector2(in);
        }

        @Override
        public Vector2[] newArray(int size) {
            return new Vector2[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(x);
        parcel.writeDouble(y);
    }
}
