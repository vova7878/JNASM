package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.Utils.CHECK;
import static com.v7878.jnasm.Utils.CHECK_ALIGNED;
import static com.v7878.jnasm.Utils.CHECK_EQ;
import static com.v7878.jnasm.Utils.CHECK_IMPLIES;
import static com.v7878.jnasm.Utils.CHECK_LE;
import static com.v7878.jnasm.Utils.CHECK_LT;
import static com.v7878.jnasm.Utils.CHECK_NE;
import static com.v7878.jnasm.Utils.isAligned;
import static com.v7878.jnasm.Utils.isInt;
import static com.v7878.jnasm.Utils.isLInt;
import static com.v7878.jnasm.Utils.roundDown;
import static com.v7878.jnasm.Utils.roundUp;
import static com.v7878.jnasm.riscv64.RV64Branch.BranchCondition;
import static com.v7878.jnasm.riscv64.RV64Branch.BranchCondition.kCondEQ;
import static com.v7878.jnasm.riscv64.RV64Branch.BranchCondition.kCondNE;
import static com.v7878.jnasm.riscv64.RV64Extension.kRiscv64CompressedExtensionsMask;
import static com.v7878.jnasm.riscv64.RV64FenceType.kFenceNNRW;
import static com.v7878.jnasm.riscv64.RV64VRegister.V0;
import static com.v7878.jnasm.riscv64.RV64XRegister.RA;
import static com.v7878.jnasm.riscv64.RV64XRegister.SP;
import static com.v7878.jnasm.riscv64.RV64XRegister.TMP;
import static com.v7878.jnasm.riscv64.RV64XRegister.Zero;

