package com.v7878.jnasm.riscv64;

public enum FenceType {
    // bitmask of
    // kFenceWrite = 0x1
    // kFenceRead = 0x2
    // kFenceOutput = 0x4
    // kFenceInput = 0x8

    // TODO: rename?
    kFenceNNNN(0b0000),
    kFenceNNNW(0b0001),
    kFenceNNRN(0b0010),
    kFenceNNRW(0b0011),
    kFenceNONN(0b0100),
    kFenceNONW(0b0101),
    kFenceNORN(0b0110),
    kFenceNORW(0b0111),
    kFenceINNN(0b1000),
    kFenceINNW(0b1001),
    kFenceINRN(0b1010),
    kFenceINRW(0b1011),
    kFenceIONN(0b1100),
    kFenceIONW(0b1101),
    kFenceIORN(0b1110),
    kFenceIORW(0b1111);

    public static final FenceType kFenceDefault = kFenceIORW;

    private final int value;

    FenceType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
