package com.v7878.jnasm.riscv64;

import com.v7878.jnasm.Label;

public class Riscv64Label extends Label {
    public static final int kNoPrevBranchId = -1;

    int prev_branch_id_;

    public Riscv64Label() {
        super();
        prev_branch_id_ = kNoPrevBranchId;
    }
}
