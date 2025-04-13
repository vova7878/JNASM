package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.Utils.CHECK;
import static com.v7878.jnasm.Utils.CHECK_LE;
import static com.v7878.jnasm.Utils.CHECK_NE;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondEQ;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondGE;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondGEU;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondGT;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondGTU;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondLE;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondLEU;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondLT;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondLTU;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kCondNE;
import static com.v7878.jnasm.riscv64.Branch.BranchCondition.kUncond;
import static com.v7878.jnasm.riscv64.Branch.OffsetBits.kOffset12;
import static com.v7878.jnasm.riscv64.Branch.OffsetBits.kOffset13;
import static com.v7878.jnasm.riscv64.Branch.OffsetBits.kOffset21;
import static com.v7878.jnasm.riscv64.Branch.OffsetBits.kOffset32;
import static com.v7878.jnasm.riscv64.Branch.OffsetBits.kOffset9;
import static com.v7878.jnasm.riscv64.Branch.Type.kCall;
import static com.v7878.jnasm.riscv64.Branch.Type.kCondBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kCondBranch21;
import static com.v7878.jnasm.riscv64.Branch.Type.kCondCBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kCondCBranch21;
import static com.v7878.jnasm.riscv64.Branch.Type.kLongCall;
import static com.v7878.jnasm.riscv64.Branch.Type.kLongCondBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kLongCondCBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kLongUncondBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kUncondBranch;
import static com.v7878.jnasm.riscv64.Branch.Type.kUncondCBranch;
import static com.v7878.jnasm.riscv64.XRegister.Zero;

import com.v7878.jnasm.Utils;

// Note that PC-relative literal loads are handled as pseudo branches because they need
// to be emitted after branch relocation to use correct offsets.
class Branch {
    public static final int kUnresolved = 0xffffffff;  // Unresolved target_
    public static final int kMaxBranchLength = 12;  // In bytes.

    // Bit sizes of offsets defined as enums to minimize chance of typos.
    enum OffsetBits {
        kOffset9(9),
        kOffset12(12),
        kOffset13(13),
        kOffset21(21),
        kOffset32(32);

        private final int value;

