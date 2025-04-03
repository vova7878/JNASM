package com.v7878.jnasm;

public abstract class ManagedRegister {
    private static final int kNoRegister = -1;

    protected final int id_;

    protected ManagedRegister(int regId) {
        this.id_ = regId;
    }

    protected ManagedRegister() {
        this.id_ = kNoRegister;
    }

    public boolean equals(Object obj) {
        return obj instanceof ManagedRegister other && this.id_ == other.id_;
    }

    public boolean isNoRegister() {
        return id_ == kNoRegister;
    }

    public boolean isRegister() {
        return id_ != kNoRegister;
    }

    public int regId() {
        return id_;
    }

    @Override
    public String toString() {
        return "ManagedRegister(" + id_ + ")";
    }
}
