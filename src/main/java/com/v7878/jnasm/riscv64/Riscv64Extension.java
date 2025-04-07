package com.v7878.jnasm.riscv64;

public enum Riscv64Extension {
    // Pseudo-extension encompassing all loads and stores. Used to check that
    // we do not have loads and stores in the middle of a LR/SC sequence.
    kLoadStore(0),
    kZifencei(1),
    kM(2),
    kA(3),
    kZicsr(4),
    kF(5),
    kD(6),
    kZba(7),
    kZbb(8),
    kZbs(9),
    kV(10),
    // "C" extension instructions except floating point loads/stores.
    kZca(11),
    // "C" extension double loads/stores.
    kZcd(12),
    // Note: RV64 cannot implement Zcf ("C" extension float loads/stores).
    // Simple 16-bit operations not present in the original "C" extension.
    kZcb(13);

    public static final int kRiscv64AllExtensionsMask = (1 << (kZcb.index() + 1)) - 1;

    private final int value;

    Riscv64Extension(int value) {
        this.value = value;
    }

    public static Riscv64Extension of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }

    public int extensionBit() {
        return 1 << value;
    }
}
