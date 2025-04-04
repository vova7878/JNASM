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

    public static boolean isInt8(long value) {
        return value >= -128 && value <= 127;
    }

    public static boolean isUInt8(long value) {
        return value >= 0 && value <= 255;
    }

    public static boolean isInt16(long value) {
        return value >= -32768 && value <= 32767;
    }

    public static boolean isUInt16(long value) {
        return value >= 0 && value <= 65535;
    }

    public static boolean isInt32(long value) {
        return value >= -2147483648 && value <= 2147483647;
    }

    public static boolean isUInt32(long value) {
        return value >= 0 && value <= 4294967295L;
    }
}
