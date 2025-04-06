package com.v7878.jnasm.riscv64;

public enum VRegister {
    X0(0), // argument 0
    X1(1), // callee-saved 0
    X2(2), // callee-saved 1
    X3(3), // callee-saved 2
    X4(4), // callee-saved 3
    X5(5), // callee-saved 4
    X6(6), // callee-saved 5
    X7(7), // callee-saved 6

    X8(8), // argument 1
    X9(9), // argument 2
    X10(10), // argument 3
    X11(11), // argument 4
    X12(12), // argument 5
    X13(13), // argument 6
    X14(14), // argument 7
    X15(15), // argument 8

    X16(16), // argument 9
    X17(17), // argument 10
    X18(18), // argument 11
    X19(19), // argument 12
    X20(20), // argument 13
    X21(21), // argument 14
    X22(22), // argument 15
    X23(23), // argument 16

    X24(24),// callee-saved 7
    X25(25),// callee-saved 8
    X26(26),// callee-saved 9
    X27(27),// callee-saved 10
    X28(28),// callee-saved 11
    X29(29),// callee-saved 12
    X30(30),// callee-saved 13
    X31(31);// callee-saved 14

    public static final int kNumberOfVRegisters = 32;

    private final int value;

    VRegister(int value) {
        this.value = value;
    }

    public static VRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
