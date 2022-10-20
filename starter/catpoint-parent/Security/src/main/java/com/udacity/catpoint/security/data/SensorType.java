package com.udacity.catpoint.security.data;

import java.util.Random;

/**
 * List of available sensor types. Not currently used by system, other than for display.
 */
public enum SensorType {
    DOOR, WINDOW, MOTION;

    private static final Random random = new Random();

    public static SensorType randomSensorType(){
        SensorType[] sensorTypes = values();
        return sensorTypes[random.nextInt(sensorTypes.length)];
    }
}
