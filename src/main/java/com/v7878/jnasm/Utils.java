package com.v7878.jnasm;

public class Utils {
    public static boolean isInt8(int value) {
        return value >= -128 && value <= 127;
    }

    public static boolean isUInt8(int value) {
        return value >= 0 && value <= 255;
    }

    public static boolean isInt16(int value) {
        return value >= -32768 && value <= 32767;
    }

    public static boolean isUInt16(int value) {
        return value >= 0 && value <= 65535;
    }
}
