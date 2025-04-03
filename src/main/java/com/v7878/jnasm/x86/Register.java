package com.v7878.jnasm.x86;

public enum Register {
    EAX(0),
    ECX(1),
    EDX(2),
    EBX(3),
    ESP(4),
    EBP(5),
    ESI(6),
    EDI(7);

    public static final int kNumberOfCpuRegisters = 8;

    //TODO?
    //kFirstByteUnsafeRegister(4)

    private final int value;

    Register(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
