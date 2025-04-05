package com.v7878.jnasm.x86;

public enum XmmRegister {
    XMM0(0),
    XMM1(1),
    XMM2(2),
    XMM3(3),
    XMM4(4),
    XMM5(5),
    XMM6(6),
    XMM7(7);

    public static final int kNumberOfXmmRegisters = 8;

    private final int value;

    XmmRegister(int value) {
        this.value = value;
    }

    public static XmmRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
