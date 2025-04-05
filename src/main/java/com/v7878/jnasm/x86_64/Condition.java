package com.v7878.jnasm.x86_64;

public enum Condition {
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

    public static final Condition kZero = kEqual;
    public static final Condition kNotZero = kNotEqual;
    public static final Condition kNegative = kSign;
    public static final Condition kPositive = kNotSign;
    public static final Condition kCarrySet = kBelow;
    public static final Condition kCarryClear = kAboveEqual;
    public static final Condition kUnordered = kParityEven;

    private final int value;

    Condition(int value) {
        this.value = value;
    }

    public int index() {
        return value;
    }
}
