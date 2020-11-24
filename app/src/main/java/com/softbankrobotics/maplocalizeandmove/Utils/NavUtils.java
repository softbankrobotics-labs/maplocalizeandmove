package com.softbankrobotics.maplocalizeandmove.Utils;

import com.aldebaran.qi.sdk.object.geometry.Quaternion;

import static java.lang.Math.atan2;

class NavUtils {

    private static final String TAG = "MSI_MapLocalizeAndMove";

    /**
     * Get the "yaw" (or "theta") angle from a quaternion (the only angle relevant for navigation).
     */
    static double getYawFromQuaternion(Quaternion q) {
        // yaw (z-axis rotation)
        double x = q.getX();
        double y = q.getY();
        double z = q.getZ();
        double w = q.getW();
        double sinYaw = 2.0 * (w * z + x * y);
        double cosYaw = 1.0 - 2.0 * (y * y + z * z);
        return atan2(sinYaw, cosYaw);
    }
}
