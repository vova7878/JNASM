package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.riscv64.FRegister.kNumberOfFRegisters;
import static com.v7878.jnasm.riscv64.XRegister.kNumberOfXRegisters;

import com.v7878.jnasm.ManagedRegister;

// An instance of class 'ManagedRegister' represents a single Riscv64 register.
// A register can be one of the following:
//  * core register (enum XRegister)
//  * floating-point register (enum FRegister)
// TODO?: * vector register (enum VRegister)
//
// 'ManagedRegister::NoRegister()' provides an invalid register.
// There is a one-to-one mapping between ManagedRegister and register id.
public class RV64ManagedRegister extends ManagedRegister {
    // Register ids map:
    //   [0..R[  core registers (enum XRegister)
    //   [R..F[  floating-point registers (enum FRegister)
    // where
    //   R = kNumberOfXRegIds
    //   F = R + kNumberOfFRegIds

    public static final int kNumberOfXRegIds = kNumberOfXRegisters;
    public static final int kNumberOfFRegIds = kNumberOfFRegisters;
    public static final int kNumberOfRegIds = kNumberOfXRegIds + kNumberOfFRegIds;

    private RV64ManagedRegister(int regId) {
        super(regId);
    }

    private RV64ManagedRegister() {
        super();
    }

    public static RV64ManagedRegister NoRegister() {
        return new RV64ManagedRegister();
    }

    public static RV64ManagedRegister fromXRegister(XRegister r) {
        return new RV64ManagedRegister(r.index());
    }

    public static RV64ManagedRegister fromFRegister(FRegister r) {
        return new RV64ManagedRegister(r.index() + kNumberOfXRegIds);
    }

    public boolean isXRegister() {
        return 0 <= id && id < kNumberOfXRegIds;
    }

    public boolean isFRegister() {
        int test = id - kNumberOfXRegIds;
        return 0 <= test && test < kNumberOfFRegisters;
    }

    public XRegister asXRegister() {
        return XRegister.of(id);
    }

    public FRegister asFRegister() {
        return FRegister.of(id - kNumberOfXRegIds);
    }

    @Override
    public String toString() {
        if (isNoRegister()) {
            return "No Register";
        } else if (isXRegister()) {
            return "XRegister: " + asXRegister();
        } else if (isFRegister()) {
            return "FRegister: " + asFRegister();
        }
        return "???: " + id;
    }
}
