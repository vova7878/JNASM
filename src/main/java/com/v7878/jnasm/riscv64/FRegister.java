package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.Utils.isUInt3;

public enum FRegister {
    F0(0),
    F1(1),
    F2(2),
    F3(3),
    F4(4),
    F5(5),
    F6(6),
    F7(7),
    F8(8),
    F9(9),
    F10(10),
    F11(11),
    F12(12),
    F13(13),
    F14(14),
    F15(15),
    F16(16),
    F17(17),
    F18(18),
    F19(19),
    F20(20),
    F21(21),
    F22(22),
    F23(23),
    F24(24),
    F25(25),
    F26(26),
    F27(27),
    F28(28),
    F29(29),
    F30(30),
    F31(31);

    public static final int kNumberOfFRegisters = 32;

    // Aliases.
    public static final FRegister FT0 = F0; // temporary 0
    public static final FRegister FT1 = F1; // temporary 1
    public static final FRegister FT2 = F2; // temporary 2
    public static final FRegister FT3 = F3; // temporary 3
    public static final FRegister FT4 = F4; // temporary 4
    public static final FRegister FT5 = F5; // temporary 5
    public static final FRegister FT6 = F6; // temporary 6
    public static final FRegister FT7 = F7; // temporary 7

    public static final FRegister FS0 = F8; // callee-saved 0
    public static final FRegister FS1 = F9; // callee-saved 1

    public static final FRegister FA0 = F10; // argument 0 / return value 0
    public static final FRegister FA1 = F11; // argument 1 / return value 1
    public static final FRegister FA2 = F12; // argument 2
    public static final FRegister FA3 = F13; // argument 3
    public static final FRegister FA4 = F14; // argument 4
    public static final FRegister FA5 = F15; // argument 5
    public static final FRegister FA6 = F16; // argument 6
    public static final FRegister FA7 = F17; // argument 7

    public static final FRegister FS2 = F18; // callee-saved 2
    public static final FRegister FS3 = F19; // callee-saved 3
    public static final FRegister FS4 = F20; // callee-saved 4
    public static final FRegister FS5 = F21; // callee-saved 5
    public static final FRegister FS6 = F22; // callee-saved 6
    public static final FRegister FS7 = F23; // callee-saved 7
    public static final FRegister FS8 = F24; // callee-saved 8
    public static final FRegister FS9 = F25; // callee-saved 9
    public static final FRegister FS10 = F26; // callee-saved 10
    public static final FRegister FS11 = F27; // callee-saved 11

    public static final FRegister FT8 = F28; // temporary 8
    public static final FRegister FT9 = F29; // temporary 9
    public static final FRegister FT10 = F30; // temporary 10
    public static final FRegister FT11 = F31; // temporary 11

    public static final FRegister FTMP = FT11; // Reserved for special uses, such as assembler macro instructions.

    private final int value;

    FRegister(int value) {
        this.value = value;
    }

    public static FRegister of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }

    public boolean isShortReg() {
        return isUInt3(index() - 8);
    }
}
