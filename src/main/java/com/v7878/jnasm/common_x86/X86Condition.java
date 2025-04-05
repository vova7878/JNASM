package com.v7878.jnasm.common_x86;

public enum X86Condition {
    kOverflow(0),
    kNoOverflow(1),
    kBelow(2),
    kAboveEqual(3),
    kEqual(4),
    kNotEqual(5),
    kBelowEqual(6),
    kAbove(7),
    kSign(8),
    kNotSign(9),
    kParityEven(10),
    kParityOdd(11),
    kLess(12),
    kGreaterEqual(13),
    kLessEqual(14),
    kGreater(15);

    public static final X86Condition kZero = kEqual;
    public static final X86Condition kNotZero = kNotEqual;
    public static final X86Condition kNegative = kSign;
    public static final X86Condition kPositive = kNotSign;
    public static final X86Condition kCarrySet = kBelow;
    public static final X86Condition kCarryClear = kAboveEqual;
    public static final X86Condition kUnordered = kParityEven;

    private final int value;

    X86Condition(int value) {
        this.value = value;
    }

    public static X86Condition of(int index) {
        return values()[index];
    }

    public int index() {
        return value;
    }
}
