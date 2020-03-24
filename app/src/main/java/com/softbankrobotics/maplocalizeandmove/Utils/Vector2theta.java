package com.softbankrobotics.maplocalizeandmove.Utils;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.geometry.Quaternion;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;

import java.io.Serializable;

public class Vector2theta implements Parcelable, Serializable {
    public static final Creator<Vector2theta> CREATOR = new Creator<Vector2theta>() {
        @Override
        public Vector2theta createFromParcel(Parcel in) {
            return new Vector2theta(in);
        }

        @Override
        public Vector2theta[] newArray(int size) {
            return new Vector2theta[size];
        }
    };
    private double x, y, theta;

    private Vector2theta(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    /***************** PARCELABLE REQUIREMENTS *******************/

    private Vector2theta(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
        theta = in.readDouble();
    }

    /**
     * creates a Vector2theta representing the translation between two frames and angle
     *
     * @param frameOrigin      the origin of the translation
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
     *
     * @return the transform
     */
    public Transform createTransform() {
        // this.theta is the radian angle to appy taht was serialized
        return TransformBuilder.create().from2DTransform(this.x, this.y, this.theta);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(x);
        parcel.writeDouble(y);
        parcel.writeDouble(theta);
    }
}
