package com.v7878.jnasm.x86;

public enum X86ByteRegister {
    AL(0),
    CL(1),
    DL(2),
    BL(3),
    AH(4),
    CH(5),
    DH(6),
    BH(7);

    private final int value;

    X86ByteRegister(int value) {
        this.value = value;
    }

    public static X86ByteRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
