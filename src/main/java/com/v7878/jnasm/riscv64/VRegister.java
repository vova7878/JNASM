package com.v7878.jnasm.riscv64;

public enum VRegister {
    V0(0), // argument 0
    V1(1), // callee-saved 0
    V2(2), // callee-saved 1
    V3(3), // callee-saved 2
    V4(4), // callee-saved 3
    V5(5), // callee-saved 4
    V6(6), // callee-saved 5
    V7(7), // callee-saved 6

    V8(8), // argument 1
    V9(9), // argument 2
    V10(10), // argument 3
    V11(11), // argument 4
    V12(12), // argument 5
    V13(13), // argument 6
    V14(14), // argument 7
    V15(15), // argument 8

    V16(16), // argument 9
    V17(17), // argument 10
    V18(18), // argument 11
    V19(19), // argument 12
    V20(20), // argument 13
    V21(21), // argument 14
    V22(22), // argument 15
    V23(23), // argument 16

    V24(24),// callee-saved 7
    V25(25),// callee-saved 8
    V26(26),// callee-saved 9
    V27(27),// callee-saved 10
    V28(28),// callee-saved 11
    V29(29),// callee-saved 12
    V30(30),// callee-saved 13
    V31(31);// callee-saved 14

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
