package com.v7878.jnasm;

public abstract class ManagedRegister {
    private static final int kNoRegister = -1;

    protected final int id;

    protected ManagedRegister(int regId) {
        this.id = regId;
    }

    protected ManagedRegister() {
        this.id = kNoRegister;
    }

    public boolean equals(Object obj) {
        return obj instanceof ManagedRegister other
                && this.getClass() == other.getClass()
                && this.id == other.id;
    }

    public boolean isNoRegister() {
        return id == kNoRegister;
    }

    public boolean isRegister() {
        return id != kNoRegister;
    }

    public int regId() {
        return id;
    }

    @Override
    public String toString() {
        return "ManagedRegister(" + id + ")";
    }
}