import com.v7878.jnasm.Assembler;
import com.v7878.jnasm.Label;
import com.v7878.jnasm.Utils;
import com.v7878.jnasm.riscv64.RV64Branch.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class RV64Assembler extends Assembler implements RV64AssemblerI {
    private static final int kXlen = 64;

    private final List<RV64Branch> branches_;

    private final int no_override_enabled_extensions;
    private int enabled_extensions;

    // Whether appending instructions at the end of the buffer or overwriting the existing ones.
    private boolean overwriting;
    // The current overwrite location.
    private int overwrite_location;

    private final List<RV64Literal> literals_;
    private final List<RV64Literal> long_literals_;  // 64-bit literals separated for alignment reasons.

    public RV64Assembler(int enabled_extensions) {
        this.no_override_enabled_extensions =
                this.enabled_extensions = enabled_extensions;
        this.branches_ = new ArrayList<>();
        this.literals_ = new ArrayList<>();
        this.long_literals_ = new ArrayList<>();
    }

    public void finalizeCode() {
        super.finalizeCode();
        EmitLiterals();
        PromoteBranches();
        EmitBranches();
    }

    private boolean IsExtensionEnabled(RV64Extension ext) {
        return (enabled_extensions & ext.extensionBit()) != 0;
    }

    private void AssertExtensionsEnabled(RV64Extension ext) {
        if (!IsExtensionEnabled(ext)) {
            throw new IllegalStateException(String.format(
                    "Extension %s is not enabled", ext));
        }
    }

    private void AssertExtensionsEnabled(RV64Extension ext, RV64Extension... other_exts) {
        AssertExtensionsEnabled(ext);
        for (var other_ext : other_exts) {
            AssertExtensionsEnabled(other_ext);
        }
    }

    private enum Nf {
        k1(0b000),
        k2(0b001),
        k3(0b010),
        k4(0b011),
        k5(0b100),
        k6(0b101),
        k7(0b110),
        k8(0b111);

        private final int value;

        Nf(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private enum VAIEncoding {
        // ----Operands---- | Type of Scalar                | Instruction type
        kOPIVV(0b000),  // vector-vector    | --                            | R-type
        kOPFVV(0b001),  // vector-vector    | --                            | R-type
        kOPMVV(0b010),  // vector-vector    | --                            | R-type
        kOPIVI(0b011),  // vector-immediate | imm[4:0]                      | R-type
        kOPIVX(0b100),  // vector-scalar    | GPR x register rs1            | R-type
        kOPFVF(0b101),  // vector-scalar    | FP f register rs1             | R-type
        kOPMVX(0b110),  // vector-scalar    | GPR x register rs1            | R-type
        kOPCFG(0b111);  // scalars-imms     | GPR x register rs1 & rs2/imm  | R/I-type

        private final int value;

        VAIEncoding(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private enum MemAddressMode {
        kUnitStride(0b00),
        kIndexedUnordered(0b01),
        kStrided(0b10),
        kIndexedOrdered(0b11);

        private final int value;

        MemAddressMode(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private enum VectorWidth {
        k8(0b000),
        k16(0b101),
        k32(0b110),
        k64(0b111);

        // TODO
        public static final VectorWidth kMask = k8;
        public static final VectorWidth kWholeR = k8;

        private final int value;

        VectorWidth(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    // Create a mask for the least significant "bits"
    // The returned value is always unsigned to prevent undefined behavior for bitwise ops.
    //
    // Given 'bits',
    // Returns:
    //                   <--- bits --->
    // +-----------------+------------+
    // | 0 ............0 |   1.....1  |
    // +-----------------+------------+
    // msb                           lsb
    private static int MaskLeastSignificant(int bits) {
        assert bits <= 32 : "Bits out of range for int";
        if (bits >= 32) {
            return -1;
        } else {
            return (1 << bits) - 1;
        }
    }

    // Clears the bitfield starting at the least significant bit "lsb" with a bitwidth of 'width'.
    // (Equivalent of ARM BFC instruction).
    //
    // Given:
    //           <-- width  -->
    // +--------+------------+--------+
    // | ABC... |  bitfield  | XYZ... +
    // +--------+------------+--------+
    //                       lsb      0
    // Returns:
    //           <-- width  -->
    // +--------+------------+--------+
    // | ABC... | 0........0 | XYZ... +
    // +--------+------------+--------+
    //                       lsb      0
    private static int BitFieldClear(int value, int lsb, int width) {
        assert lsb + width <= 32 : "Bit field out of range for value";
        final int mask = MaskLeastSignificant(width);

        return value & ~(mask << lsb);
    }

    // Inserts the contents of 'data' into bitfield of 'value'  starting
    // at the least significant bit "lsb" with a bitwidth of 'width'.
    // (Equivalent of ARM BFI instruction).
    //
    // Given (data):
    //           <-- width  -->
    // +--------+------------+--------+
    // | ABC... |  bitfield  | XYZ... +
    // +--------+------------+--------+
    //                       lsb      0
    // Returns:
    //           <-- width  -->
    // +--------+------------+--------+
    // | ABC... | 0...data   | XYZ... +
    // +--------+------------+--------+
    //                       lsb      0
    private static int BitFieldInsert(int value, int data, int lsb, int width) {
        assert lsb + width <= 32 : "Bit field out of range for value";
        final int data_mask = MaskLeastSignificant(width);
        final int value_cleared = BitFieldClear(value, lsb, width);

        return value_cleared | ((data & data_mask) << lsb);
    }

    // Extracts the bitfield starting at the least significant bit "lsb" with a bitwidth of 'width'.
    // Signed types are sign-extended during extraction. (Equivalent of ARM UBFX/SBFX instruction).
    //
    // Given:
    //           <-- width   -->
    // +--------+-------------+-------+
    // |        |   bitfield  |       +
    // +--------+-------------+-------+
    //                       lsb      0
    // Returns:
    //                  <-- width   -->
    // +----------------+-------------+
    // | 0...        0  |   bitfield  |
    // +----------------+-------------+
    //                                0
    // where S is the highest bit in 'bitfield'.
    private static int BitFieldExtract(int value, int lsb, int width) {
        assert lsb + width <= 32 : "Bit field out of range for value";
        return (value >>> lsb) & MaskLeastSignificant(width);
    }

    private record OffsetPair(int imm20, int short_offset) {
    }

    // Split 32-bit offset into an `imm20` for LUI/AUIPC and
    // a signed 12-bit short offset for ADDI/JALR/etc.
    private static OffsetPair SplitOffset(int offset) {
        // The highest 0x800 values are out of range.
        CHECK_LT(offset, 0x7ffff800);
        // Round `offset` to nearest 4KiB offset because short offset has range [-0x800, 0x800).
        int near_offset = (offset + 0x800) & ~0xfff;
        // Calculate the short offset.
        int short_offset = offset - near_offset;
        assert (isInt(12, short_offset));
        // Extract the `imm20`.
        int imm20 = near_offset >>> 12;
        // Return the result as a pair.
        return new OffsetPair(imm20, short_offset);
    }

    private static int ToInt12(int uint12) {
        CHECK(Utils.isUInt(12, uint12));
        return uint12 - ((uint12 & 0x800) << 1);
    }

    // RVV constants and helpers

    @SuppressWarnings("SameParameterValue")
    private static int EncodeRVVMemF7(Nf nf, int mew, MemAddressMode mop, RV64VM vm) {
        CHECK(Utils.isUInt(3, nf.value()));
        CHECK(Utils.isUInt(1, mew));
        CHECK(Utils.isUInt(2, mop.value()));
        CHECK(Utils.isUInt(1, vm.value()));

        return (nf.value() << 4) | (mew << 3) | (mop.value() << 1) | vm.value();
    }

    private static int EncodeRVVF7(int funct6, RV64VM vm) {
        CHECK(Utils.isUInt(6, funct6));
        return (funct6 << 1) | vm.value();
    }

    private static int EncodeIntWidth(int width, int imm) {
        CHECK(isInt(width, imm));
        return imm & MaskLeastSignificant(width);
    }

    private static int EncodeInt5(int imm) {
        return EncodeIntWidth(5, imm);
    }

    private static int EncodeInt6(int imm) {
        return EncodeIntWidth(6, imm);
    }

    static boolean IsShortRegIndex(int reg) {
        int uv = reg - 8;
        return Utils.isUInt(3, uv);
    }

    static int EncodeShortRegIndex(int reg) {
        CHECK(IsShortRegIndex(reg));
        return reg - 8;
    }

    // Rearrange given offset in the way {offset[0] | offset[1]}
    private static int EncodeOffset0_1(int offset) {
        CHECK(Utils.isUInt(2, offset));
        return offset >>> 1 | (offset & 1) << 1;
    }

    // Rearrange given offset, scaled by 4, in the way {offset[5:2] | offset[7:6]}
    private static int ExtractOffset52_76(int offset) {
        assert isAligned(offset, 4) : "Offset should be scalable by 4";
        CHECK(Utils.isUInt(8, offset));

        int imm_52 = BitFieldExtract(offset, 2, 4);
        int imm_76 = BitFieldExtract(offset, 6, 2);

        return BitFieldInsert(imm_76, imm_52, 2, 4);
    }

    // Rearrange given offset, scaled by 8, in the way {offset[5:3] | offset[8:6]}
    private static int ExtractOffset53_86(int offset) {
        assert isAligned(offset, 8) : "Offset should be scalable by 8";
        CHECK(Utils.isUInt(9, offset));

        int imm_53 = BitFieldExtract(offset, 3, 3);
        int imm_86 = BitFieldExtract(offset, 6, 3);

        return BitFieldInsert(imm_86, imm_53, 3, 3);
    }

    // Rearrange given offset, scaled by 4, in the way {offset[5:2] | offset[6]}
    private static int ExtractOffset52_6(int offset) {
        assert isAligned(offset, 4) : "Offset should be scalable by 4";
        CHECK(Utils.isUInt(7, offset));

        int imm_52 = BitFieldExtract(offset, 2, 4);
        int imm_6 = BitFieldExtract(offset, 6, 1);

        return BitFieldInsert(imm_6, imm_52, 1, 4);
    }

    // Rearrange given offset, scaled by 8, in the way {offset[5:3], offset[7:6]}
    private static int ExtractOffset53_76(int offset) {
        assert isAligned(offset, 8) : "Offset should be scalable by 8";
        CHECK(Utils.isUInt(8, offset));

        int imm_53 = BitFieldExtract(offset, 3, 3);
        int imm_76 = BitFieldExtract(offset, 6, 2);

        return BitFieldInsert(imm_76, imm_53, 2, 3);
    }

    private static boolean IsImmCLuiEncodable(int uimm) {
        // Instruction c.lui is odd and its immediate value is a bit tricky
        // Its value is not a full 32 bits value, but its bits [31:12]
        // (where the bit 17 marks the sign bit) shifted towards the bottom i.e. bits [19:0]
        // are the meaningful ones. Since that we want a signed non-zero 6-bit immediate to
        // keep values in the range [0, 0x1f], and the range [0xfffe0, 0xfffff] for negative values
        // since the sign bit was bit 17 (which is now bit 5 and replicated in the higher bits too)
        // Also encoding with immediate = 0 is reserved
        // For more details please see 16.5 chapter is the specification

        if (uimm == 0) return false;
        if (Utils.isUInt(5, uimm)) return true;
        return Utils.isUInt(5, uimm - 0xfffe0);
    }

    private RV64Branch GetBranch(int branch_id) {
        return branches_.get(branch_id);
    }

    @Override
    public void bind(Label label) {
        bind((RV64Label) label);
    }

    public void bind(RV64Label label) {
        CHECK(!label.isBound());
        int bound_pc = size();

        // Walk the list of branches referring to and preceding this label.
        // Store the previously unknown target addresses in them.
        while (label.isLinked()) {
            int branch_id = label.getLinkPosition();
            RV64Branch branch = GetBranch(branch_id);
            branch.Resolve(bound_pc);
            // On to the next branch in the list...
            label.position = branch.NextBranchId();
        }

        // Now make the label object contain its own location (relative to the end of the preceding
        // branch, if any; it will be used by the branches referring to and following this label).
        int prev_branch_id = RV64Label.kNoPrevBranchId;
        if (!branches_.isEmpty()) {
            prev_branch_id = branches_.size() - 1;
            RV64Branch prev_branch = GetBranch(prev_branch_id);
            bound_pc -= prev_branch.GetEndLocation();
        }
        label.prev_branch_id_ = prev_branch_id;
        label.bindTo(bound_pc);
    }

    @Override
    public void jump(Label label) {
        jump((RV64Label) label);
    }

    public void jump(RV64Label label) {
        J(label, false);
    }

    private int GetLabelLocation(RV64Label label) {
        CHECK(label.isBound());
        int target = label.getPosition();
        if (label.prev_branch_id_ != RV64Label.kNoPrevBranchId) {
            // Get label location based on the branch preceding it.
            RV64Branch prev_branch = GetBranch(label.prev_branch_id_);
            target += prev_branch.GetEndLocation();
        }
        return target;
    }

    // Emit helpers.

    private void Emit16(int value) {
        if (overwriting) {
            // Branches to labels are emitted into their placeholders here.
            store16(overwrite_location, value);
            overwrite_location += 2;
        } else {
            // Other instructions are simply appended at the end here.
            emit16(value);
        }
    }

    private void Emit32(int value) {
        if (overwriting) {
            // Branches to labels are emitted into their placeholders here.
            store32(overwrite_location, value);
            overwrite_location += 4;
        } else {
            // Other instructions are simply appended at the end here.
            emit32(value);
        }
    }

    // I-type instruction:
    //
    //    31                   20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . . . . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [        imm11:0            rs1   funct3     rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitI(int imm12, int rs1, int funct3, int rd, int opcode) {
        CHECK(isInt(12, imm12));
        CHECK(Utils.isUInt(5, rs1));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, rd));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = (imm12 << 20) | (rs1 << 15) |
                (funct3 << 12) | (rd << 7) | opcode;
        Emit32(encoding);
    }

    // R-type instruction:
    //
    //    31         25 24     20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . . | . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [   funct7        rs2       rs1   funct3     rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitR(int funct7, int rs2, int rs1, int funct3, int rd, int opcode) {
        CHECK(Utils.isUInt(7, funct7));
        CHECK(Utils.isUInt(5, rs2));
        CHECK(Utils.isUInt(5, rs1));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, rd));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = (funct7 << 25) | (rs2 << 20) | (rs1 << 15)
                | (funct3 << 12) | (rd << 7) | opcode;
        Emit32(encoding);
    }

    // R-type instruction variant for floating-point fused multiply-add/sub (F[N]MADD/ F[N]MSUB):
    //
    //    31     27  25 24     20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . | . | . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [  rs3     fmt    rs2       rs1   funct3     rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitR4(int rs3, int fmt, int rs2, int rs1, int funct3, int rd, int opcode) {
        CHECK(Utils.isUInt(5, rs3));
        CHECK(Utils.isUInt(2, fmt));
        CHECK(Utils.isUInt(5, rs2));
        CHECK(Utils.isUInt(5, rs1));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, rd));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = (rs3 << 27) | (fmt << 25) | (rs2 << 20)
                | (rs1 << 15) | (funct3 << 12) | (rd << 7) | opcode;
        Emit32(encoding);
    }

    // S-type instruction:
    //
    //    31         25 24     20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . . | . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [   imm11:5       rs2       rs1   funct3   imm4:0      opcode   ]
    //   -----------------------------------------------------------------
    private void EmitS(int imm12, int rs2, int rs1, int funct3, int opcode) {
        CHECK(isInt(12, imm12));
        CHECK(Utils.isUInt(5, rs2));
        CHECK(Utils.isUInt(5, rs1));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = ((imm12 & 0xFE0) << 20) | (rs2 << 20) | (rs1 << 15)
                | (funct3 << 12) | ((imm12 & 0x1F) << 7) | opcode;
        Emit32(encoding);
    }

    // I-type instruction variant for shifts (SLLI / SRLI / SRAI):
    //
    //    31       26 25       20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . | . . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [  imm11:6  imm5:0(shamt)   rs1   funct3     rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitI6(int funct6, int imm6, RV64XRegister rs1, int funct3, RV64XRegister rd, int opcode) {
        CHECK(Utils.isUInt(6, funct6));
        CHECK(Utils.isUInt(6, imm6));
        CHECK(Utils.isUInt(5, rs1.index()));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, rd.index()));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = (funct6 << 26) | (imm6 << 20) | (rs1.index() << 15)
                | (funct3 << 12) | (rd.index() << 7) | opcode;
        Emit32(encoding);
    }

    // B-type instruction:
    //
    //   31 30       25 24     20 19     15 14 12 11    8 7 6           0
    //   -----------------------------------------------------------------
    //   [ | . . . . . | . . . . | . . . . | . . | . . . | | . . . . . . ]
    //  imm12 imm11:5      rs2       rs1   funct3 imm4:1 imm11  opcode   ]
    //   -----------------------------------------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitB(int offset, RV64XRegister rs2, RV64XRegister rs1, int funct3, int opcode) {
        CHECK_ALIGNED(offset, 2);
        CHECK(isInt(13, offset));
        CHECK(Utils.isUInt(5, rs2.index()));
        CHECK(Utils.isUInt(5, rs1.index()));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(7, opcode));
        int imm12 = ((offset) >> 1) & 0xfff;
        int encoding = ((imm12 & 0x800) << (31 - 11)) | ((imm12 & 0x03f0) << (25 - 4))
                | (rs2.index() << 20) | (rs1.index() << 15) | (funct3 << 12)
                | ((imm12 & 0xf) << 8) | ((imm12 & 0x400) >> (10 - 7)) | opcode;
        Emit32(encoding);
    }

    // U-type instruction:
    //
    //    31                                   12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . . . . . . . . . . . . . . . | . . . . | . . . . . . ]
    //   [                imm31:12                    rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitU(int imm20, RV64XRegister rd, int opcode) {
        CHECK(Utils.isUInt(20, imm20));
        CHECK(Utils.isUInt(5, rd.index()));
        CHECK(Utils.isUInt(7, opcode));
        int encoding = (imm20 << 12) | (rd.index() << 7) | opcode;
        Emit32(encoding);
    }

    // J-type instruction:
    //
    //   31 30               21   19           12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ | . . . . . . . . . | | . . . . . . . | . . . . | . . . . . . ]
    //  imm20    imm10:1      imm11   imm19:12        rd        opcode   ]
    //   -----------------------------------------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitJ(int offset, RV64XRegister rd, int opcode) {
        CHECK_ALIGNED(offset, 2);
        CHECK(isInt(21, offset));
        CHECK(Utils.isUInt(5, rd.index()));
        CHECK(Utils.isUInt(7, opcode));
        int imm20 = (offset >> 1) & 0xfffff;
        int encoding = ((imm20 & 0x80000) << (31 - 19)) | ((imm20 & 0x03ff) << 21)
                | ((imm20 & 0x400) << (20 - 10)) | ((imm20 & 0x7f800) << (12 - 11))
                | (rd.index() << 7) | opcode;
        Emit32(encoding);
    }

    // Compressed Instruction Encodings

    // CR-type instruction:
    //
    //   15    12 11      7 6       2 1 0
    //   ---------------------------------
    //   [ . . . | . . . . | . . . . | . ]
    //   [ func4   rd/rs1      rs2    op ]
    //   ---------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitCR(int funct4, RV64XRegister rd_rs1, RV64XRegister rs2, int opcode) {
        CHECK(Utils.isUInt(4, funct4));
        CHECK(Utils.isUInt(5, rd_rs1.index()));
        CHECK(Utils.isUInt(5, rs2.index()));
        CHECK(Utils.isUInt(2, opcode));

        int encoding = (funct4 << 12) | (rd_rs1.index() << 7)
                | (rs2.index() << 2) | opcode;
        Emit16(encoding);
    }

    // CI-type instruction:
    //
    //   15  13   11      7 6       2 1 0
    //   ---------------------------------
    //   [ . . | | . . . . | . . . . | . ]
    //   [func3 imm rd/rs1     imm    op ]
    //   ---------------------------------
    private void EmitCI(int funct3, int rd_rs1, int imm6, int opcode) {
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, rd_rs1));
        CHECK(Utils.isUInt(6, imm6));
        CHECK(Utils.isUInt(2, opcode));

        int immH1 = BitFieldExtract(imm6, 5, 1);
        int immL5 = BitFieldExtract(imm6, 0, 5);

        int encoding = (funct3 << 13) | (immH1 << 12) |
                (rd_rs1 << 7) | (immL5 << 2) | opcode;
        Emit16(encoding);
    }

    // CSS-type instruction:
    //
    //   15  13 12        7 6       2 1 0
    //   ---------------------------------
    //   [ . . | . . . . . | . . . . | . ]
    //   [func3     imm6      rs2     op ]
    //   ---------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitCSS(int funct3, int offset6, int rs2, int opcode) {
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(6, offset6));
        CHECK(Utils.isUInt(5, rs2));
        CHECK(Utils.isUInt(2, opcode));

        int encoding = (funct3 << 13) | (offset6 << 7) | (rs2 << 2) | opcode;
        Emit16(encoding);
    }

    // CIW-type instruction:
    //
    //   15  13 12            5 4   2 1 0
    //   ---------------------------------
    //   [ . . | . . . . . . . | . . | . ]
    //   [func3     imm8         rd'  op ]
    //   ---------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitCIW(int funct3, int imm8, int rd_s, int opcode) {
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(8, imm8));
        CHECK(IsShortRegIndex(rd_s));
        CHECK(Utils.isUInt(2, opcode));

        int encoding = (funct3 << 13) | (imm8 << 5) | (EncodeShortRegIndex(rd_s) << 2) | opcode;
        Emit16(encoding);
    }

    // CL/S-type instruction:
    //
    //   15  13 12  10 9  7 6 5 4   2 1 0
    //   ---------------------------------
    //   [ . . | . . | . . | . | . . | . ]
    //   [func3  imm   rs1' imm rds2' op ]
    //   ---------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitCM(int funct3, int imm5, RV64XRegister rs1_s, int rd_rs2_s, int opcode) {
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(5, imm5));
        CHECK(rs1_s.isShortReg());
        CHECK(IsShortRegIndex(rd_rs2_s));
        CHECK(Utils.isUInt(2, opcode));

        int immH3 = BitFieldExtract(imm5, 2, 3);
        int immL2 = BitFieldExtract(imm5, 0, 2);

        int encoding = (funct3 << 13) | (immH3 << 10) | (rs1_s.encodeShortReg() << 7)
                | (immL2 << 5) | (EncodeShortRegIndex(rd_rs2_s) << 2) | opcode;
        Emit16(encoding);
    }

    // CA-type instruction:
    //
    //   15         10 9  7 6 5 4   2 1 0
    //   ---------------------------------
    //   [ . . . . . | . . | . | . . | . ]
    //   [    funct6 rds1' funct2 rs2' op]
    //   ---------------------------------
    private void EmitCA(int funct6, RV64XRegister rd_rs1_s, int funct2, int rs2_v, int opcode) {
        CHECK(Utils.isUInt(6, funct6));
        CHECK(rd_rs1_s.isShortReg());
        CHECK(Utils.isUInt(2, funct2));
        CHECK(Utils.isUInt(3, rs2_v));
        CHECK(Utils.isUInt(2, opcode));

        int encoding = (funct6 << 10) | (rd_rs1_s.encodeShortReg() << 7)
                | (funct2 << 5) | (rs2_v << 2) | opcode;
        Emit16(encoding);
    }

    private void EmitCAReg(int funct6, RV64XRegister rd_rs1_s, int funct2, RV64XRegister rs2_s, int opcode) {
        CHECK(rs2_s.isShortReg());
        EmitCA(funct6, rd_rs1_s, funct2, rs2_s.encodeShortReg(), opcode);
    }

    @SuppressWarnings("SameParameterValue")
    private void EmitCAImm(int funct6, RV64XRegister rd_rs1_s, int funct2, int funct3, int opcode) {
        EmitCA(funct6, rd_rs1_s, funct2, funct3, opcode);
    }

    // CB-type instruction:
    //
    //   15  13 12  10 9  7 6       2 1 0
    //   ---------------------------------
    //   [ . . | . . | . . | . . . . | . ]
    //   [func3 offset rs1'   offset  op ]
    //   ---------------------------------
    private void EmitCB(int funct3, int offset8, RV64XRegister rd_rs1_s, int opcode) {
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(8, offset8));
        CHECK(rd_rs1_s.isShortReg());
        CHECK(Utils.isUInt(2, opcode));

        int offsetH3 = BitFieldExtract(offset8, 5, 3);
        int offsetL5 = BitFieldExtract(offset8, 0, 5);

        int encoding = (funct3 << 13) | (offsetH3 << 10) |
                (rd_rs1_s.encodeShortReg() << 7) | (offsetL5 << 2) | opcode;
        Emit16(encoding);
    }

    // Wrappers for EmitCB with different imm bit permutation

    @SuppressWarnings("SameParameterValue")
    private void EmitCBBranch(int funct3, int offset, RV64XRegister rs1_s, int opcode) {
        CHECK(isInt(9, offset));
        CHECK_ALIGNED(offset, 2);

        // offset[8|4:3]
        int offsetH3 = (BitFieldExtract(offset, 8, 1) << 2) |
                BitFieldExtract(offset, 3, 2);
        // offset[7:6|2:1|5]
        int offsetL5 = (BitFieldExtract(offset, 6, 2) << 3) |
                (BitFieldExtract(offset, 1, 2) << 1) |
                BitFieldExtract(offset, 5, 1);

        EmitCB(funct3, BitFieldInsert(offsetL5, offsetH3, 5, 3), rs1_s, opcode);
    }

    @SuppressWarnings("SameParameterValue")
    private void EmitCBArithmetic(int funct3, int funct2, int imm, RV64XRegister rd_s, int opcode) {
        int imm_5 = BitFieldExtract(imm, 5, 1);
        int immH3 = BitFieldInsert(funct2, imm_5, 2, 1);
        int immL5 = BitFieldExtract(imm, 0, 5);

        EmitCB(funct3, BitFieldInsert(immL5, immH3, 5, 3), rd_s, opcode);
    }

    // CJ-type instruction:
    //
    //   15  13 12                  2 1 0
    //   ---------------------------------
    //   [ . . | . . . . . . . . . . | . ]
    //   [func3    jump target 11     op ]
    //   ---------------------------------
    @SuppressWarnings("SameParameterValue")
    private void EmitCJ(int funct3, int offset, int opcode) {
        CHECK_ALIGNED(offset, 2);
        CHECK(isInt(12, offset));
        CHECK(Utils.isUInt(3, funct3));
        CHECK(Utils.isUInt(2, opcode));

        // offset[11|4|9:8|10|6|7|3:1|5]
        int jumpt = (BitFieldExtract(offset, 11, 1) << 10) |
                (BitFieldExtract(offset, 4, 1) << 9) |
                (BitFieldExtract(offset, 8, 2) << 7) |
                (BitFieldExtract(offset, 10, 1) << 6) |
                (BitFieldExtract(offset, 6, 1) << 5) |
                (BitFieldExtract(offset, 7, 1) << 4) |
                (BitFieldExtract(offset, 1, 3) << 1) |
                BitFieldExtract(offset, 5, 1);

        CHECK(Utils.isUInt(11, jumpt));

        int encoding = funct3 << 13 | jumpt << 2 | opcode;
        Emit16(encoding);
    }

    private void EmitLiterals() {
        for (var literal : literals_) {
            bind(literal.getLabel());
            CHECK_EQ(literal.getSize(), 4);
            emit32((int) literal.getValue());
        }
        // These need to be 8-byte-aligned but we shall add the alignment padding after the branch
        // promotion, if needed. Since all literals are accessed with AUIPC+Load(imm12) without branch
        // promotion, this late adjustment cannot take long literals out of instruction range.
        for (var literal : long_literals_) {
            bind(literal.getLabel());
            CHECK_EQ(literal.getSize(), 8);
            emit64(literal.getValue());
        }
    }

    private void AlignLiterals(List<RV64Literal> literals, int element_size) {
        // This can increase the PC-relative distance but all literals are accessed with AUIPC+Load(imm12)
        // without branch promotion, so this late adjustment cannot take them out of instruction range.
        if (!literals.isEmpty()) {
            int first_literal_location = GetLabelLocation(literals.getFirst().getLabel());
            int padding = roundUp(first_literal_location, element_size) - first_literal_location;
            if (padding != 0) {
                // Insert the padding and fill it with zeros.
                getBuffer().resize(size() + padding);
                int lit_size = literals.size() * element_size;
                getBuffer().move(first_literal_location + padding, first_literal_location, lit_size);
                for (int i = 0; i < padding; i++) {
                    store8(first_literal_location + i, 0);
                }
                // Increase target addresses in literal and address loads in order for correct
                // offsets from PC to be generated.
                for (var branch : branches_) {
                    var target = branch.GetTarget();
                    if (target >= first_literal_location) {
                        branch.Resolve(target + padding);
                    }
                }
                // If after this we ever call GetLabelLocation() to get the location of a literal,
                // we need to adjust the location of the literal's label as well.
                for (var literal : literals) {
                    // Bound label's position is negative, hence decrementing it instead of incrementing.
                    literal.getLabel().position -= padding;
                }
            }
        }
    }

    private void PromoteBranches() {
        // Promote short branches to long as necessary.
        boolean changed;
        // To avoid re-computing predicate on each iteration cache it in local
        do {
            changed = false;
            for (var branch : branches_) {
                CHECK(branch.IsResolved());
                int delta = branch.PromoteIfNeeded();
                // If this branch has been promoted and needs to expand in size,
                // relocate all branches by the expansion size.
                if (delta != 0) {
                    changed = true;
                    int expand_location = branch.GetLocation();
                    for (var branch2 : branches_) {
                        branch2.Relocate(expand_location, delta);
                    }
                }
            }
        } while (changed);

        // Account for branch expansion by resizing the code buffer
        // and moving the code in it to its final location.
        int branch_count = branches_.size();
        if (branch_count > 0) {
            // Resize.
            RV64Branch last_branch = branches_.get(branch_count - 1);
            int size_delta = last_branch.GetEndLocation() - last_branch.GetOldEndLocation();
            int old_size = size();
            getBuffer().resize(old_size + size_delta);
            // Move the code residing between branch placeholders.
            int end = old_size;
            for (int i = branch_count; i > 0; ) {
                RV64Branch branch = branches_.get(--i);
                int size = end - branch.GetOldEndLocation();
                getBuffer().move(branch.GetEndLocation(), branch.GetOldEndLocation(), size);
                end = branch.GetOldLocation();
            }
        }

        // Align literals by moving them up if needed.
        AlignLiterals(literals_, 4);
        AlignLiterals(long_literals_, 8);
    }

    private void EmitBcond(BranchCondition cond,
                           RV64XRegister rs,
                           RV64XRegister rt,
                           int offset) {
        switch (cond) {
            case kCondEQ -> Beq(rs, rt, offset);
            case kCondNE -> Bne(rs, rt, offset);
            case kCondLT -> Blt(rs, rt, offset);
            case kCondGE -> Bge(rs, rt, offset);
            case kCondLE -> Ble(rs, rt, offset);
            case kCondGT -> Bgt(rs, rt, offset);
            case kCondLTU -> Bltu(rs, rt, offset);
            case kCondGEU -> Bgeu(rs, rt, offset);
            case kCondLEU -> Bleu(rs, rt, offset);
            case kCondGTU -> Bgtu(rs, rt, offset);
            default -> throw new IllegalStateException("Unexpected branch condition " + cond);
        }
    }

    private class ScopedExtensionsOverride implements AutoCloseable {
        private final int old_enabled_extensions_;

        public ScopedExtensionsOverride(int enabled_extensions) {
            old_enabled_extensions_ = RV64Assembler.this.enabled_extensions;
            RV64Assembler.this.enabled_extensions = enabled_extensions;
        }

        @Override
        public void close() {
            RV64Assembler.this.enabled_extensions = old_enabled_extensions_;
        }
    }

    private ScopedExtensionsOverride noCompression() {
        return new ScopedExtensionsOverride(
                no_override_enabled_extensions & ~kRiscv64CompressedExtensionsMask);
    }

    private ScopedExtensionsOverride useCompression() {
        return new ScopedExtensionsOverride(no_override_enabled_extensions);
    }

    private void EmitBranch(RV64Branch branch) {
        CHECK(overwriting);
        overwrite_location = branch.GetLocation();
        final int offset = branch.GetOffset();
        BranchCondition condition = branch.GetCondition();
        RV64XRegister lhs = branch.GetLeftRegister();
        RV64XRegister rhs = branch.GetRightRegister();

        // Disable Compressed emitter explicitly and enable where it is needed
        try (var ignored = noCompression()) {
            BiConsumer<RV64XRegister, IntConsumer> emit_auipc_and_next = (reg, next) -> {
                CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                var pair = SplitOffset(offset);
                Auipc(reg, pair.imm20);
                next.accept(pair.short_offset);
            };

            Runnable emit_cbcondz_opposite = () -> {
                assert (branch.IsCompressableCondition());
                try (var ignored1 = useCompression()) {
                    if (condition == kCondNE) {
                        assert (RV64Branch.OppositeCondition(condition) == kCondEQ);
                        CBeqz(branch.GetNonZeroRegister(), branch.GetLength());
                    } else {
                        assert (RV64Branch.OppositeCondition(condition) == kCondNE);
                        CBnez(branch.GetNonZeroRegister(), branch.GetLength());
                    }
                }
            };

            switch (branch.GetType()) {
                // Compressed branches
                case kCondCBranch: {
                    try (var ignored1 = useCompression()) {
                        CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                        assert (branch.IsCompressableCondition());
                        if (condition == kCondEQ) {
                            CBeqz(branch.GetNonZeroRegister(), offset);
                        } else {
                            CBnez(branch.GetNonZeroRegister(), offset);
                        }
                    }
                    break;
                }
                case kUncondCBranch: {
                    try (var ignored1 = useCompression()) {
                        CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                        CJ(offset);
                    }
                    break;
                }

                // Short branches.
                case kUncondBranch:
                    CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                    J(offset);
                    break;
                case kCondBranch:
                    CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                    EmitBcond(condition, lhs, rhs, offset);
                    break;
                case kCall:
                    CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                    CHECK(lhs != Zero);
                    Jal(lhs, offset);
                    break;

                // Medium branch.
                case kCondBranch21:
                    EmitBcond(RV64Branch.OppositeCondition(condition), lhs, rhs, branch.GetLength());
                    CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                    J(offset);
                    break;
                case kCondCBranch21: {
                    emit_cbcondz_opposite.run();
                    CHECK_EQ(overwrite_location, branch.GetOffsetLocation());
                    J(offset);
                    break;
                }

                // TODO: avoid usage of TMP register (make it explicit)
                // Long branches.
                case kLongCondCBranch:
                    emit_cbcondz_opposite.run();
                    emit_auipc_and_next.accept(TMP, (int short_offset) ->
                            Jalr(Zero, TMP, short_offset));
                    break;
                case kLongCondBranch:
                    EmitBcond(RV64Branch.OppositeCondition(condition), lhs, rhs, branch.GetLength());
                    // fall through
                case kLongUncondBranch:
                    emit_auipc_and_next.accept(TMP, (int short_offset) ->
                            Jalr(Zero, TMP, short_offset));
                    break;
                case kLongCall:
                    CHECK(lhs != Zero);
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            Jalr(lhs, lhs, short_offset));
                    break;

                // label.
                case kLabel:
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            Addi(lhs, lhs, short_offset));
                    break;

                // literals.
                case kLiteral:
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            Lw(lhs, lhs, short_offset));
                    break;
                case kLiteralUnsigned:
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            Lwu(lhs, lhs, short_offset));
                    break;
                case kLiteralLong:
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            Ld(lhs, lhs, short_offset));
                    break;
                case kLiteralFloat:
                    CHECK(lhs != Zero);
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            FLw(branch.GetFRegister(), lhs, short_offset));
                    break;
                case kLiteralDouble:
                    CHECK(lhs != Zero);
                    emit_auipc_and_next.accept(lhs, (int short_offset) ->
                            FLd(branch.GetFRegister(), lhs, short_offset));
                    break;
            }
        }
        CHECK_EQ(overwrite_location, branch.GetEndLocation());
        CHECK_LE(branch.GetLength(), (RV64Branch.kMaxBranchLength));
    }

    private void EmitBranches() {
        // Switch from appending instructions at the end of the buffer to overwriting
        // existing instructions (branch placeholders) in the buffer.
        overwriting = true;
        for (var branch : branches_) {
            EmitBranch(branch);
        }
        overwriting = false;
    }

    private void FinalizeLabeledBranch(RV64Label label) {
        int alignment = IsExtensionEnabled(RV64Extension.kZca) ? 2 : 4;
        int branch_id = branches_.size() - 1;
        RV64Branch this_branch = branches_.get(branch_id);
        int branch_length = this_branch.GetLength();
        assert (isAligned(branch_length, alignment));
        int length = branch_length / alignment;
        if (!label.isBound()) {
            // Branch forward (to a following label), distance is unknown.
            // The first branch forward will contain 0, serving as the terminator of
            // the list of forward-reaching branches.
            this_branch.LinkToList(label.position);
            // Now make the label object point to this branch
            // (this forms a linked list of branches preceding this label).
            label.linkTo(branch_id);
        }
        // Reserve space for the branch.
        for (; length != 0; --length) {
            if (alignment == 2) {
                Emit16(0);
            } else {
                Emit32(0);
            }
        }
    }

    private void Bcond(RV64Label label, boolean is_bare, BranchCondition condition, RV64XRegister lhs, RV64XRegister rhs) {
        // If lhs = rhs, this can be a NOP.
        if (RV64Branch.IsNop(condition, lhs, rhs)) {
            return;
        }
        if (RV64Branch.IsUncond(condition, lhs, rhs)) {
            Buncond(label, Zero, is_bare);
            return;
        }

        int target = label.isBound() ? GetLabelLocation(label) : RV64Branch.kUnresolved;
        branches_.add(new RV64Branch(size(), target, condition, lhs, rhs,
                is_bare, IsExtensionEnabled(RV64Extension.kZca)));
        FinalizeLabeledBranch(label);
    }

    private void Buncond(RV64Label label, RV64XRegister rd, boolean is_bare) {
        int target = label.isBound() ? GetLabelLocation(label) : RV64Branch.kUnresolved;
        branches_.add(new RV64Branch(size(), target, rd, is_bare,
                IsExtensionEnabled(RV64Extension.kZca)));
        FinalizeLabeledBranch(label);
    }

    private void LoadLiteral(RV64Literal literal, RV64XRegister rd, Type literal_type) {
        // TODO: what if literal can be loaded as immediate?
        RV64Label label = literal.getLabel();
        CHECK(!label.isBound());
        branches_.add(new RV64Branch(size(), RV64Branch.kUnresolved, rd, literal_type));
        FinalizeLabeledBranch(label);
    }

    private void LoadLiteral(RV64Literal literal, RV64XRegister tmp, RV64FRegister rd, Type literal_type) {
        // TODO: what if literal can be loaded as immediate?
        RV64Label label = literal.getLabel();
        CHECK(!label.isBound());
        branches_.add(new RV64Branch(size(), RV64Branch.kUnresolved, tmp, rd, literal_type));
        FinalizeLabeledBranch(label);
    }

    // This method is used to adjust the base register and offset pair for
    // a load/store when the offset doesn't fit into 12-bit signed integer.
    private void AdjustBaseAndOffset(RV64XRegister tmp, RV64XRegister[] base, int[] offset) {
        if (isInt(12, offset[0])) {
            return;
        }
        CHECK(tmp != Zero);

        final int kPositiveOffsetMaxSimpleAdjustment = 0x7ff;
        final int kHighestOffsetForSimpleAdjustment = 2 * kPositiveOffsetMaxSimpleAdjustment;
        final int kPositiveOffsetSimpleAdjustmentAligned8 =
                roundDown(kPositiveOffsetMaxSimpleAdjustment, 8);
        final int kPositiveOffsetSimpleAdjustmentAligned4 =
                roundDown(kPositiveOffsetMaxSimpleAdjustment, 4);
        final int kNegativeOffsetSimpleAdjustment = -0x800;
        final int kLowestOffsetForSimpleAdjustment = 2 * kNegativeOffsetSimpleAdjustment;

        if (offset[0] >= 0 && offset[0] <= kHighestOffsetForSimpleAdjustment) {
            // Make the adjustment 8-byte aligned (0x7f8) except for offsets that cannot be reached
            // with this adjustment, then try 4-byte alignment, then just half of the offset.
            int adjustment = isInt(12, offset[0] - kPositiveOffsetSimpleAdjustmentAligned8)
                    ? kPositiveOffsetSimpleAdjustmentAligned8
                    : isInt(12, offset[0] - kPositiveOffsetSimpleAdjustmentAligned4)
                    ? kPositiveOffsetSimpleAdjustmentAligned4
                    : offset[0] / 2;
            CHECK(isInt(12, adjustment));
            Addi(tmp, base[0], adjustment);
            offset[0] -= adjustment;
        } else if (offset[0] < 0 && offset[0] >= kLowestOffsetForSimpleAdjustment) {
            Addi(tmp, base[0], kNegativeOffsetSimpleAdjustment);
            offset[0] -= kNegativeOffsetSimpleAdjustment;
        } else if (offset[0] >= 0x7ffff800) {
            // Support even large offsets outside the range supported by `SplitOffset()`.
            LoadConst32(tmp, offset[0]);
            Add(tmp, tmp, base[0]);
            offset[0] = 0;
        } else {
            var pair = SplitOffset(offset[0]);
            Lui(tmp, pair.imm20);
            Add(tmp, tmp, base[0]);
            offset[0] = pair.short_offset;
        }
        base[0] = tmp;
    }

    private interface XXI {
        void apply(RV64XRegister x1, RV64XRegister x2, int i);
    }

    private void LoadFromOffset(XXI insn, RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        // tmp may be equal to rd
        CHECK(tmp != rs1);
        RV64XRegister[] rs1_arr = {rs1};
        int[] offset_arr = {offset};
        AdjustBaseAndOffset(tmp, rs1_arr, offset_arr);
        insn.apply(rd, rs1_arr[0], offset_arr[0]);
    }

    private void StoreToOffset(XXI insn, RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset) {
        CHECK(tmp != rs1);
        CHECK(tmp != rs2);
        RV64XRegister[] rs1_arr = {rs1};
        int[] offset_arr = {offset};
        AdjustBaseAndOffset(tmp, rs1_arr, offset_arr);
        insn.apply(rs2, rs1_arr[0], offset_arr[0]);
    }

    private interface FXI {
        void apply(RV64FRegister f1, RV64XRegister x2, int i);
    }

    private void FLoadFromOffset(FXI insn, RV64XRegister tmp, RV64FRegister rd, RV64XRegister rs1, int offset) {
        CHECK(tmp != rs1);
        RV64XRegister[] rs1_arr = {rs1};
        int[] offset_arr = {offset};
        AdjustBaseAndOffset(tmp, rs1_arr, offset_arr);
        insn.apply(rd, rs1_arr[0], offset_arr[0]);
    }

    private void FStoreToOffset(FXI insn, RV64XRegister tmp, RV64FRegister rs2, RV64XRegister rs1, int offset) {
        CHECK(tmp != rs1);
        RV64XRegister[] rs1_arr = {rs1};
        int[] offset_arr = {offset};
        AdjustBaseAndOffset(tmp, rs1_arr, offset_arr);
        insn.apply(rs2, rs1_arr[0], offset_arr[0]);
    }

    private interface XXLX {
        void apply(RV64XRegister x1, RV64XRegister x2, long l, RV64XRegister x3);
    }

    private void AddConstImpl(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1,
                              long value, XXI addi, XXLX add_large) {
        CHECK(tmp != rs1);
        CHECK(tmp != SP); // TODO: Why?

        if (isLInt(12, value)) {
            addi.apply(rd, rs1, (int) value);
            return;
        }
        CHECK(tmp != Zero);

        final int kPositiveValueSimpleAdjustment = 0x7ff;
        final int kHighestValueForSimpleAdjustment = 2 * kPositiveValueSimpleAdjustment;
        final int kNegativeValueSimpleAdjustment = -0x800;
        final int kLowestValueForSimpleAdjustment = 2 * kNegativeValueSimpleAdjustment;

        if (value >= 0 && value <= kHighestValueForSimpleAdjustment) {
            addi.apply(tmp, rs1, kPositiveValueSimpleAdjustment);
            addi.apply(rd, tmp, (int) (value - kPositiveValueSimpleAdjustment));
        } else if (value < 0 && value >= kLowestValueForSimpleAdjustment) {
            addi.apply(tmp, rs1, kNegativeValueSimpleAdjustment);
            addi.apply(rd, tmp, (int) (value - kNegativeValueSimpleAdjustment));
        } else {
            add_large.apply(rd, rs1, value, tmp);
        }
    }

    //_____________________________ RV64 VARIANTS extension _____________________________//

    //______________________________ RV64 "I" Instructions ______________________________//

    // LUI/AUIPC (RV32I, with sign-extension on RV64I), opcode = 0x17, 0x37

    public void Lui(RV64XRegister rd, int imm20) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero && rd != SP && IsImmCLuiEncodable(imm20)) {
                CLui(rd, imm20);
                return;
            }
        }

        EmitU(imm20, rd, 0x37);
    }

    public void Auipc(RV64XRegister rd, int imm20) {
        EmitU(imm20, rd, 0x17);
    }

    // Jump instructions (RV32I), opcode = 0x67, 0x6f

    public void Jal(RV64XRegister rd, int offset) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == Zero && isInt(12, offset)) {
                CJ(offset);
                return;
            }
            // Note: `c.jal` is RV32-only.
        }

        EmitJ(offset, rd, 0x6F);
    }

    public void Jalr(RV64XRegister rd, RV64XRegister rs1, int offset) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == RA && rs1 != Zero && offset == 0) {
                CJalr(rs1);
                return;
            } else if (rd == Zero && rs1 != Zero && offset == 0) {
                CJr(rs1);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x0, rd.index(), 0x67);
    }

    // Branch instructions, opcode = 0x63 (subfunc from 0x0 ~ 0x7), 0x67, 0x6f

    public void Beq(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rs2 == Zero && rs1.isShortReg() && isInt(9, offset)) {
                CBeqz(rs1, offset);
                return;
            } else if (rs1 == Zero) {
                if (rs2.isShortReg() && isInt(9, offset)) {
                    CBeqz(rs2, offset);
                    return;
                }
            }
        }

        EmitB(offset, rs2, rs1, 0x0, 0x63);
    }

    public void Bne(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rs2 == Zero && rs1.isShortReg() && isInt(9, offset)) {
                CBnez(rs1, offset);
                return;
            } else if (rs1 == Zero) {
                if (rs2.isShortReg() && isInt(9, offset)) {
                    CBnez(rs2, offset);
                    return;
                }
            }
        }

        EmitB(offset, rs2, rs1, 0x1, 0x63);
    }

    public void Blt(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x4, 0x63);
    }

    public void Bge(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x5, 0x63);
    }

    public void Bltu(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x6, 0x63);
    }

    public void Bgeu(RV64XRegister rs1, RV64XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x7, 0x63);
    }

    // Load instructions (RV32I+RV64I): opcode = 0x03, funct3 from 0x0 ~ 0x6

    public void Lb(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);
        EmitI(offset, rs1.index(), 0x0, rd.index(), 0x03);
    }

    public void Lh(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(2, offset) && isAligned(offset, 2)) {
                    CLh(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x1, rd.index(), 0x03);
    }

    public void Lw(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero && rs1 == SP && Utils.isUInt(8, offset) && isAligned(offset, 4)) {
                CLwsp(rd, offset);
                return;
            } else if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(7, offset) && isAligned(offset, 4)) {
                    CLw(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x03);
    }

    public void Ld(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero && rs1 == SP && Utils.isUInt(9, offset) && isAligned(offset, 8)) {
                CLdsp(rd, offset);
                return;
            } else if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(8, offset) && isAligned(offset, 8)) {
                    CLd(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x03);
    }

    public void Lbu(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(2, offset)) {
                    CLbu(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x4, rd.index(), 0x03);
    }

    public void Lhu(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(2, offset) && isAligned(offset, 2)) {
                    CLhu(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x5, rd.index(), 0x03);
    }

    public void Lwu(RV64XRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);
        EmitI(offset, rs1.index(), 0x6, rd.index(), 0x3);
    }

    // Store instructions (RV32I+RV64I): opcode = 0x23, funct3 from 0x0 ~ 0x3

    public void Sb(RV64XRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rs2.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(2, offset)) {
                    CSb(rs2, rs1, offset);
                    return;
                }
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x0, 0x23);
    }

    public void Sh(RV64XRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rs2.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(2, offset) && isAligned(offset, 2)) {
                    CSh(rs2, rs1, offset);
                    return;
                }
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x1, 0x23);
    }

    public void Sw(RV64XRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rs1 == SP && Utils.isUInt(8, offset) && isAligned(offset, 4)) {
                CSwsp(rs2, offset);
                return;
            } else if (rs2.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(7, offset) && isAligned(offset, 4)) {
                    CSw(rs2, rs1, offset);
                    return;
                }
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x2, 0x23);
    }

    public void Sd(RV64XRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rs1 == SP && Utils.isUInt(9, offset) && isAligned(offset, 8)) {
                CSdsp(rs2, offset);
                return;
            } else if (rs2.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(8, offset) && isAligned(offset, 8)) {
                    CSd(rs2, rs1, offset);
                    return;
                }
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x3, 0x23);
    }

    // IMM ALU instructions (RV32I): opcode = 0x13, funct3 from 0x0 ~ 0x7

    public void Addi(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero) {
                if (rs1 == Zero && isInt(6, imm12)) {
                    CLi(rd, imm12);
                    return;
                } else if (imm12 != 0) {
                    if (rd == rs1) {
                        // We're testing against clang's assembler and therefore
                        // if both c.addi and c.addi16sp are viable, we use the c.addi just like clang.
                        if (isInt(6, imm12)) {
                            CAddi(rd, imm12);
                            return;
                        } else if (rd == SP && isInt(10, imm12) && isAligned(imm12, 16)) {
                            CAddi16Sp(imm12);
                            return;
                        }
                    } else if (rd.isShortReg() && rs1 == SP && Utils.isUInt(10, imm12) && isAligned(imm12, 4)) {
                        CAddi4Spn(rd, imm12);
                        return;
                    }
                } else if (rs1 != Zero) {
                    CMv(rd, rs1);
                    return;
                }
            } else if (rd == rs1 && imm12 == 0) {
                CNop();
                return;
            }
        }

        EmitI(imm12, rs1.index(), 0x0, rd.index(), 0x13);
    }

    public void Slti(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x2, rd.index(), 0x13);
    }

    public void Sltiu(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x3, rd.index(), 0x13);
    }

    public void Xori(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rd == rs1) {
                if (rd.isShortReg() && imm12 == -1) {
                    CNot(rd);
                    return;
                }
            }
        }

        EmitI(imm12, rs1.index(), 0x4, rd.index(), 0x13);
    }

    public void Ori(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x6, rd.index(), 0x13);
    }

    public void Andi(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1) {
                if (rd.isShortReg() && isInt(6, imm12)) {
                    CAndi(rd, imm12);
                    return;
                }
            }
        }

        EmitI(imm12, rs1.index(), 0x7, rd.index(), 0x13);
    }

    // 0x1 Split: 0x0(6b) + imm12(6b)
    public void Slli(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1 && rd != Zero && shamt != 0) {
                CSlli(rd, shamt);
                return;
            }
        }

        EmitI6(0x0, shamt, rs1, 0x1, rd, 0x13);
    }

    // 0x5 Split: 0x0(6b) + imm12(6b)
    public void Srli(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1) {
                if (rd.isShortReg() && shamt != 0) {
                    CSrli(rd, shamt);
                    return;
                }
            }
        }

        EmitI6(0x0, shamt, rs1, 0x5, rd, 0x13);
    }

    // 0x5 Split: 0x10(6b) + imm12(6b)
    public void Srai(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1) {
                if (rd.isShortReg() && shamt != 0) {
                    CSrai(rd, shamt);
                    return;
                }
            }
        }

        EmitI6(0x10, shamt, rs1, 0x5, rd, 0x13);
    }

    // ALU instructions (RV32I): opcode = 0x33, funct3 from 0x0 ~ 0x7

    public void Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero) {
                if (rs1 != Zero || rs2 != Zero) {
                    if (rs1 == Zero) {
                        CHECK_NE(rs2.index(), Zero.index());
                        CMv(rd, rs2);
                        return;
                    } else if (rs2 == Zero) {
                        CHECK_NE(rs1.index(), Zero.index());
                        CMv(rd, rs1);
                        return;
                    } else if (rd == rs1) {
                        CHECK_NE(rs2.index(), Zero.index());
                        CAdd(rd, rs2);
                        return;
                    } else if (rd == rs2) {
                        CHECK_NE(rs1.index(), Zero.index());
                        CAdd(rd, rs1);
                        return;
                    }
                } else {
                    CLi(rd, 0);
                    return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Sub(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1 && rd.isShortReg()) {
                if (rs2.isShortReg()) {
                    CSub(rd, rs2);
                    return;
                }
            }
        }

        EmitR(0x20, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Slt(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x02, rd.index(), 0x33);
    }

    public void Sltu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x03, rd.index(), 0x33);
    }

    public void Xor(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd.isShortReg()) {
                if (rd == rs1 && rs2.isShortReg()) {
                    CXor(rd, rs2);
                    return;
                } else if (rd == rs2) {
                    if (rs1.isShortReg()) {
                        CXor(rd, rs1);
                        return;
                    }
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x04, rd.index(), 0x33);
    }

    public void Or(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd.isShortReg()) {
                if (rd == rs1 && rs2.isShortReg()) {
                    COr(rd, rs2);
                    return;
                } else if (rd == rs2) {
                    if (rs1.isShortReg()) {
                        COr(rd, rs1);
                        return;
                    }
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x06, rd.index(), 0x33);
    }

    public void And(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd.isShortReg()) {
                if (rd == rs1 && rs2.isShortReg()) {
                    CAnd(rd, rs2);
                    return;
                } else if (rd == rs2) {
                    if (rs1.isShortReg()) {
                        CAnd(rd, rs1);
                        return;
                    }
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x07, rd.index(), 0x33);
    }

    public void Sll(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x01, rd.index(), 0x33);
    }

    public void Srl(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x05, rd.index(), 0x33);
    }

    public void Sra(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x20, rs2.index(), rs1.index(), 0x05, rd.index(), 0x33);
    }

    // 32bit Imm ALU instructions (RV64I): opcode = 0x1b, funct3 from 0x0, 0x1, 0x5

    public void Addiw(RV64XRegister rd, RV64XRegister rs1, int imm12) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd != Zero && isInt(6, imm12)) {
                if (rd == rs1) {
                    CAddiw(rd, imm12);
                    return;
                } else if (rs1 == Zero) {
                    CLi(rd, imm12);
                    return;
                }
            }
        }

        EmitI(imm12, rs1.index(), 0x0, rd.index(), 0x1b);
    }

    public void Slliw(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x0, shamt, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Srliw(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x0, shamt, rs1.index(), 0x5, rd.index(), 0x1b);
    }

    public void Sraiw(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x20, shamt, rs1.index(), 0x5, rd.index(), 0x1b);
    }

    // 32bit ALU instructions (RV64I): opcode = 0x3b, funct3 from 0x0 ~ 0x7

    public void Addw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd.isShortReg()) {
                if (rd == rs1 && rs2.isShortReg()) {
                    CAddw(rd, rs2);
                    return;
                } else if (rd == rs2) {
                    if (rs1.isShortReg()) {
                        CAddw(rd, rs1);
                        return;
                    }
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Subw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            if (rd == rs1 && rd.isShortReg()) {
                if (rs2.isShortReg()) {
                    CSubw(rd, rs2);
                    return;
                }
            }
        }

        EmitR(0x20, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Sllw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x1, rd.index(), 0x3b);
    }

    public void Srlw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Sraw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        EmitR(0x20, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    // Environment call and breakpoint (RV32I), opcode = 0x73

    public void Ecall() {
        EmitI(0x0, 0x0, 0x0, 0x0, 0x73);
    }

    public void Ebreak() {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            CEbreak();
            return;
        }

        EmitI(0x1, 0x0, 0x0, 0x0, 0x73);
    }

    // Fence instruction (RV32I): opcode = 0xf, funct3 = 0

    public void Fence(RV64FenceType pred, RV64FenceType succ) {
        EmitI(/* normal fence */ pred.value() << 4 | succ.value(), 0x0, 0x0, 0x0, 0xf);
    }

    public void FenceTso() {
        final int kPred = kFenceNNRW.value();
        final int kSucc = kFenceNNRW.value();
        EmitI(ToInt12(/* TSO fence */ 0x8 << 8 | kPred << 4 | kSucc), 0x0, 0x0, 0x0, 0xf);
    }

    //______________________________ RV64 "I" Instructions  END ______________________________//

    //_________________________ RV64 "Zifencei" Instructions  START __________________________//

    // "Zifencei" Standard Extension, opcode = 0xf, funct3 = 1

    public void FenceI() {
        AssertExtensionsEnabled(RV64Extension.kZifencei);
        EmitI(0x0, 0x0, 0x1, 0x0, 0xf);
    }

    //__________________________ RV64 "Zifencei" Instructions  END ___________________________//

    //_____________________________ RV64 "M" Instructions  START _____________________________//

    // RV32M Standard Extension: opcode = 0x33, funct3 from 0x0 ~ 0x7

    public void Mul(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);

        if (IsExtensionEnabled(RV64Extension.kZcb)) {
            if (rd.isShortReg()) {
                if (rd == rs1 && rs2.isShortReg()) {
                    CMul(rd, rs2);
                    return;
                } else if (rd == rs2) {
                    if (rs1.isShortReg()) {
                        CMul(rd, rs1);
                        return;
                    }
                }
            }
        }

        EmitR(0x1, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Mulh(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Mulhsu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x2, rd.index(), 0x33);
    }

    public void Mulhu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x3, rd.index(), 0x33);
    }

    public void Div(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Divu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Rem(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Remu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    // RV64M Standard Extension: opcode = 0x3b, funct3 0x0 and from 0x4 ~ 0x7

    public void Mulw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Divw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x4, rd.index(), 0x3b);
    }

    public void Divuw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Remw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x6, rd.index(), 0x3b);
    }

    public void Remuw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x7, rd.index(), 0x3b);
    }

    //______________________________ RV64 "M" Instructions  END ______________________________//

    //_____________________________ RV64 "A" Instructions  START _____________________________//

    public void LrW(RV64XRegister rd, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        CHECK(aqrl != RV64AqRl.kRelease);
        EmitR4(0x2, aqrl.value(), 0x0, rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void LrD(RV64XRegister rd, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        CHECK(aqrl != RV64AqRl.kRelease);
        EmitR4(0x2, aqrl.value(), 0x0, rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void ScW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        CHECK(aqrl != RV64AqRl.kAcquire);
        EmitR4(0x3, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void ScD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        CHECK(aqrl != RV64AqRl.kAcquire);
        EmitR4(0x3, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoSwapW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x1, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoSwapD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x1, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoAddW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x0, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoAddD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x0, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoXorW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x4, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoXorD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x4, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoAndW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0xc, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoAndD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0xc, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoOrW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x8, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoOrD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x8, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMinW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x10, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMinD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x10, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMaxW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x14, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMaxD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x14, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMinuW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x18, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMinuD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x18, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMaxuW(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x1c, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMaxuD(RV64XRegister rd, RV64XRegister rs2, RV64XRegister rs1, RV64AqRl aqrl) {
        AssertExtensionsEnabled(RV64Extension.kA);
        EmitR4(0x1c, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    //_____________________________ RV64 "A" Instructions  END _______________________________//

    //___________________________ RV64 "Zicsr" Instructions  START ___________________________//

    // "Zicsr" Standard Extension, opcode = 0x73, funct3 from 0x1 ~ 0x3 and 0x5 ~ 0x7

    public void Csrrw(RV64XRegister rd, int csr, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x1, rd.index(), 0x73);
    }

    public void Csrrs(RV64XRegister rd, int csr, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x73);
    }

    public void Csrrc(RV64XRegister rd, int csr, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x73);
    }

    public void Csrrwi(RV64XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x5, rd.index(), 0x73);
    }

    public void Csrrsi(RV64XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x6, rd.index(), 0x73);
    }

    public void Csrrci(RV64XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(RV64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x7, rd.index(), 0x73);
    }

    //____________________________ RV64 "Zicsr" Instructions  END ____________________________//

    //_____________________________ RV64 "FD" Instructions  START ____________________________//

    // FP load/store instructions (RV32F+RV32D): opcode = 0x07, 0x27

    public void FLw(RV64FRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kF);
        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x07);
    }

    public void FLd(RV64FRegister rd, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kD);

        if (IsExtensionEnabled(RV64Extension.kZcd)) {
            if (rs1 == SP && Utils.isUInt(9, offset) && isAligned(offset, 8)) {
                CFLdsp(rd, offset);
                return;
            } else if (rd.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(8, offset) && isAligned(offset, 8)) {
                    CFLd(rd, rs1, offset);
                    return;
                }
            }
        }

        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x07);
    }

    public void FSw(RV64FRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kF);
        EmitS(offset, rs2.index(), rs1.index(), 0x2, 0x27);
    }

    public void FSd(RV64FRegister rs2, RV64XRegister rs1, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kD);

        if (IsExtensionEnabled(RV64Extension.kZcd)) {
            if (rs1 == SP && Utils.isUInt(9, offset) && isAligned(offset, 8)) {
                CFSdsp(rs2, offset);
                return;
            } else if (rs2.isShortReg()) {
                if (rs1.isShortReg() && Utils.isUInt(8, offset) && isAligned(offset, 8)) {
                    CFSd(rs2, rs1, offset);
                    return;
                }
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x3, 0x27);
    }

    // FP FMA instructions (RV32F+RV32D): opcode = 0x43, 0x47, 0x4b, 0x4f

    public void FMAddS(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x43);
    }

    public void FMAddD(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x43);
    }

    public void FMSubS(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x47);
    }

    public void FMSubD(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x47);
    }

    public void FNMSubS(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4b);
    }

    public void FNMSubD(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4b);
    }

    public void FNMAddS(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4f);
    }

    public void FNMAddD(
            RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FRegister rs3, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4f);
    }

    // Simple FP instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b0XXXX0D

    public void FAddS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FAddD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSubS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x4, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSubD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x5, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FMulS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x8, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FMulD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x9, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FDivS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0xc, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FDivD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0xd, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSqrtS(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x2c, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSqrtD(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x2d, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSgnjS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FSgnjD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FSgnjnS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FSgnjnD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FSgnjxS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FSgnjxD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FMinS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x14, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMinD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x15, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMaxS(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x14, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FMaxD(RV64FRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        EmitR(0x15, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
        AssertExtensionsEnabled(RV64Extension.kD);
    }

    public void FCvtSD(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF, RV64Extension.kD);
        EmitR(0x20, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDS(RV64FRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF, RV64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x21, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    // FP compare instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b101000D

    public void FEqS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FEqD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FLtS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FLtD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FLeS(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FLeD(RV64XRegister rd, RV64FRegister rs1, RV64FRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    // FP conversion instructions (RV32F+RV32D+RV64F+RV64D): opcode = 0x53, funct7 = 0b110X00D

    public void FCvtWS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x60, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x61, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWuS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x60, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWuD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x61, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x60, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x61, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLuS(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x60, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLuD(RV64XRegister rd, RV64FRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x61, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSW(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x68, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDW(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x69, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSWu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x68, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDWu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x69, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSL(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x68, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDL(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x69, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSLu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x68, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDLu(RV64FRegister rd, RV64XRegister rs1, RV64FPRoundingMode frm) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x69, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    // FP move instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x0, funct7 = 0b111X00D

    public void FMvXW(RV64XRegister rd, RV64FRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x70, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvXD(RV64XRegister rd, RV64FRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x71, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvWX(RV64FRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x78, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvDX(RV64FRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x79, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    // FP classify instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x1, funct7 = 0b111X00D

    public void FClassS(RV64XRegister rd, RV64FRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kF);
        EmitR(0x70, 0x0, rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FClassD(RV64XRegister rd, RV64FRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kD);
        EmitR(0x71, 0x0, rs1.index(), 0x1, rd.index(), 0x53);
    }

    //_____________________________ RV64 "FD" Instructions  END ______________________________//

    //______________________________ RV64 "C" Instructions  START ____________________________//

    public void CLwsp(RV64XRegister rd, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        int imm6 = ExtractOffset52_76(offset);
        EmitCI(0b010, rd.index(), imm6, 0b10);
    }

    public void CLdsp(RV64XRegister rd, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        int imm6 = ExtractOffset53_86(offset);
        EmitCI(0b011, rd.index(), imm6, 0b10);
    }

    public void CFLdsp(RV64FRegister rd, int offset) {
        AssertExtensionsEnabled(
                RV64Extension.kLoadStore, RV64Extension.kZcd, RV64Extension.kD);
        int imm6 = ExtractOffset53_86(offset);
        EmitCI(0b001, rd.index(), imm6, 0b10);
    }

    public void CSwsp(RV64XRegister rs2, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int offset6 = ExtractOffset52_76(offset);
        EmitCSS(0b110, offset6, rs2.index(), 0b10);
    }

    public void CSdsp(RV64XRegister rs2, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int offset6 = ExtractOffset53_86(offset);
        EmitCSS(0b111, offset6, rs2.index(), 0b10);
    }

    public void CFSdsp(RV64FRegister rs2, int offset) {
        AssertExtensionsEnabled(
                RV64Extension.kLoadStore, RV64Extension.kZcd, RV64Extension.kD);
        int offset6 = ExtractOffset53_86(offset);
        EmitCSS(0b101, offset6, rs2.index(), 0b10);
    }

    public void CLw(RV64XRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int imm5 = ExtractOffset52_6(offset);
        EmitCM(0b010, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CLd(RV64XRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b011, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CFLd(RV64FRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(
                RV64Extension.kLoadStore, RV64Extension.kZcd, RV64Extension.kD);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b001, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CSw(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int imm5 = ExtractOffset52_6(offset);
        EmitCM(0b110, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CSd(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZca);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b111, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CFSd(RV64FRegister rs2_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(
                RV64Extension.kLoadStore, RV64Extension.kZcd, RV64Extension.kD);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b101, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CLi(RV64XRegister rd, int imm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        CHECK(isInt(6, imm));
        int imm6 = EncodeInt6(imm);
        EmitCI(0b010, rd.index(), imm6, 0b01);
    }

    public void CLui(RV64XRegister rd, int nzimm6) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        CHECK_NE(rd.index(), SP.index());
        CHECK(IsImmCLuiEncodable(nzimm6));
        EmitCI(0b011, rd.index(), nzimm6 & MaskLeastSignificant(6), 0b01);
    }

    public void CAddi(RV64XRegister rd, int nzimm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        CHECK_NE(nzimm, 0);
        int imm6 = EncodeInt6(nzimm);
        EmitCI(0b000, rd.index(), imm6, 0b01);
    }

    public void CAddiw(RV64XRegister rd, int imm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        int imm6 = EncodeInt6(imm);
        EmitCI(0b001, rd.index(), imm6, 0b01);
    }

    public void CAddi16Sp(int nzimm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(nzimm, 0);
        CHECK(isAligned(nzimm, 16));
        CHECK(isInt(10, nzimm));

        // nzimm[9]
        int imms1 = BitFieldExtract(nzimm, 9, 1);
        // nzimm[4|6|8:7|5]
        int imms0 = (BitFieldExtract(nzimm, 4, 1) << 4) |
                (BitFieldExtract(nzimm, 6, 1) << 3) |
                (BitFieldExtract(nzimm, 7, 2) << 1) |
                BitFieldExtract(nzimm, 5, 1);

        int imm6 = BitFieldInsert(imms0, imms1, 5, 1);
        EmitCI(0b011, SP.index(), imm6, 0b01);
    }

    public void CAddi4Spn(RV64XRegister rd_s, int nzuimm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(nzuimm, 0);
        CHECK(isAligned(nzuimm, 4));
        CHECK(Utils.isUInt(10, nzuimm));

        // nzuimm[5:4|9:6|2|3]
        int uimm = (BitFieldExtract(nzuimm, 4, 2) << 6) |
                (BitFieldExtract(nzuimm, 6, 4) << 2) |
                (BitFieldExtract(nzuimm, 2, 1) << 1) |
                BitFieldExtract(nzuimm, 3, 1);

        EmitCIW(0b000, uimm, rd_s.index(), 0b00);
    }

    public void CSlli(RV64XRegister rd, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK_NE(rd.index(), Zero.index());
        EmitCI(0b000, rd.index(), shamt, 0b10);
    }

    public void CSrli(RV64XRegister rd_s, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK(Utils.isUInt(6, shamt));
        EmitCBArithmetic(0b100, 0b00, shamt, rd_s, 0b01);
    }

    public void CSrai(RV64XRegister rd_s, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK(Utils.isUInt(6, shamt));
        EmitCBArithmetic(0b100, 0b01, shamt, rd_s, 0b01);
    }

    public void CAndi(RV64XRegister rd_s, int imm) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK(isInt(6, imm));
        EmitCBArithmetic(0b100, 0b10, imm, rd_s, 0b01);
    }

    public void CMv(RV64XRegister rd, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        CHECK_NE(rs2.index(), Zero.index());
        EmitCR(0b1000, rd, rs2, 0b10);
    }

    public void CAdd(RV64XRegister rd, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rd.index(), Zero.index());
        CHECK_NE(rs2.index(), Zero.index());
        EmitCR(0b1001, rd, rs2, 0b10);
    }

    public void CAnd(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b11, rs2_s, 0b01);
    }

    public void COr(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b10, rs2_s, 0b01);
    }

    public void CXor(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b01, rs2_s, 0b01);
    }

    public void CSub(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b00, rs2_s, 0b01);
    }

    public void CAddw(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100111, rd_s, 0b01, rs2_s, 0b01);
    }

    public void CSubw(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCAReg(0b100111, rd_s, 0b00, rs2_s, 0b01);
    }

    // "Zcb" Standard Extension, part of "C", opcode = 0b00, 0b01, funct3 = 0b100.

    public void CLbu(RV64XRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZcb);
        EmitCAReg(0b100000, rs1_s, EncodeOffset0_1(offset), rd_s, 0b00);
    }

    public void CLhu(RV64XRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZcb);
        CHECK(Utils.isUInt(2, offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100001, rs1_s, BitFieldExtract(offset, 1, 1), rd_s, 0b00);
    }

    public void CLh(RV64XRegister rd_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZcb);
        CHECK(Utils.isUInt(2, offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100001, rs1_s, 0b10 | BitFieldExtract(offset, 1, 1), rd_s, 0b00);
    }

    public void CSb(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZcb);
        EmitCAReg(0b100010, rs1_s, EncodeOffset0_1(offset), rs2_s, 0b00);
    }

    public void CSh(RV64XRegister rs2_s, RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kZcb);
        CHECK(Utils.isUInt(2, offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100011, rs1_s, BitFieldExtract(offset, 1, 1), rs2_s, 0b00);
    }

    public void CZextB(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b000, 0b01);
    }

    public void CSextB(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZbb, RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b001, 0b01);
    }

    public void CZextH(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZbb, RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b010, 0b01);
    }

    public void CSextH(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZbb, RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b011, 0b01);
    }

    public void CZextW(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZba, RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b100, 0b01);
    }

    public void CNot(RV64XRegister rd_rs1_s) {
        AssertExtensionsEnabled(RV64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b101, 0b01);
    }

    public void CMul(RV64XRegister rd_s, RV64XRegister rs2_s) {
        AssertExtensionsEnabled(RV64Extension.kM, RV64Extension.kZcb);
        EmitCAReg(0b100111, rd_s, 0b10, rs2_s, 0b01);
    }

    public void CJ(int offset) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCJ(0b101, offset, 0b01);
    }

    public void CJr(RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rs1.index(), Zero.index());
        EmitCR(0b1000, rs1, Zero, 0b10);
    }

    public void CJalr(RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        CHECK_NE(rs1.index(), Zero.index());
        EmitCR(0b1001, rs1, Zero, 0b10);
    }

    public void CBeqz(RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCBBranch(0b110, offset, rs1_s, 0b01);
    }

    public void CBnez(RV64XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCBBranch(0b111, offset, rs1_s, 0b01);
    }

    public void CEbreak() {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCR(0b1001, Zero, Zero, 0b10);
    }

    public void CNop() {
        AssertExtensionsEnabled(RV64Extension.kZca);
        EmitCI(0b000, Zero.index(), 0, 0b01);
    }

    public void CUnimp() {
        AssertExtensionsEnabled(RV64Extension.kZca);
        Emit16(0x0);
    }

    //_____________________________ RV64 "C" Instructions  END _______________________________//

    //_____________________________ RV64 "Zba" Instructions  START ___________________________//

    public void AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x4, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Sh1Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x33);
    }

    public void Sh1AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x3b);
    }

    public void Sh2Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Sh2AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x4, rd.index(), 0x3b);
    }

    public void Sh3Add(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Sh3AddUw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x6, rd.index(), 0x3b);
    }

    public void SlliUw(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZba);
        EmitI6(0x2, shamt, rs1, 0x1, rd, 0x1b);
    }

    //_____________________________ RV64 "Zba" Instructions  END _____________________________//

    //_____________________________ RV64 "Zbb" Instructions  START ___________________________//

    public void Andn(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    public void Orn(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Xnor(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Clz(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x0, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Clzw(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x0, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Ctz(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x1, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Ctzw(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x1, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Cpop(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x2, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Cpopw(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x2, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Min(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Minu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Max(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Maxu(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    public void Rol(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Rolw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x1, rd.index(), 0x3b);
    }

    public void Ror(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Rorw(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Rori(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        CHECK_LT(shamt, 64);
        EmitI6(0x18, shamt, rs1, 0x5, rd, 0x13);
    }

    public void Roriw(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        CHECK_LT(shamt, 32);
        EmitI6(0x18, shamt, rs1, 0x5, rd, 0x1b);
    }

    public void OrcB(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x14, 0x7, rs1.index(), 0x5, rd.index(), 0x13);
    }

    public void Rev8(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x35, 0x18, rs1.index(), 0x5, rd.index(), 0x13);
    }

    public void ZbbSextB(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x4, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void ZbbSextH(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x30, 0x5, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void ZbbZextH(RV64XRegister rd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kZbb);
        EmitR(0x4, 0x0, rs1.index(), 0x4, rd.index(), 0x3b);
    }

    //_____________________________ RV64 "Zbb" Instructions  END ____________________________//

    //____________________________ RV64 "Zbs" Instructions  START ___________________________//

    public void Bclr(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitR(0x24, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Bclri(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitI6(0x12, shamt, rs1, 0x1, rd, 0x13);
    }

    public void Bext(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitR(0x24, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Bexti(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitI6(0x12, shamt, rs1, 0x5, rd, 0x13);
    }

    public void Binv(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitR(0x34, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Binvi(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitI6(0x1A, shamt, rs1, 0x1, rd, 0x13);
    }

    public void Bset(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitR(0x14, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Bseti(RV64XRegister rd, RV64XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(RV64Extension.kZbs);
        EmitI6(0xA, shamt, rs1, 0x1, rd, 0x13);
    }

    //_____________________________ RV64 "Zbs" Instructions  END _____________________________//

    //______________________________ RVV "VSet" Instructions  START __________________________//

    public void VSetvli(RV64XRegister rd, RV64XRegister rs1, int vtypei) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(Utils.isUInt(11, vtypei));
        EmitI(vtypei, rs1.index(), VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    public void VSetivli(RV64XRegister rd, int uimm, int vtypei) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(Utils.isUInt(10, vtypei));
        CHECK(Utils.isUInt(5, uimm));
        EmitI((~0 << 10 | vtypei), uimm, VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    public void VSetvl(RV64XRegister rd, RV64XRegister rs1, RV64XRegister rs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        EmitR(0x40, rs2.index(), rs1.index(), VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    //_____________________________ RVV "VSet" Instructions  END _____________________________//

    //__________________________ RVV Load/Store Instructions  START __________________________//

    public void VLe8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLe16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLe32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLe64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSe8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSe16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSe32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSe64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLm(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01011, rs1.index(), VectorWidth.kMask.value(), vd.index(), 0x7);
    }

    public void VSm(RV64VRegister vs3, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01011, rs1.index(), VectorWidth.kMask.value(), vs3.index(), 0x27);
    }

    public void VLe8ff(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLe16ff(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLe32ff(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLe64ff(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLse8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLse16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLse32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLse64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSse8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSse16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSse32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSse64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLoxei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSoxei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLseg2e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg2e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg2e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg2e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg3e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg3e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg3e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg3e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg4e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg4e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg4e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg4e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg5e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg5e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg5e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg5e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg6e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg6e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg6e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg6e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg7e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg7e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg7e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg7e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg8e8(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg8e16(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg8e32(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg8e64(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSseg2e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg2e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg2e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg2e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg3e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg3e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg3e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg3e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg4e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg4e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg4e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg4e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg5e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg5e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg5e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg5e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg6e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg6e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg6e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg6e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg7e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg7e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg7e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg7e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg8e8(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg8e16(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg8e32(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg8e64(RV64VRegister vs3, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLseg2e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg2e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg2e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg2e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg3e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg3e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg3e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg3e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg4e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg4e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg4e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg4e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg5e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg5e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg5e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg5e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg6e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg6e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg6e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg6e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg7e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg7e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg7e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg7e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg8e8ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg8e16ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg8e32ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg8e64ff(RV64VRegister vd, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg2e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg2e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg2e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg2e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg3e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg3e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg3e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg3e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg4e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg4e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg4e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg4e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg5e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg5e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg5e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg5e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg6e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg6e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg6e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg6e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg7e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg7e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg7e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg7e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg8e8(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg8e16(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg8e32(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg8e64(RV64VRegister vd, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSsseg2e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e8(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e16(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e32(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e64(RV64VRegister vs3, RV64XRegister rs1, RV64XRegister rs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLuxseg2ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSuxseg2ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLoxseg2ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei8(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei16(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei32(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei64(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSoxseg2ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei8(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei16(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei32(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei64(RV64VRegister vs3, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VL1re8(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL1re16(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL1re32(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL1re64(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL2re8(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL2re16(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL2re32(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL2re64(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL4re8(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL4re16(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL4re32(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL4re64(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL8re8(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL8re16(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL8re32(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL8re64(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL1r(RV64VRegister vd, RV64XRegister rs1) {
        VL1re8(vd, rs1);
    }

    public void VL2r(RV64VRegister vd, RV64XRegister rs1) {
        VL2re8(vd, rs1);
    }

    public void VL4r(RV64VRegister vd, RV64XRegister rs1) {
        VL4re8(vd, rs1);
    }

    public void VL8r(RV64VRegister vd, RV64XRegister rs1) {
        VL8re8(vd, rs1);
    }

    public void VS1r(RV64VRegister vs3, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS2r(RV64VRegister vs3, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS4r(RV64VRegister vs3, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS8r(RV64VRegister vs3, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kLoadStore, RV64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, RV64VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    //___________________________ RVV Load/Store Instructions  END ___________________________//

    //___________________________ RVV Arithmetic Instructions  START _________________________//

    public void VAdd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAdd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAdd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRsub_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNeg_v(RV64VRegister vd, RV64VRegister vs2) {
        VRsub_vx(vd, vs2, Zero, RV64VM.kUnmasked);
    }

    public void VMinu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMinu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMin_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMin_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMaxu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMaxu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMax_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMax_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAnd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAnd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAnd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VOr_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VOr_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VOr_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VXor_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VXor_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VXor_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNot_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        VXor_vi(vd, vs2, -1, vm);
    }

    public void VRgather_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VRgather_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRgather_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSlideup_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSlideup_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VRgatherei16_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSlidedown_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSlidedown_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VAdc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAdc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAdc_vim(RV64VRegister vd, RV64VRegister vs2, int imm5) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMadc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMadc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMadc_vim(RV64VRegister vd, RV64VRegister vs2, int imm5) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMadc_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMadc_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMadc_vi(RV64VRegister vd, RV64VRegister vs2, int imm5) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, RV64VM.kUnmasked);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSbc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSbc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsbc_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsbc_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsbc_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsbc_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMerge_vvm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMerge_vxm(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMerge_vim(RV64VRegister vd, RV64VRegister vs2, int imm5) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMv_vv(RV64VRegister vd, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kUnmasked);
        EmitR(funct7, V0.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMv_vx(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kUnmasked);
        EmitR(funct7, V0.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMv_vi(RV64VRegister vd, int imm5) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kUnmasked);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, V0.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMseq_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMseq_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMseq_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsne_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsne_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsne_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsltu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsltu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgtu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        VMsltu_vv(vd, vs1, vs2, vm);
    }

    public void VMslt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMslt_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        VMslt_vv(vd, vs1, vs2, vm);
    }

    public void VMsleu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsleu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsleu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsgeu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        VMsleu_vv(vd, vs1, vs2, vm);
    }

    public void VMsltu_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm) {
        if (aimm5 < 1 || aimm5 > 6) {
            throw new IllegalArgumentException("Immediate should be between [1, 16]: " + aimm5);
        }
        CHECK(Utils.isUInt(4, aimm5 - 1));
        VMsleu_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsle_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsle_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsle_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsge_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        VMsle_vv(vd, vs1, vs2, vm);
    }

    public void VMslt_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm) {
        VMsle_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsgtu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgtu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011110, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsgeu_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        if (aimm5 < 1 || aimm5 > 6) {
            throw new IllegalArgumentException("Immediate should be between [1, 16]: " + aimm5);
        }
        CHECK(Utils.isUInt(4, aimm5 - 1));
        VMsgtu_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsgt_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgt_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsge_vi(RV64VRegister vd, RV64VRegister vs2, int aimm5, RV64VM vm) {
        VMsgt_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VSaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSaddu_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSadd_vi(RV64VRegister vd, RV64VRegister vs2, int imm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSll_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSll_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSll_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSmul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void Vmv1r_v(RV64VRegister vd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b100111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k1.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv2r_v(RV64VRegister vd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_EQ(vd.index() % 2, 0);
        CHECK_EQ(vs2.index() % 2, 0);
        final int funct7 = EncodeRVVF7(0b100111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k2.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv4r_v(RV64VRegister vd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_EQ(vd.index() % 4, 0);
        CHECK_EQ(vs2.index() % 4, 0);
        final int funct7 = EncodeRVVF7(0b100111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k4.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv8r_v(RV64VRegister vd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_EQ(vd.index() % 8, 0);
        CHECK_EQ(vs2.index() % 8, 0);
        final int funct7 = EncodeRVVF7(0b100111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k8.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSrl_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSrl_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSrl_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSra_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSra_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSra_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsrl_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsrl_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsrl_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsra_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsra_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsra_vi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNsrl_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNsrl_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNsrl_wi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNcvt_x_x_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        VNsrl_wx(vd, vs2, Zero, vm);
    }

    public void VNsra_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNsra_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNsra_wi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNclipu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNclipu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNclipu_wi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNclip_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNclip_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNclip_wi(RV64VRegister vd, RV64VRegister vs2, int uimm5, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VWredsumu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VWredsum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VRedsum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedand_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedor_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedxor_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedminu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmin_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmaxu_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmax_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VSlide1up_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VSlide1down_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VCompress_vm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMandn_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011000, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMand_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011001, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMmv_m(RV64VRegister vd, RV64VRegister vs2) {
        VMand_mm(vd, vs2, vs2);
    }

    public void VMor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011010, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMxor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011011, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMclr_m(RV64VRegister vd) {
        VMxor_mm(vd, vd, vd);
    }

    public void VMorn_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011100, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMnand_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011101, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMnot_m(RV64VRegister vd, RV64VRegister vs2) {
        VMnand_mm(vd, vs2, vs2);
    }

    public void VMnor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011110, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMxnor_mm(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011111, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMset_m(RV64VRegister vd) {
        VMxnor_mm(vd, vd, vd);
    }

    public void VDivu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VDivu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VDiv_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VDiv_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VRemu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRemu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VRem_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRem_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulhu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulhu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulhsu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulhsu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulh_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulh_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMadd_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VNmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VNmsub_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMacc_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VNmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VNmsac_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWaddu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWaddu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWcvtu_x_x_v(RV64VRegister vd, RV64VRegister vs, RV64VM vm) {
        VWaddu_vx(vd, vs, Zero, vm);
    }

    public void VWadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWadd_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWcvt_x_x_v(RV64VRegister vd, RV64VRegister vs, RV64VM vm) {
        VWadd_vx(vd, vs, Zero, vm);
    }

    public void VWsubu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsubu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsub_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWaddu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWaddu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWadd_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWadd_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsubu_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsubu_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsub_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsub_wx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmulu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmulu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmulsu_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmulsu_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmul_vx(RV64VRegister vd, RV64VRegister vs2, RV64XRegister rs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccu_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmaccu_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmacc_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccus_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccsu_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmaccsu_vx(RV64VRegister vd, RV64XRegister rs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VFadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFadd_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredusum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredosum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmin_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmin_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredmin_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmax_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmax_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredmax_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnj_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnj_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFsgnjn_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnjn_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFneg_v(RV64VRegister vd, RV64VRegister vs) {
        VFsgnjn_vv(vd, vs, vs, RV64VM.kUnmasked);
    }

    public void VFsgnjx_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnjx_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFabs_v(RV64VRegister vd, RV64VRegister vs) {
        VFsgnjx_vv(vd, vs, vs, RV64VM.kUnmasked);
    }

    public void VFslide1up_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFslide1down_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmerge_vfm(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kV0_t);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmv_v_f(RV64VRegister vd, RV64FRegister fs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, RV64VM.kUnmasked);
        EmitR(funct7, V0.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfeq_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfeq_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfle_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfle_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfge_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        VMfle_vv(vd, vs1, vs2, vm);
    }

    public void VMflt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMflt_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfgt_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        VMflt_vv(vd, vs1, vs2, vm);
    }

    public void VMfne_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfne_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfgt_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfge_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFdiv_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFdiv_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFrdiv_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmul_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFrsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmadd_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmadd_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmadd_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmsub_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmsub_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmsub_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwadd_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwredusum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwredosum_vs(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_wf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwsub_wv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_wf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmul_vv(RV64VRegister vd, RV64VRegister vs2, RV64VRegister vs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmul_vf(RV64VRegister vd, RV64VRegister vs2, RV64FRegister fs1, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwnmacc_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwnmacc_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwnmsac_vv(RV64VRegister vd, RV64VRegister vs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwnmsac_vf(RV64VRegister vd, RV64FRegister fs1, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMv_s_x(RV64VRegister vd, RV64XRegister rs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kUnmasked);
        EmitR(funct7, 0b00000, rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMv_x_s(RV64XRegister rd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VCpop_m(RV64XRegister rd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VFirst_m(RV64XRegister rd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, vm);
        EmitR(funct7, vs2.index(), 0b10001, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VZext_vf8(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf8(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VZext_vf4(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00100, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf4(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00101, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VZext_vf2(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00110, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf2(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00111, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VFmv_s_f(RV64VRegister vd, RV64FRegister fs1) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kUnmasked);
        EmitR(funct7, 0b00000, fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmv_f_s(RV64FRegister fd, RV64VRegister vs2) {
        AssertExtensionsEnabled(RV64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, RV64VM.kUnmasked);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), fd.index(), 0x57);
    }

    public void VFcvt_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_f_xu_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_f_x_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_rtz_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_rtz_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_xu_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_x_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_rtz_xu_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_rtz_x_f_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_xu_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_x_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_xu_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_x_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rod_f_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10101, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rtz_xu_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rtz_x_f_w(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsqrt_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFrsqrt7_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFrec7_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00101, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFclass_v(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMsbf_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00001, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMsof_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMsif_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VIota_m(RV64VRegister vd, RV64VRegister vs2, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VId_v(RV64VRegister vd, RV64VM vm) {
        AssertExtensionsEnabled(RV64Extension.kV);
        CHECK_IMPLIES(vm == RV64VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, V0.index(), 0b10001, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    //_________________________ RVV Arithmetic Instructions  END   ___________________________//

    //____________________________ RV64 MACRO Instructions  START ____________________________//

    // Pseudo instructions

    public void Nop() {
        Addi(Zero, Zero, 0);
    }

    public void Li(RV64XRegister rd, long imm) {
        Loadd(rd, newI64Literal(imm));
    }

    public void Mv(RV64XRegister rd, RV64XRegister rs) {
        Addi(rd, rs, 0);
    }

    public void Not(RV64XRegister rd, RV64XRegister rs) {
        Xori(rd, rs, -1);
    }

    public void Neg(RV64XRegister rd, RV64XRegister rs) {
        Sub(rd, Zero, rs);
    }

    public void NegW(RV64XRegister rd, RV64XRegister rs) {
        Subw(rd, Zero, rs);
    }

    public void SextB(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZbb)) {
            if (IsExtensionEnabled(RV64Extension.kZcb) && rd == rs && rd.isShortReg()) {
                CSextB(rd);
            } else {
                ZbbSextB(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 8);
            Srai(rd, rd, kXlen - 8);
        }
    }

    public void SextH(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZbb)) {
            if (IsExtensionEnabled(RV64Extension.kZcb) && rd == rs && rd.isShortReg()) {
                CSextH(rd);
            } else {
                ZbbSextH(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 16);
            Srai(rd, rd, kXlen - 16);
        }
    }

    public void SextW(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZca) && rd != Zero && (rd == rs || rs == Zero)) {
            if (rd == rs) {
                CAddiw(rd, 0);
            } else {
                CLi(rd, 0);
            }
        } else {
            Addiw(rd, rs, 0);
        }
    }

    public void ZextB(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZcb) && rd == rs && rd.isShortReg()) {
            CZextB(rd);
        } else {
            Andi(rd, rs, 0xff);
        }
    }

    public void ZextH(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZbb)) {
            if (IsExtensionEnabled(RV64Extension.kZcb) && rd == rs && rd.isShortReg()) {
                CZextH(rd);
            } else {
                ZbbZextH(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 16);
            Srli(rd, rd, kXlen - 16);
        }
    }

    public void ZextW(RV64XRegister rd, RV64XRegister rs) {
        if (IsExtensionEnabled(RV64Extension.kZba)) {
            if (IsExtensionEnabled(RV64Extension.kZcb) && rd == rs && rd.isShortReg()) {
                CZextW(rd);
            } else {
                AddUw(rd, rs, Zero);
            }
        } else {
            Slli(rd, rs, kXlen - 32);
            Srli(rd, rd, kXlen - 32);
        }
    }

    public void Seqz(RV64XRegister rd, RV64XRegister rs) {
        Sltiu(rd, rs, 1);
    }

    public void Snez(RV64XRegister rd, RV64XRegister rs) {
        Sltu(rd, Zero, rs);
    }

    public void Sltz(RV64XRegister rd, RV64XRegister rs) {
        Slt(rd, rs, Zero);
    }

    public void Sgtz(RV64XRegister rd, RV64XRegister rs) {
        Slt(rd, Zero, rs);
    }

    public void FMvS(RV64FRegister rd, RV64FRegister rs) {
        FSgnjS(rd, rs, rs);
    }

    public void FAbsS(RV64FRegister rd, RV64FRegister rs) {
        FSgnjxS(rd, rs, rs);
    }

    public void FNegS(RV64FRegister rd, RV64FRegister rs) {
        FSgnjnS(rd, rs, rs);
    }

    public void FMvD(RV64FRegister rd, RV64FRegister rs) {
        FSgnjD(rd, rs, rs);
    }

    public void FAbsD(RV64FRegister rd, RV64FRegister rs) {
        FSgnjxD(rd, rs, rs);
    }

    public void FNegD(RV64FRegister rd, RV64FRegister rs) {
        FSgnjnD(rd, rs, rs);
    }

    public void Beqz(RV64XRegister rs, int offset) {
        Beq(rs, Zero, offset);
    }

    public void Bnez(RV64XRegister rs, int offset) {
        Bne(rs, Zero, offset);
    }

    public void Blez(RV64XRegister rt, int offset) {
        Bge(Zero, rt, offset);
    }

    public void Bgez(RV64XRegister rt, int offset) {
        Bge(rt, Zero, offset);
    }

    public void Bltz(RV64XRegister rt, int offset) {
        Blt(rt, Zero, offset);
    }

    public void Bgtz(RV64XRegister rt, int offset) {
        Blt(Zero, rt, offset);
    }

    public void Bgt(RV64XRegister rs, RV64XRegister rt, int offset) {
        Blt(rt, rs, offset);
    }

    public void Ble(RV64XRegister rs, RV64XRegister rt, int offset) {
        Bge(rt, rs, offset);
    }

    public void Bgtu(RV64XRegister rs, RV64XRegister rt, int offset) {
        Bltu(rt, rs, offset);
    }

    public void Bleu(RV64XRegister rs, RV64XRegister rt, int offset) {
        Bgeu(rt, rs, offset);
    }

    public void J(int offset) {
        Jal(Zero, offset);
    }

    public void Jal(int offset) {
        Jal(RA, offset);
    }

    public void Jr(RV64XRegister rs) {
        Jalr(Zero, rs, 0);
    }

    public void Jalr(RV64XRegister rs) {
        Jalr(RA, rs, 0);
    }

    public void Jalr(RV64XRegister rd, RV64XRegister rs) {
        Jalr(rd, rs, 0);
    }

    public void Ret() {
        Jalr(Zero, RA, 0);
    }

    public void RdCycle(RV64XRegister rd) {
        Csrrs(rd, 0xc00, Zero);
    }

    public void RdTime(RV64XRegister rd) {
        Csrrs(rd, 0xc01, Zero);
    }

    public void RdInstret(RV64XRegister rd) {
        Csrrs(rd, 0xc02, Zero);
    }

    public void Csrr(RV64XRegister rd, int csr) {
        Csrrs(rd, csr, Zero);
    }

    public void Csrw(int csr, RV64XRegister rs) {
        Csrrw(Zero, csr, rs);
    }

    public void Csrs(int csr, RV64XRegister rs) {
        Csrrs(Zero, csr, rs);
    }

    public void Csrc(int csr, RV64XRegister rs) {
        Csrrc(Zero, csr, rs);
    }

    public void Csrwi(int csr, int uimm5) {
        Csrrwi(Zero, csr, uimm5);
    }

    public void Csrsi(int csr, int uimm5) {
        Csrrsi(Zero, csr, uimm5);
    }

    public void Csrci(int csr, int uimm5) {
        Csrrci(Zero, csr, uimm5);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadb(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lb, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadh(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lh, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadw(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lw, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadd(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Ld, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadbu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lbu, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadhu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lhu, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Loadwu(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int offset) {
        LoadFromOffset(this::Lwu, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Storeb(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset) {
        StoreToOffset(this::Sb, tmp, rs2, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Storeh(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset) {
        StoreToOffset(this::Sh, tmp, rs2, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Storew(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset) {
        StoreToOffset(this::Sw, tmp, rs2, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void Stored(RV64XRegister tmp, RV64XRegister rs2, RV64XRegister rs1, int offset) {
        StoreToOffset(this::Sd, tmp, rs2, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void FLoadw(RV64XRegister tmp, RV64FRegister rd, RV64XRegister rs1, int offset) {
        FLoadFromOffset(this::FLw, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void FLoadd(RV64XRegister tmp, RV64FRegister rd, RV64XRegister rs1, int offset) {
        FLoadFromOffset(this::FLd, tmp, rd, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void FStorew(RV64XRegister tmp, RV64FRegister rs2, RV64XRegister rs1, int offset) {
        FStoreToOffset(this::FSw, tmp, rs2, rs1, offset);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void FStored(RV64XRegister tmp, RV64FRegister rs2, RV64XRegister rs1, int offset) {
        FStoreToOffset(this::FSd, tmp, rs2, rs1, offset);
    }

    public void LoadConst32(RV64XRegister rd, int value) {
        Loadw(rd, newI32Literal(value));
    }

    public void LoadConst64(RV64XRegister rd, long value) {
        Li(rd, value);
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void AddConst32(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, int value) {
        AddConstImpl(tmp, rd, rs1, value, this::Addiw, (rd_, rs1_, value_, tmp_) -> {
            LoadConst32(tmp_, (int) value_);
            Addw(rd_, rs1_, tmp_);
        });
    }

    // If you are sure that tmp register is not needed, set it to Zero.
    public void AddConst64(RV64XRegister tmp, RV64XRegister rd, RV64XRegister rs1, long value) {
        AddConstImpl(tmp, rd, rs1, value, this::Addi, (rd_, rs1_, value_, tmp_) -> {
            LoadConst64(tmp_, value_);
            Add(rd_, rs1_, tmp_);
        });
    }

    public void Beqz(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Beq(rs, Zero, label, is_bare);
    }

    public void Bnez(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Bne(rs, Zero, label, is_bare);
    }

    public void Blez(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Ble(rs, Zero, label, is_bare);
    }

    public void Bgez(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Bge(rs, Zero, label, is_bare);
    }

    public void Bltz(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Blt(rs, Zero, label, is_bare);
    }

    public void Bgtz(RV64XRegister rs, RV64Label label, boolean is_bare) {
        Bgt(rs, Zero, label, is_bare);
    }

    public void Beq(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, kCondEQ, rs, rt);
    }

    public void Bne(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, kCondNE, rs, rt);
    }

    public void Ble(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLE, rs, rt);
    }

    public void Bge(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGE, rs, rt);
    }

    public void Blt(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLT, rs, rt);
    }

    public void Bgt(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGT, rs, rt);
    }

    public void Bleu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLEU, rs, rt);
    }

    public void Bgeu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGEU, rs, rt);
    }

    public void Bltu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLTU, rs, rt);
    }

    public void Bgtu(RV64XRegister rs, RV64XRegister rt, RV64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGTU, rs, rt);
    }

    public void Jal(RV64XRegister rd, RV64Label label, boolean is_bare) {
        Buncond(label, rd, is_bare);
    }

    public void J(RV64Label label, boolean is_bare) {
        Jal(Zero, label, is_bare);
    }

    public void Jal(RV64Label label, boolean is_bare) {
        Jal(RA, label, is_bare);
    }

    public RV64Literal newI32Literal(int value) {
        var lit = new RV64Literal(value, true);
        literals_.add(lit);
        return lit;
    }

    public RV64Literal newF32Literal(float value) {
        return newI32Literal(Float.floatToRawIntBits(value));
    }

    public RV64Literal newI64Literal(long value) {
        var lit = new RV64Literal(value, false);
        long_literals_.add(lit);
        return lit;
    }

    public RV64Literal newF64Literal(double value) {
        return newI64Literal(Double.doubleToRawLongBits(value));
    }

    public void Loadw(RV64XRegister rd, RV64Literal literal) {
        CHECK_EQ(literal.getSize(), 4);
        LoadLiteral(literal, rd, Type.kLiteral);
    }

    public void Loadwu(RV64XRegister rd, RV64Literal literal) {
        CHECK_EQ(literal.getSize(), 4);
        LoadLiteral(literal, rd, Type.kLiteralUnsigned);
    }

    public void Loadd(RV64XRegister rd, RV64Literal literal) {
        CHECK_EQ(literal.getSize(), 8);
        LoadLiteral(literal, rd, Type.kLiteralLong);
    }

    public void FLoadw(RV64XRegister tmp, RV64FRegister rd, RV64Literal literal) {
        CHECK_NE(tmp.index(), Zero.index());
        CHECK_EQ(literal.getSize(), 4);
        LoadLiteral(literal, tmp, rd, Type.kLiteralFloat);
    }

    public void FLoadd(RV64XRegister tmp, RV64FRegister rd, RV64Literal literal) {
        CHECK_NE(tmp.index(), Zero.index());
        CHECK_EQ(literal.getSize(), 8);
        LoadLiteral(literal, tmp, rd, Type.kLiteralDouble);
    }

    public void LoadLabelAddress(RV64XRegister rd, RV64Label label) {
        CHECK_NE(rd.index(), Zero.index());
        int target = label.isBound() ? GetLabelLocation(label) : RV64Branch.kUnresolved;
        branches_.add(new RV64Branch(size(), target, rd, Type.kLabel));
        FinalizeLabeledBranch(label);
    }

    public void Unimp() {
        if (IsExtensionEnabled(RV64Extension.kZca)) {
            CUnimp();
        } else {
            Emit32(0xC0001073);
        }
    }

    //______________________________ RV64 MACRO Instructions END _____________________________//
}
