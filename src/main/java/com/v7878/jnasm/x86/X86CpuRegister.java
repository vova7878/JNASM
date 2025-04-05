package com.v7878.jnasm.x86;

public enum X86CpuRegister {
    EAX(0),
    ECX(1),
    EDX(2),
    EBX(3),
    ESP(4),
    EBP(5),
    ESI(6),
    EDI(7);

    public static final int kNumberOfCpuRegisters = 8;
    public static final int kFirstByteUnsafeRegister = 4;

    private final int value;

    X86CpuRegister(int value) {
        this.value = value;
    }

    public static X86CpuRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
