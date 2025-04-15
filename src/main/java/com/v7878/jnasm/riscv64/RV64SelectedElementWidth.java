package com.v7878.jnasm.riscv64;

public enum RV64SelectedElementWidth {
    kE8(0b000),
    kE16(0b001),
    kE32(0b010),
    kE64(0b011),
    kReserved1(0b100),
    kReserved2(0b101),
    kReserved3(0b110),
    kReserved4(0b111);

    private final int value;

    RV64SelectedElementWidth(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
