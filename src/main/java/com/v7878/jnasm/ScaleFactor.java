package com.v7878.jnasm;

public enum ScaleFactor {
    TIMES_1(0),
    TIMES_2(1),
    TIMES_4(2),
    TIMES_8(3);

    private final int value;

    ScaleFactor(int value) {
        this.value = value;
    }

    public static ScaleFactor of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }

    public int value() {
        return 1 << value;
    }
}
