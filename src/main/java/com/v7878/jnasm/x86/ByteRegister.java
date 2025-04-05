package com.v7878.jnasm.x86;

public enum ByteRegister {
    AL(0),
    CL(1),
    DL(2),
    BL(3),
    AH(4),
    CH(5),
    DH(6),
    BH(7);

    private final int value;

    ByteRegister(int value) {
        this.value = value;
    }

    public int index() {
        return value;
    }

    public static String toString(ByteRegister reg) {
        return "ByteRegister[" + reg.index() + "]";
    }
}
