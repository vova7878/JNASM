package com.v7878.jnasm.x86;

import static com.v7878.jnasm.common_x86.X87Register.kNumberOfX87Registers;
import static com.v7878.jnasm.x86.X86CpuRegister.kNumberOfCpuRegisters;
import static com.v7878.jnasm.x86.X86XmmRegister.kNumberOfXmmRegisters;

import com.v7878.jnasm.ManagedRegister;
import com.v7878.jnasm.common_x86.X87Register;

// An instance of class 'ManagedRegister' represents a single cpu register
// (enum X86CpuRegister), or xmm register (enum X86XmmRegister).
// 'ManagedRegister::NoRegister()' provides an invalid register.
// There is a one-to-one mapping between ManagedRegister and register id.
public class X86ManagedRegister extends ManagedRegister {
    // ids map:
    //   [0..R[  cpu registers (enum X86CpuRegister)
    //   [R..X[  xmm registers (enum X86XmmRegister)
    //   [X..S[  x87 registers (enum X87Register)
    // where
    //   R = kNumberOfCpuRegIds
    //   X = R + kNumberOfXmmRegIds
    //   S = X + kNumberOfX87RegIds

    public static final int kNumberOfCpuRegIds = kNumberOfCpuRegisters;
    public static final int kNumberOfXmmRegIds = kNumberOfXmmRegisters;
    public static final int kNumberOfX87RegIds = kNumberOfX87Registers;
    public static final int kNumberOfRegIds = kNumberOfCpuRegIds + kNumberOfXmmRegIds + kNumberOfX87RegIds;

    private X86ManagedRegister(int regId) {
        super(regId);
    }

    private X86ManagedRegister() {
        super();
    }

    public static X86ManagedRegister NoRegister() {
        return new X86ManagedRegister();
    }

    public static X86ManagedRegister fromCpuRegister(X86CpuRegister r) {
        return new X86ManagedRegister(r.index());
    }

    public static X86ManagedRegister fromXmmRegister(X86XmmRegister r) {
        return new X86ManagedRegister(r.index() + kNumberOfCpuRegIds);
    }

    public static X86ManagedRegister fromX87Register(X87Register r) {
        return new X86ManagedRegister(r.index() + kNumberOfCpuRegIds + kNumberOfXmmRegIds);
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

    public X86CpuRegister asCpuRegister() {
        return X86CpuRegister.of(id);
    }

    public X86XmmRegister asXmmRegister() {
        return X86XmmRegister.of(id - kNumberOfCpuRegIds);
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