        OffsetBits(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    enum BranchCondition {
        kCondEQ,
        kCondNE,
        kCondLT,
        kCondGE,
        kCondLE,
        kCondGT,
        kCondLTU,
        kCondGEU,
        kCondLEU,
        kCondGTU,
        kUncond
    }

    enum Type {
        // Compressed branches
        kCondCBranch(2, 0, kOffset9),
        kUncondCBranch(2, 0, kOffset12),

        // Short branches
        kCondBranch(4, 0, kOffset13),
        kUncondBranch(4, 0, kOffset21),
        kCall(4, 0, kOffset21),

        // Medium branches
        // Compressed version
        kCondCBranch21(6, 2, kOffset21),
        kCondBranch21(8, 4, kOffset21),

        // Long branches.
        kLongCondCBranch(10, 2, kOffset32),
        kLongCondBranch(12, 4, kOffset32),
        kLongUncondBranch(8, 0, kOffset32),
        kLongCall(8, 0, kOffset32),

        // Label.
        kLabel(8, 0, kOffset32),

        // Literals.
        kLiteral(8, 0, kOffset32),
        kLiteralUnsigned(8, 0, kOffset32),
        kLiteralLong(8, 0, kOffset32),
        kLiteralFloat(8, 0, kOffset32),
        kLiteralDouble(8, 0, kOffset32);

        // Branch length in bytes.
        public final int length;
        // The offset in bytes of the PC used in the (only) PC-relative instruction from
        // the start of the branch sequence. RISC-V always uses the address of the PC-relative
        // instruction as the PC, so this is essentially the offset of that instruction.
        public final int pc_offset;
        // How large (in bits) a PC-relative offset can be for a given type of branch.
        public final OffsetBits offset_size;

        Type(int length, int pc_offset, OffsetBits offset_size) {
            this.length = length;
            this.pc_offset = pc_offset;
            this.offset_size = offset_size;
        }
    }

    // Some conditional branches with lhs = rhs are effectively NOPs, while some
    // others are effectively unconditional.
    public static boolean IsNop(BranchCondition condition, XRegister lhs, XRegister rhs) {
        return switch (condition) {
            case kCondNE, kCondLT, kCondGT, kCondLTU, kCondGTU -> lhs == rhs;
            default -> false;
        };
    }

    public static boolean IsUncond(BranchCondition condition, XRegister lhs, XRegister rhs) {
        return switch (condition) {
            case kUncond -> true;
            case kCondEQ, kCondGE, kCondLE, kCondLEU, kCondGEU -> lhs == rhs;
            default -> false;
        };
    }

    public static boolean IsCompressed(Type type) {
        return switch (type) {
            case kCondCBranch, kUncondCBranch, kCondCBranch21, kLongCondCBranch -> true;
            default -> false;
        };
    }

    // Calculates the distance between two byte locations in the assembler buffer and
    // returns the number of bits needed to represent the distance as a signed integer.
    public static OffsetBits GetOffsetSizeNeeded(int location, int target) {
        // For unresolved targets assume the shortest encoding
        // (later it will be made longer if needed).
        if (target == kUnresolved) {
            return kOffset9;
        }
        int distance = target - location;

        if (Utils.isInt(9, distance)) {
            return kOffset9;
        } else if (Utils.isInt(12, distance)) {
            return kOffset12;
        } else if (Utils.isInt(13, distance)) {
            return kOffset13;
        } else if (Utils.isInt(21, distance)) {
            return kOffset21;
        } else {
            return kOffset32;
        }
    }

    public static BranchCondition OppositeCondition(BranchCondition cond) {
        return switch (cond) {
            case kCondEQ -> kCondNE;
            case kCondNE -> kCondEQ;
            case kCondLT -> kCondGE;
            case kCondGE -> kCondLT;
            case kCondLE -> kCondGT;
            case kCondGT -> kCondLE;
            case kCondLTU -> kCondGEU;
            case kCondGEU -> kCondLTU;
            case kCondLEU -> kCondGTU;
            case kCondGTU -> kCondLEU;
            default -> throw new IllegalArgumentException("Unexpected branch condition " + cond);
        };
    }

    // Unconditional branch or call.
    public Branch(int location, int target, XRegister rd, boolean is_bare, boolean compression_allowed) {
        this.old_location_ = location;
        this.location_ = location;
        this.target_ = target;
        this.lhs_reg_ = rd;
        this.rhs_reg_ = Zero;
        this.freg_ = null;
        this.condition_ = kUncond;
        this.is_bare_ = is_bare;
        this.compression_allowed_ = compression_allowed;
        this.next_branch_id_ = 0;

        InitializeType(rd != Zero ? kCall :
                (compression_allowed ? kUncondCBranch : kUncondBranch));
    }

    // Conditional branch.
    public Branch(int location, int target, BranchCondition condition, XRegister lhs_reg,
                  XRegister rhs_reg, boolean is_bare, boolean compression_allowed) {
        this.old_location_ = location;
        this.location_ = location;
        this.target_ = target;
        this.lhs_reg_ = lhs_reg;
        this.rhs_reg_ = rhs_reg;
        this.freg_ = null;
        this.condition_ = condition;
        this.is_bare_ = is_bare;
        this.compression_allowed_ = compression_allowed && IsCompressableCondition();
        this.next_branch_id_ = 0;

        CHECK_NE(condition.ordinal(), kUncond.ordinal());
        CHECK(!IsNop(condition, lhs_reg, rhs_reg));
        CHECK(!IsUncond(condition, lhs_reg, rhs_reg));

        // Note: compression_allowed_ field used instead of compression_allowed parameter
        InitializeType(compression_allowed_ ? kCondCBranch : kCondBranch);
    }

    // Label address or integer literal.
    public Branch(int location, int target, XRegister rd, Type label_or_literal_type) {
        this.old_location_ = location;
        this.location_ = location;
        this.target_ = target;
        this.lhs_reg_ = rd;
        this.rhs_reg_ = Zero;
        this.freg_ = null;
        this.condition_ = kUncond;
        this.is_bare_ = false;
        this.compression_allowed_ = false;
        this.next_branch_id_ = 0;

        CHECK_NE(rd.index(), Zero.index());

        InitializeType(label_or_literal_type);
    }

    // Floting point literal.
    public Branch(int location, int target, XRegister tmp, FRegister rd, Type literal_type) {
        this.old_location_ = location;
        this.location_ = location;
        this.target_ = target;
        this.lhs_reg_ = tmp;
        this.rhs_reg_ = Zero;
        this.freg_ = rd;
        this.condition_ = kUncond;
        this.is_bare_ = false;
        this.compression_allowed_ = false;
        this.next_branch_id_ = 0;

        CHECK_NE(tmp.index(), Zero.index());

        InitializeType(literal_type);
    }

    // Completes branch construction by determining and recording its type.
    private void InitializeType(Type initial_type) {
        OffsetBits offset_size_needed = GetOffsetSizeNeeded(location_, target_);

        switch (initial_type) {
            case kCondCBranch:
                CHECK(IsCompressableCondition());
                if (condition_ != kUncond) {
                    if (is_bare_) {
                        InitShortOrLong(offset_size_needed, kCondCBranch,
                                kCondBranch, kCondCBranch21);
                    } else {
                        InitShortOrLong(offset_size_needed, kCondCBranch,
                                kCondBranch, kCondCBranch21, kLongCondCBranch);
                    }
                    break;
                }
                // fall through
            case kUncondCBranch:
                if (is_bare_) {
                    InitShortOrLong(offset_size_needed, kUncondCBranch, kUncondBranch);
                } else {
                    InitShortOrLong(offset_size_needed, kUncondCBranch, kUncondBranch, kLongUncondBranch);
                }
                break;
            case kCondBranch:
                if (condition_ != kUncond) {
                    if (is_bare_) {
                        InitShortOrLong(offset_size_needed, kCondBranch, kCondBranch21);
                    } else {
                        InitShortOrLong(offset_size_needed, kCondBranch, kCondBranch21, kLongCondBranch);
                    }
                    break;
                }
                // fall through
            case kUncondBranch:
                if (is_bare_) {
                    InitShortOrLong(offset_size_needed, kUncondBranch);
                } else {
                    InitShortOrLong(offset_size_needed, kUncondBranch, kLongUncondBranch);
                }
                break;
            case kCall:
                if (is_bare_) {
                    InitShortOrLong(offset_size_needed, kCall);
                } else {
                    InitShortOrLong(offset_size_needed, kCall, kLongCall);
                }
                break;
            case kLabel:
                type_ = initial_type;
                break;
            case kLiteral:
            case kLiteralUnsigned:
            case kLiteralLong:
            case kLiteralFloat:
            case kLiteralDouble:
                CHECK(!IsResolved());
                type_ = initial_type;
                break;
            default:
                throw new IllegalStateException("Unexpected branch type " + initial_type);
        }

        old_type_ = type_;
    }

    // Helper for the above.
    private void InitShortOrLong(OffsetBits offset_size, Type... types) {
        for (var type : types) {
            if (type.offset_size.value() >= offset_size.value()) {
                type_ = type;
                return;
            }
        }
        throw illegalOffset();
    }

    private static RuntimeException illegalOffset() {
        throw new IllegalStateException("Offset exceeds limit");
    }

    private final int old_location_;  // Offset into assembler buffer in bytes.
    private int location_;      // Offset into assembler buffer in bytes.
    private int target_;        // Offset into assembler buffer in bytes.

    // Left-hand side register in conditional branches or
    // destination register in calls or literals.
    private final XRegister lhs_reg_;
    private final XRegister rhs_reg_;          // Right-hand side register in conditional branches.
    private final FRegister freg_;             // Destination register in FP literals.
    private final BranchCondition condition_;  // Condition for conditional branches.

    private Type type_;      // Current type of the branch.
    private Type old_type_;  // Initial type of the branch.

    private final boolean is_bare_;
    private final boolean compression_allowed_;

    // Id of the next branch bound to the same label in singly-linked zero-terminated list
    // NOTE: encoded the same way as a position in a linked Label (id + BIAS)
    // Label itself is used to hold the 'head' of this list
    private int next_branch_id_;

    public Type GetType() {
        return type_;
    }

    public Type GetOldType() {
        return old_type_;
    }

    public BranchCondition GetCondition() {
        return condition_;
    }

    public XRegister GetLeftRegister() {
        return lhs_reg_;
    }

    public XRegister GetRightRegister() {
        return rhs_reg_;
    }

    public XRegister GetNonZeroRegister() {
        assert (GetLeftRegister() == Zero || GetRightRegister() == Zero);
        assert (GetLeftRegister() != Zero || GetRightRegister() != Zero);
        return GetLeftRegister() == Zero ? GetRightRegister() : GetLeftRegister();
    }

    public FRegister GetFRegister() {
        return freg_;
    }

    public int GetTarget() {
        return target_;
    }

    public int GetLocation() {
        return location_;
    }

    public int GetOldLocation() {
        return old_location_;
    }

    public int GetLength() {
        return type_.length;
    }

    public int GetOldLength() {
        return old_type_.length;
    }

    public int GetEndLocation() {
        return GetLocation() + GetLength();
    }

    public int GetOldEndLocation() {
        return GetOldLocation() + GetOldLength();
    }

    public boolean IsResolved() {
        return target_ != kUnresolved;
    }

    public int NextBranchId() {
        return next_branch_id_;
    }

    // Checks if condition meets compression requirements
    public boolean IsCompressableCondition() {
        return (condition_ == kCondEQ || condition_ == kCondNE) &&
                ((lhs_reg_ == Zero && rhs_reg_.isShortReg()) || (rhs_reg_ == Zero && lhs_reg_.isShortReg()));
    }

    // Returns the bit size of the signed offset that the branch instruction can handle.
    public OffsetBits GetOffsetSize() {
        return type_.offset_size;
    }

    // Resolve a branch when the target is known.
    public void Resolve(int target) {
        target_ = target;
    }

    // Relocate a branch by a given delta if needed due to expansion of this or another
    // branch at a given location by this delta (just changes location_ and target_).
    public void Relocate(int expand_location, int delta) {
        // All targets should be resolved before we start promoting branches.
        CHECK(IsResolved());
        if (location_ > expand_location) {
            location_ += delta;
        }
        if (target_ > expand_location) {
            target_ += delta;
        }
    }

    // If necessary, updates the type by promoting a short branch to a longer branch
    // based on the branch location and target. Returns the amount (in bytes) by
    // which the branch size has increased.
    public int PromoteIfNeeded() {
        // All targets should be resolved before we start promoting branches.
        CHECK(IsResolved());
        Type old_type = type_;
        switch (type_) {
            // Compressed branches
            case kUncondCBranch: {
                OffsetBits needed_size = GetOffsetSizeNeeded(GetOffsetLocation(), target_);
                if (needed_size.value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (needed_size.value() <= kUncondBranch.offset_size.value()) {
                    type_ = kUncondBranch;
                    break;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = kLongUncondBranch;
                break;
            }
            case kCondCBranch: {
                CHECK(IsCompressableCondition());
                OffsetBits needed_size = GetOffsetSizeNeeded(GetOffsetLocation(), target_);
                if (needed_size.value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (needed_size.value() <= kCondBranch.offset_size.value()) {
                    type_ = kCondBranch;
                    break;
                }

                // fall through
            }
            // Short branches
            case kCondBranch: {
                OffsetBits needed_size = GetOffsetSizeNeeded(GetOffsetLocation(), target_);
                if (needed_size.value() <= GetOffsetSize().value()) {
                    return 0;
                }

                Type cond21Type = compression_allowed_ ? kCondCBranch21 : kCondBranch21;
                Type longCondType = compression_allowed_ ? kLongCondCBranch : kLongCondBranch;

                // The offset remains the same for `kCond[C]Branch21` for forward branches.
                assert (cond21Type.length - cond21Type.pc_offset ==
                        kCondBranch.length - kCondBranch.pc_offset);
                if (target_ <= location_) {
                    // Calculate the needed size for kCond[C]Branch21.
                    needed_size = GetOffsetSizeNeeded(location_ + cond21Type.pc_offset, target_);
                }

                if (needed_size.value() <= cond21Type.offset_size.value()) {
                    type_ = cond21Type;
                    break;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = longCondType;
                break;
            }
            case kUncondBranch:
                if (GetOffsetSizeNeeded(GetOffsetLocation(), target_).value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = kLongUncondBranch;
                break;
            case kCall:
                if (GetOffsetSizeNeeded(GetOffsetLocation(), target_).value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = kLongCall;
                break;
            // Medium branches
            case kCondCBranch21: {
                OffsetBits needed_size = GetOffsetSizeNeeded(GetOffsetLocation(), target_);
                if (needed_size.value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = kLongCondCBranch;
                break;
            }
            case kCondBranch21: {
                OffsetBits needed_size = GetOffsetSizeNeeded(GetOffsetLocation(), target_);
                if (needed_size.value() <= GetOffsetSize().value()) {
                    return 0;
                }

                if (is_bare_) {
                    throw illegalOffset();
                }

                type_ = kLongCondBranch;
                break;
            }
            default:
                // Other branch types cannot be promoted.
                CHECK_LE(GetOffsetSizeNeeded(GetOffsetLocation(), target_).value(), GetOffsetSize().value());
                return 0;
        }
        assert (type_.length > old_type.length);
        return type_.length - old_type.length;
    }

    // Returns the offset into assembler buffer that shall be used as the base PC for
    // offset calculation. RISC-V always uses the address of the PC-relative instruction
    // as the PC, so this is essentially the location of that instruction.
    public int GetOffsetLocation() {
        return location_ + type_.pc_offset;
    }

    // Calculates and returns the offset ready for encoding in the branch instruction(s).
    public int GetOffset() {
        CHECK(IsResolved());
        // Calculate the byte distance between instructions and also account for
        // different PC-relative origins.
        return target_ - GetOffsetLocation();
    }

    // Link with the next branch
    public void LinkToList(int next_branch_id) {
        next_branch_id_ = next_branch_id;
    }
}
