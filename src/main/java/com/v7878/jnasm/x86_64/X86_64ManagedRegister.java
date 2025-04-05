package com.v7878.jnasm.x86_64;

import static com.v7878.jnasm.common_x86.X87Register.kNumberOfX87Registers;
import static com.v7878.jnasm.x86_64.X86_64CpuRegister.kNumberOfCpuRegisters;
import static com.v7878.jnasm.x86_64.X86_64XmmRegister.kNumberOfXmmRegisters;

import com.v7878.jnasm.ManagedRegister;
import com.v7878.jnasm.common_x86.X87Register;

// An instance of class 'ManagedRegister' represents a single cpu register
// (enum X86_64CpuRegister), or xmm register (enum X86_64XmmRegister).
// 'ManagedRegister::NoRegister()' provides an invalid register.
// There is a one-to-one mapping between ManagedRegister and register id.
public class X86_64ManagedRegister extends ManagedRegister {
    // ids map:
    //   [0..R[  cpu registers (enum X86_64CpuRegister)
    //   [R..X[  xmm registers (enum X86_64XmmRegister)
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

    public static X86_64ManagedRegister fromCpuRegister(X86_64CpuRegister r) {
        return new X86_64ManagedRegister(r.index());
    }

    public static X86_64ManagedRegister fromXmmRegister(X86_64XmmRegister r) {
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

    public X86_64CpuRegister asCpuRegister() {
        return X86_64CpuRegister.of(id);
    }

    public X86_64XmmRegister asXmmRegister() {
        return X86_64XmmRegister.of(id - kNumberOfCpuRegIds);
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
