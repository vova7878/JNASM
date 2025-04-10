package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.Utils.isUInt3;

public enum XRegister {
    X0(0),
    X1(1),
    X2(2),
    X3(3),
    X4(4),
    X5(5),
    X6(6),
    X7(7),
    X8(8),
    X9(9),
    X10(10),
    X11(11),
    X12(12),
    X13(13),
    X14(14),
    X15(15),
    X16(16),
    X17(17),
    X18(18),
    X19(19),
    X20(20),
    X21(21),
    X22(22),
    X23(23),
    X24(24),
    X25(25),
    X26(26),
    X27(27),
    X28(28),
    X29(29),
    X30(30),
    X31(31);

    public static final int kNumberOfXRegisters = 32;

    // Aliases.
    public static final XRegister Zero = X0; // hard-wired zero
    public static final XRegister RA = X1; // return address
    public static final XRegister SP = X2; // stack pointer
    public static final XRegister GP = X3; // global pointer (unavailable, used for shadow stack by the compiler / libc)
    public static final XRegister TP = X4; // thread pointer (points to TLS area, not ART-internal thread)

    public static final XRegister T0 = X5; // temporary 0
    public static final XRegister T1 = X6; // temporary 1
    public static final XRegister T2 = X7; // temporary 2

    public static final XRegister S0 = X8; // callee-saved 0
    public static final XRegister S1 = X9; // callee-saved 1

    public static final XRegister A0 = X10; // argument 0 / return value 0
    public static final XRegister A1 = X11; // argument 1 / return value 1
    public static final XRegister A2 = X12; // argument 2
    public static final XRegister A3 = X13; // argument 3
    public static final XRegister A4 = X14; // argument 4
    public static final XRegister A5 = X15; // argument 5
    public static final XRegister A6 = X16; // argument 6
    public static final XRegister A7 = X17; // argument 7

    public static final XRegister S2 = X18; // callee-saved 2
    public static final XRegister S3 = X19; // callee-saved 3
    public static final XRegister S4 = X20; // callee-saved 4
    public static final XRegister S5 = X21; // callee-saved 5
    public static final XRegister S6 = X22; // callee-saved 6
    public static final XRegister S7 = X23; // callee-saved 7
    public static final XRegister S8 = X24; // callee-saved 8
    public static final XRegister S9 = X25; // callee-saved 9
    public static final XRegister S10 = X26; // callee-saved 10
    public static final XRegister S11 = X27; // callee-saved 11

    public static final XRegister T3 = X28; // temporary 3
    public static final XRegister T4 = X29; // temporary 4
    public static final XRegister T5 = X30; // temporary 5
    public static final XRegister T6 = X31; // temporary 6

    public static final XRegister FP = S0; // frame pointer
    public static final XRegister TR = S1; // ART Thread Register - managed runtime
    public static final XRegister TMP = T6; // Reserved for special uses, such as assembler macro instructions.
    public static final XRegister TMP2 = T5; // Reserved for special uses, such as assembler macro instructions.

    private final int value;

    XRegister(int value) {
        this.value = value;
    }

    public static XRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }

    public boolean isShortReg() {
        return isUInt3(index() - 8);
    }
}
