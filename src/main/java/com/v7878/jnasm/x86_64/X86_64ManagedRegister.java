package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.x86_64.CpuRegister.kNumberOfCpuRegisters;
import static com.v7878.jnasm.x86_64.X87Register.kNumberOfX87Registers;
import static com.v7878.jnasm.x86_64.XmmRegister.kNumberOfXmmRegisters;

import com.v7878.jnasm.ManagedRegister;

// An instance of class 'ManagedRegister' represents a single cpu register
// (enum CpuRegister), or xmm register (enum XmmRegister).
// 'ManagedRegister::NoRegister()' provides an invalid register.
// There is a one-to-one mapping between ManagedRegister and register id.
public class X86_64ManagedRegister extends ManagedRegister {
    // ids map:
    //   [0..R[  cpu registers (enum CpuRegister)
    //   [R..X[  xmm registers (enum XmmRegister)
    //   [X..S[  x87 registers (enum X87Register)
    // where
    //   R = kNumberOfCpuRegIds
    //   X = R + kNumberOfXmmRegIds
    //   S = X + kNumberOfX87RegIds

    public static final int kNumberOfCpuRegIds = kNumberOfCpuRegisters;
    public static final int kNumberOfXmmRegIds = kNumberOfXmmRegisters;
    public static final int kNumberOfX87RegIds = kNumberOfX87Registers;
    public static final int kNumberOfRegIds = kNumberOfCpuRegIds + kNumberOfXmmRegIds + kNumberOfX87RegIds;

    private X86_64ManagedRegister(int regId) {
        super(regId);
    }

    private X86_64ManagedRegister() {
        super();
    }

    public static X86_64ManagedRegister NoRegister() {
        return new X86_64ManagedRegister();
    }

    public static X86_64ManagedRegister fromCpuRegister(CpuRegister r) {
        return new X86_64ManagedRegister(r.index());
    }

    public static X86_64ManagedRegister fromXmmRegister(XmmRegister r) {
        return new X86_64ManagedRegister(r.index() + kNumberOfCpuRegIds);
    }

    public static X86_64ManagedRegister fromX87Register(X87Register r) {
        return new X86_64ManagedRegister(r.index() + kNumberOfCpuRegIds + kNumberOfXmmRegIds);
    }

    public boolean isCpuRegister() {
        return 0 <= id && id < kNumberOfCpuRegIds;
    }

    public boolean isXmmRegister() {
        int test = id - kNumberOfCpuRegIds;
        return 0 <= test && test < kNumberOfXmmRegIds;
    }

    public boolean isX87Register() {
        int test = id - (kNumberOfCpuRegIds + kNumberOfXmmRegIds);
        return 0 <= test && test < kNumberOfX87RegIds;
    }

    public CpuRegister asCpuRegister() {
        return CpuRegister.of(id);
    }

    public XmmRegister asXmmRegister() {
        return XmmRegister.of(id - kNumberOfCpuRegIds);
    }

    public X87Register asX87Register() {
        return X87Register.of(id - (kNumberOfCpuRegIds + kNumberOfXmmRegIds));
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
        return "???: " + id;
    }
}
