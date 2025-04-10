package com.v7878.jnasm;

public class Utils {
    public static void CHECK(boolean value) {
        if (!value) {
            // TODO: message
            throw new AssertionError();
        }
    }

    public static void CHECK_EQ(int a, int b) {
        CHECK(a == b);
    }

    public static void CHECK_NE(int a, int b) {
        CHECK(a != b);
    }

    public static void CHECK_LT(int a, int b) {
        CHECK(a < b);
    }

    public static void CHECK_LT(long a, long b) {
        CHECK(a < b);
    }

    public static void CHECK_LE(int a, int b) {
        CHECK(a <= b);
    }

    public static void CHECK_GT(int a, int b) {
        CHECK(a > b);
    }

    public static void CHECK_GE(int a, int b) {
        CHECK(a >= b);
    }

    public static void CHECK_IMPLIES(boolean a, boolean b) {
        CHECK(!a || b);
    }

    public static void CHECK_ALIGNED(int value, int alignment) {
        CHECK(isAligned(value, alignment));
    }

    public static boolean isUInt(int width, int value) {
        assert width > 0 : "width must be > 0";
        assert width <= 32 : "width must be <= max (32)";
        if (width >= 32) return value >= 0;
        return value >>> width == 0;
    }

    public static boolean isInt(int width, int value) {
        assert width > 0 : "width must be > 0";
        assert width <= 32 : "width must be <= max (32)";
        if (width >= 32) return true;
        value = value >> width;
        return value == 0 || value == -1;
    }

    public static boolean isUInt1(int value) {
        return isUInt(1, value);
    }

    public static boolean isUInt2(int value) {
        return isUInt(2, value);
    }

    public static boolean isUInt3(int value) {
        return isUInt(3, value);
    }

    public static boolean isUInt4(int value) {
        return isUInt(4, value);
    }

    public static boolean isUInt5(int value) {
        return isUInt(5, value);
    }

    public static boolean isInt6(int value) {
        return isInt(6, value);
    }

    public static boolean isUInt6(int value) {
        return isUInt(6, value);
    }

    public static boolean isUInt7(int value) {
        return isUInt(7, value);
    }

    public static boolean isInt8(int value) {
        return isInt(8, value);
    }

    public static boolean isUInt8(int value) {
        return isUInt(8, value);
    }

    public static boolean isInt9(int value) {
        return isInt(9, value);
    }

    public static boolean isUInt9(int value) {
        return isUInt(9, value);
    }

    public static boolean isInt10(int value) {
        return isInt(10, value);
    }

    public static boolean isUInt10(int value) {
        return isUInt(10, value);
    }

    public static boolean isUInt11(int value) {
        return isUInt(11, value);
    }

    public static boolean isInt12(int value) {
        return isInt(12, value);
    }

    public static boolean isUInt12(int value) {
        return isUInt(12, value);
    }

    public static boolean isInt13(int value) {
        return isInt(13, value);
    }

    public static boolean isInt16(int value) {
        return isInt(16, value);
    }

    public static boolean isUInt16(int value) {
        return isUInt(16, value);
    }

    public static boolean isUInt20(int value) {
        return isUInt(20, value);
    }

    public static boolean isInt21(int value) {
        return isInt(21, value);
    }

    public static boolean isAligned(int value, int alignment) {
        // TODO: assert isPowerOfTwo(alignment);
        return (value & (alignment - 1)) == 0;
    }

    public static boolean isAligned2(int value) {
        return isAligned(value, 2);
    }

    public static boolean isAligned4(int value) {
        return isAligned(value, 4);
    }

    public static boolean isAligned8(int value) {
        return isAligned(value, 8);
    }

    public static boolean isAligned16(int value) {
        return isAligned(value, 16);
    }

    public static boolean isAligned64(int value) {
        return isAligned(value, 64);
    }

    public static boolean isLUInt(int width, long value) {
        assert width > 0 : "width must be > 0";
        assert width <= 64 : "width must be <= max (64)";
        if (width >= 64) return value >= 0;
        return value >>> width == 0;
    }

    public static boolean isLInt(int width, long value) {
        assert width > 0 : "width must be > 0";
        assert width <= 64 : "width must be <= max (64)";
        if (width >= 64) return true;
        value = value >> width;
        return value == 0 || value == -1;
    }

    public static boolean isInt8(long value) {
        return isLInt(8, value);
    }

    public static boolean isUInt8(long value) {
        return isLUInt(8, value);
    }

    public static boolean isInt16(long value) {
        return isLInt(16, value);
    }

    public static boolean isUInt16(long value) {
        return isLUInt(16, value);
    }

    public static boolean isInt32(long value) {
        return isLInt(32, value);
    }

    public static boolean isUInt32(long value) {
        return isLUInt(32, value);
    }
}
