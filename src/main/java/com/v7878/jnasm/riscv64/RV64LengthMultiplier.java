package com.v7878.jnasm.riscv64;

public enum RV64LengthMultiplier {
    kM1Over8(0b101),
    kM1Over4(0b110),
    kM1Over2(0b111),
    kM1(0b000),
    kM2(0b001),
    kM4(0b010),
    kM8(0b011),
    kReserved1(0b100);

    private final int value;

    RV64LengthMultiplier(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
