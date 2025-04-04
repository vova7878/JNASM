package com.v7878.jnasm.x86_64;

public enum CpuRegister {
    RAX(0),
    RCX(1),
    RDX(2),
    RBX(3),
    RSP(4),
    RBP(5),
    RSI(6),
    RDI(7),
    R8(8),
    R9(9),
    R10(10),
    R11(11),
    R12(12),
    R13(13),
    R14(14),
    R15(15);

    public static final int kNumberOfCpuRegisters = 16;

    private final int value;

    CpuRegister(int value) {
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
