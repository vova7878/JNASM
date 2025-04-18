package com.v7878.jnasm.riscv64;

public enum RV64AqRl {
    kNone(0x0),
    kRelease(0x1),
    kAcquire(0x2),
    kAqRl(kRelease.value() | kAcquire.value());

    private final int value;

    RV64AqRl(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
