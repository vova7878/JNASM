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

    public static boolean isAligned(int value, int alignment) {
        // TODO: assert isPowerOfTwo(alignment);
        return (value & (alignment - 1)) == 0;
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

}
