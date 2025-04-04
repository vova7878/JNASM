package com.v7878.jnasm.x86;

import static com.v7878.jnasm.x86.CpuRegister.kNumberOfCpuRegisters;
import static com.v7878.jnasm.x86.X87Register.kNumberOfX87Registers;
import static com.v7878.jnasm.x86.XmmRegister.kNumberOfXmmRegisters;

import com.v7878.jnasm.ManagedRegister;

// An instance of class 'ManagedRegister' represents a single cpu register
// (enum CpuRegister), or xmm register (enum XmmRegister).
// 'ManagedRegister::NoRegister()' provides an invalid register.
// There is a one-to-one mapping between ManagedRegister and register id.
public class X86ManagedRegister extends ManagedRegister {
    // ids map:
    //   [0..R[  cpu registers (enum CpuRegister)
    //   [R..X[  xmm registers (enum XmmRegister)
    //   [X..S[  x87 registers (enum X87Register)
    // where
    //   R = kNumberOfCpuRegIds
    //   X = R + kNumberOfXmmRegIds
    //   S = X + kNumberOfX87RegIds

    public static final int kNumberOfCpuRegIds = kNumberOfCpuRegisters;
    public static final int kNumberOfCpuAllocIds = kNumberOfCpuRegisters;

    public static final int kNumberOfXmmRegIds = kNumberOfXmmRegisters;
    public static final int kNumberOfXmmAllocIds = kNumberOfXmmRegisters;

    public static final int kNumberOfX87RegIds = kNumberOfX87Registers;
    public static final int kNumberOfX87AllocIds = kNumberOfX87Registers;

    public static final int kNumberOfRegIds = kNumberOfCpuRegIds + kNumberOfXmmRegIds + kNumberOfX87RegIds;
    public static final int kNumberOfAllocIds = kNumberOfCpuAllocIds + kNumberOfXmmAllocIds + kNumberOfX87RegIds;

    private X86ManagedRegister(int regId) {
        super(regId);
    }

    private X86ManagedRegister() {
        super();
    }

    public static X86ManagedRegister NoRegister() {
        return new X86ManagedRegister();
    }

    public static X86ManagedRegister fromCpuRegister(CpuRegister r) {
        return new X86ManagedRegister(r.ordinal());
    }

    public static X86ManagedRegister fromXmmRegister(XmmRegister r) {
        return new X86ManagedRegister(r.ordinal() + kNumberOfCpuRegIds);
    }

    public static X86ManagedRegister fromX87Register(X87Register r) {
        return new X86ManagedRegister(r.ordinal() + kNumberOfCpuRegIds + kNumberOfXmmRegIds);
    }

    public boolean isCpuRegister() {
        return 0 <= id_ && id_ < kNumberOfCpuRegIds;
    }

    public boolean isXmmRegister() {
        return 0 <= id_ - kNumberOfCpuRegIds && id_ - kNumberOfCpuRegIds < kNumberOfXmmRegIds;
    }

    public boolean isX87Register() {
        return 0 <= id_ - (kNumberOfCpuRegIds + kNumberOfXmmRegIds) && id_ - (kNumberOfCpuRegIds + kNumberOfXmmRegIds) < kNumberOfX87RegIds;
    }

    public CpuRegister asCpuRegister() {
        return CpuRegister.values()[id_];
    }

    public XmmRegister asXmmRegister() {
        return XmmRegister.values()[id_ - kNumberOfCpuRegIds];
    }

    public X87Register asX87Register() {
        return X87Register.values()[id_ - (kNumberOfCpuRegIds + kNumberOfXmmRegIds)];
    }

    @Override
    public String toString() {
        if (isNoRegister()) {
            return "No Register";
        } else if (isCpuRegister()) {
            return "CPU: " + asCpuRegister();
        } else if (isXmmRegister()) {
            return "XMM: " + asXmmRegister();
        } else if (isX87Register()) {
            return "X87: " + asX87Register();
        }
        return "???: " + id_;
    }
}
