package com.v7878.jnasm.common_x86;

public enum X87Register {
    ST0(0),
    ST1(1),
    ST2(2),
    ST3(3),
    ST4(4),
    ST5(5),
    ST6(6),
    ST7(7);

    public static final int kNumberOfX87Registers = 8;

    private final int value;

    X87Register(int value) {
        this.value = value;
    }

    public static X87Register of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
