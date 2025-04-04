package com.v7878.jnasm.x86_64;

public enum XmmRegister {
    XMM0(0),
    XMM1(1),
    XMM2(2),
    XMM3(3),
    XMM4(4),
    XMM5(5),
    XMM6(6),
    XMM7(7),
    XMM8(8),
    XMM9(9),
    XMM10(10),
    XMM11(11),
    XMM12(12),
    XMM13(13),
    XMM14(14),
    XMM15(15);

    public static final int kNumberOfXmmRegisters = 16;

    private final int value;

    XmmRegister(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int lowBits() {
        return value & 7;
    }

    public boolean needsRex() {
        return value > 7;
    }
}
