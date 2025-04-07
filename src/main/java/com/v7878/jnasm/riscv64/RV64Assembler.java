package com.v7878.jnasm.riscv64;

import static com.v7878.jnasm.riscv64.FenceType.kFenceNNRW;
import static com.v7878.jnasm.riscv64.VRegister.V0;
import static com.v7878.jnasm.riscv64.XRegister.RA;
import static com.v7878.jnasm.riscv64.XRegister.SP;
import static com.v7878.jnasm.riscv64.XRegister.Zero;

import com.v7878.jnasm.Assembler;

public abstract class RV64Assembler extends Assembler implements RV64AssemblerI {
    private static final int kXlen = 64;

    private enum BranchCondition {
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

    private static void CHECK(boolean value) {
        if (!value) {
            // TODO: message
            throw new AssertionError();
        }
    }

    private static void CHECK_EQ(int a, int b) {
        CHECK(a == b);
    }

    private static void CHECK_NE(int a, int b) {
        CHECK(a != b);
    }

    protected abstract void CHECK_NE(XRegister rd, XRegister zero);

    private static void CHECK_LT(int a, int b) {
        CHECK(a < b);
    }

    private static void CHECK_LE(int a, int b) {
        CHECK(a <= b);
    }

    private static void CHECK_GT(int a, int b) {
        CHECK(a > b);
    }

    private static void CHECK_GE(int a, int b) {
        CHECK(a >= b);
    }

    private static boolean isUInt(int width, int value) {
        // TODO;
        throw new UnsupportedOperationException();
    }

    private static boolean isInt(int width, int value) {
        // TODO;
        throw new UnsupportedOperationException();
    }

    private static boolean isUInt2(int value) {
        return isUInt(2, value);
    }

    private static boolean isUInt3(int value) {
        return isUInt(3, value);
    }

    private static boolean isUInt4(int value) {
        return isUInt(4, value);
    }

    private static boolean isUInt5(int value) {
        return isUInt(5, value);
    }

    private static boolean isInt6(int value) {
        return isInt(6, value);
    }

    private static boolean isUInt6(int value) {
        return isUInt(6, value);
    }

    private static boolean isUInt7(int value) {
        return isUInt(7, value);
    }

    private static boolean isUInt8(int value) {
        return isUInt(8, value);
    }

    private static boolean isInt9(int value) {
        return isInt(9, value);
    }

    private static boolean isUInt9(int value) {
        return isUInt(9, value);
    }

    private static boolean isInt10(int value) {
        return isInt(10, value);
    }

    private static boolean isUInt10(int value) {
        return isUInt(10, value);
    }

    private static boolean isUInt11(int value) {
        return isUInt(11, value);
    }

    private static boolean isInt12(int value) {
        return isInt(12, value);
    }

    private static boolean isUInt12(int value) {
        return isUInt(12, value);
    }

    private static boolean isInt13(int value) {
        return isInt(13, value);
    }

    private static boolean isUInt20(int value) {
        return isUInt(20, value);
    }

    private static boolean isInt21(int value) {
        return isInt(21, value);
    }

    private static boolean isAligned(int value, int alignment) {
        // TODO;
        throw new UnsupportedOperationException();
    }

    private static boolean isAligned2(int value) {
        return isAligned(value, 2);
    }

    private static boolean isAligned4(int value) {
        return isAligned(value, 4);
    }

    private static boolean isAligned8(int value) {
        return isAligned(value, 8);
    }

    private static boolean isAligned16(int value) {
        return isAligned(value, 16);
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

    private static boolean IsShortReg(int reg) {
        int uv = reg - 8;
        return isUInt3(uv);
    }

    private static boolean IsShortReg(XRegister reg) {
        return IsShortReg(reg.index());
    }

    private static boolean IsShortReg(FRegister reg) {
        return IsShortReg(reg.index());
    }

    private static int ToInt12(int uint12) {
        CHECK(isUInt12(uint12));
        return uint12 - ((uint12 & 0x800) << 1);
    }

    abstract void Bcond(Riscv64Label label, boolean is_bare, BranchCondition condition, XRegister lhs, XRegister rhs);

    abstract void Buncond(Riscv64Label label, XRegister rd, boolean is_bare);

    abstract void Emit16(int value);

    abstract void Emit32(int value);

    // Implementation helper for `Li()`, `LoadConst32()` and `LoadConst64()`.
    abstract void LoadImmediate(XRegister rd, long imm, boolean can_use_tmp);

    abstract boolean IsExtensionEnabled(Riscv64Extension ext);

    abstract void AssertExtensionsEnabled(Riscv64Extension ext);

    abstract void AssertExtensionsEnabled(Riscv64Extension ext, Riscv64Extension... other_ext);

    abstract void CHECK_IMPLIES(boolean a, boolean b);

    abstract void CHECK_ALIGNED(int value, int alignment);

    // RVV constants and helpers

    abstract int EncodeRVVMemF7(Nf nf, int mew, MemAddressMode mop, VM vm);

    abstract int EncodeRVVF7(int funct6, VM vm);

    abstract int EncodeInt5(int imm);

    abstract int EncodeInt6(int imm);

    private static int EncodeShortReg(int reg) {
        CHECK(IsShortReg(reg));
        return reg - 8;
    }

    private static int EncodeShortReg(XRegister reg) {
        return EncodeShortReg(reg.index());
    }

    private static int EncodeShortReg(FRegister reg) {
        return EncodeShortReg(reg.index());
    }

    // Rearrange given offset in the way {offset[0] | offset[1]}
    private int EncodeOffset0_1(int offset) {
        CHECK(isUInt2(offset));
        return offset >>> 1 | (offset & 1) << 1;
    }

    // Rearrange given offset, scaled by 4, in the way {offset[5:2] | offset[7:6]}
    abstract int ExtractOffset52_76(int offset);

    // Rearrange given offset, scaled by 8, in the way {offset[5:3] | offset[8:6]}
    abstract int ExtractOffset53_86(int offset);

    // Rearrange given offset, scaled by 4, in the way {offset[5:2] | offset[6]}
    abstract int ExtractOffset52_6(int offset);

    // Rearrange given offset, scaled by 8, in the way {offset[5:3], offset[7:6]}
    abstract int ExtractOffset53_76(int offset);

    abstract boolean IsImmCLuiEncodable(int uimm);

    // Emit helpers.

    // I-type instruction:
    //
    //    31                   20 19     15 14 12 11      7 6           0
    //   -----------------------------------------------------------------
    //   [ . . . . . . . . . . . | . . . . | . . | . . . . | . . . . . . ]
    //   [        imm11:0            rs1   funct3     rd        opcode   ]
    //   -----------------------------------------------------------------
    private void EmitI(int imm12, int rs1, int funct3, int rd, int opcode) {
        CHECK(isInt12(imm12));
        CHECK(isUInt5(rs1));
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(rd));
        CHECK(isUInt7(opcode));
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
        CHECK(isUInt7(funct7));
        CHECK(isUInt5(rs2));
        CHECK(isUInt5(rs1));
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(rd));
        CHECK(isUInt7(opcode));
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
        CHECK(isUInt5(rs3));
        CHECK(isUInt2(fmt));
        CHECK(isUInt5(rs2));
        CHECK(isUInt5(rs1));
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(rd));
        CHECK(isUInt7(opcode));
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
        CHECK(isInt12(imm12));
        CHECK(isUInt5(rs2));
        CHECK(isUInt5(rs1));
        CHECK(isUInt3(funct3));
        CHECK(isUInt7(opcode));
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
    private void EmitI6(int funct6, int imm6, XRegister rs1, int funct3, XRegister rd, int opcode) {
        CHECK(isUInt6(funct6));
        CHECK(isUInt6(imm6));
        CHECK(isUInt5(rs1.index()));
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(rd.index()));
        CHECK(isUInt7(opcode));
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
    private void EmitB(int offset, XRegister rs2, XRegister rs1, int funct3, int opcode) {
        CHECK_ALIGNED(offset, 2);
        CHECK(isInt13(offset));
        CHECK(isUInt5(rs2.index()));
        CHECK(isUInt5(rs1.index()));
        CHECK(isUInt3(funct3));
        CHECK(isUInt7(opcode));
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
    private void EmitU(int imm20, XRegister rd, int opcode) {
        CHECK(isUInt20(imm20));
        CHECK(isUInt5(rd.index()));
        CHECK(isUInt7(opcode));
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
    private void EmitJ(int offset, XRegister rd, int opcode) {
        CHECK_ALIGNED(offset, 2);
        CHECK(isInt21(offset));
        CHECK(isUInt5(rd.index()));
        CHECK(isUInt7(opcode));
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
    private void EmitCR(int funct4, XRegister rd_rs1, XRegister rs2, int opcode) {
        CHECK(isUInt4(funct4));
        CHECK(isUInt5(rd_rs1.index()));
        CHECK(isUInt5(rs2.index()));
        CHECK(isUInt2(opcode));

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
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(rd_rs1));
        CHECK(isUInt6(imm6));
        CHECK(isUInt2(opcode));

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
        CHECK(isUInt3(funct3));
        CHECK(isUInt6(offset6));
        CHECK(isUInt5(rs2));
        CHECK(isUInt2(opcode));

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
        CHECK(isUInt3(funct3));
        CHECK(isUInt8(imm8));
        CHECK(IsShortReg(rd_s));
        CHECK(isUInt2(opcode));

        int encoding = (funct3 << 13) | (imm8 << 5) | (EncodeShortReg(rd_s) << 2) | opcode;
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
    private void EmitCM(int funct3, int imm5, XRegister rs1_s, int rd_rs2_s, int opcode) {
        CHECK(isUInt3(funct3));
        CHECK(isUInt5(imm5));
        CHECK(IsShortReg(rs1_s));
        CHECK(IsShortReg(rd_rs2_s));
        CHECK(isUInt2(opcode));

        int immH3 = BitFieldExtract(imm5, 2, 3);
        int immL2 = BitFieldExtract(imm5, 0, 2);

        int encoding = (funct3 << 13) | (immH3 << 10) | (EncodeShortReg(rs1_s) << 7)
                | (immL2 << 5) | (EncodeShortReg(rd_rs2_s) << 2) | opcode;
        Emit16(encoding);
    }

    // CA-type instruction:
    //
    //   15         10 9  7 6 5 4   2 1 0
    //   ---------------------------------
    //   [ . . . . . | . . | . | . . | . ]
    //   [    funct6 rds1' funct2 rs2' op]
    //   ---------------------------------
    private void EmitCA(int funct6, XRegister rd_rs1_s, int funct2, int rs2_v, int opcode) {
        CHECK(isUInt6(funct6));
        CHECK(IsShortReg(rd_rs1_s));
        CHECK(isUInt2(funct2));
        CHECK(isUInt3(rs2_v));
        CHECK(isUInt2(opcode));

        int encoding = (funct6 << 10) | (EncodeShortReg(rd_rs1_s) << 7)
                | (funct2 << 5) | (rs2_v << 2) | opcode;
        Emit16(encoding);
    }

    private void EmitCAReg(int funct6, XRegister rd_rs1_s, int funct2, XRegister rs2_s, int opcode) {
        CHECK(IsShortReg(rs2_s));
        EmitCA(funct6, rd_rs1_s, funct2, EncodeShortReg(rs2_s), opcode);
    }

    @SuppressWarnings("SameParameterValue")
    private void EmitCAImm(int funct6, XRegister rd_rs1_s, int funct2, int funct3, int opcode) {
        EmitCA(funct6, rd_rs1_s, funct2, funct3, opcode);
    }

    // CB-type instruction:
    //
    //   15  13 12  10 9  7 6       2 1 0
    //   ---------------------------------
    //   [ . . | . . | . . | . . . . | . ]
    //   [func3 offset rs1'   offset  op ]
    //   ---------------------------------
    private void EmitCB(int funct3, int offset8, XRegister rd_rs1_s, int opcode) {
        CHECK(isUInt3(funct3));
        CHECK(isUInt8(offset8));
        CHECK(IsShortReg(rd_rs1_s));
        CHECK(isUInt2(opcode));

        int offsetH3 = BitFieldExtract(offset8, 5, 3);
        int offsetL5 = BitFieldExtract(offset8, 0, 5);

        int encoding = (funct3 << 13) | (offsetH3 << 10) |
                (EncodeShortReg(rd_rs1_s) << 7) | (offsetL5 << 2) | opcode;
        Emit16(encoding);
    }

    // Wrappers for EmitCB with different imm bit permutation

    @SuppressWarnings("SameParameterValue")
    private void EmitCBBranch(int funct3, int offset, XRegister rs1_s, int opcode) {
        CHECK(isInt9(offset));
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
    private void EmitCBArithmetic(int funct3, int funct2, int imm, XRegister rd_s, int opcode) {
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
        CHECK(isInt12(offset));
        CHECK(isUInt3(funct3));
        CHECK(isUInt2(opcode));

        // offset[11|4|9:8|10|6|7|3:1|5]
        int jumpt = (BitFieldExtract(offset, 11, 1) << 10) |
                (BitFieldExtract(offset, 4, 1) << 9) |
                (BitFieldExtract(offset, 8, 2) << 7) |
                (BitFieldExtract(offset, 10, 1) << 6) |
                (BitFieldExtract(offset, 6, 1) << 5) |
                (BitFieldExtract(offset, 7, 1) << 4) |
                (BitFieldExtract(offset, 1, 3) << 1) |
                BitFieldExtract(offset, 5, 1);

        CHECK(isUInt11(jumpt));

        int encoding = funct3 << 13 | jumpt << 2 | opcode;
        Emit16(encoding);
    }

    //_____________________________ RV64 VARIANTS extension _____________________________//

    //______________________________ RV64 "I" Instructions ______________________________//

    // LUI/AUIPC (RV32I, with sign-extension on RV64I), opcode = 0x17, 0x37

    public void Lui(XRegister rd, int imm20) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero && rd != SP && IsImmCLuiEncodable(imm20)) {
                CLui(rd, imm20);
                return;
            }
        }

        EmitU(imm20, rd, 0x37);
    }

    public void Auipc(XRegister rd, int imm20) {
        EmitU(imm20, rd, 0x17);
    }

    // Jump instructions (RV32I), opcode = 0x67, 0x6f

    public void Jal(XRegister rd, int offset) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == Zero && isInt12(offset)) {
                CJ(offset);
                return;
            }
            // Note: `c.jal` is RV32-only.
        }

        EmitJ(offset, rd, 0x6F);
    }

    public void Jalr(XRegister rd, XRegister rs1, int offset) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
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

    public void Beq(XRegister rs1, XRegister rs2, int offset) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rs2 == Zero && IsShortReg(rs1) && isInt9(offset)) {
                CBeqz(rs1, offset);
                return;
            } else if (rs1 == Zero && IsShortReg(rs2) && isInt9(offset)) {
                CBeqz(rs2, offset);
                return;
            }
        }

        EmitB(offset, rs2, rs1, 0x0, 0x63);
    }

    public void Bne(XRegister rs1, XRegister rs2, int offset) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rs2 == Zero && IsShortReg(rs1) && isInt9(offset)) {
                CBnez(rs1, offset);
                return;
            } else if (rs1 == Zero && IsShortReg(rs2) && isInt9(offset)) {
                CBnez(rs2, offset);
                return;
            }
        }

        EmitB(offset, rs2, rs1, 0x1, 0x63);
    }

    public void Blt(XRegister rs1, XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x4, 0x63);
    }

    public void Bge(XRegister rs1, XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x5, 0x63);
    }

    public void Bltu(XRegister rs1, XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x6, 0x63);
    }

    public void Bgeu(XRegister rs1, XRegister rs2, int offset) {
        EmitB(offset, rs2, rs1, 0x7, 0x63);
    }

    // Load instructions (RV32I+RV64I): opcode = 0x03, funct3 from 0x0 ~ 0x6

    public void Lb(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);
        EmitI(offset, rs1.index(), 0x0, rd.index(), 0x03);
    }

    public void Lh(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rd) && IsShortReg(rs1) && isUInt2(offset) && isAligned2(offset)) {
                CLh(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x1, rd.index(), 0x03);
    }

    public void Lw(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero && rs1 == SP && isUInt8(offset) && isAligned4(offset)) {
                CLwsp(rd, offset);
                return;
            } else if (IsShortReg(rd) && IsShortReg(rs1) && isUInt7(offset) && isAligned4(offset)) {
                CLw(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x03);
    }

    public void Ld(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero && rs1 == SP && isUInt9(offset) && isAligned8(offset)) {
                CLdsp(rd, offset);
                return;
            } else if (IsShortReg(rd) && IsShortReg(rs1) && isUInt8(offset) && isAligned8(offset)) {
                CLd(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x03);
    }

    public void Lbu(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rd) && IsShortReg(rs1) && isUInt2(offset)) {
                CLbu(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x4, rd.index(), 0x03);
    }

    public void Lhu(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rd) && IsShortReg(rs1) && isUInt2(offset) && isAligned2(offset)) {
                CLhu(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x5, rd.index(), 0x03);
    }

    public void Lwu(XRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);
        EmitI(offset, rs1.index(), 0x6, rd.index(), 0x3);
    }

    // Store instructions (RV32I+RV64I): opcode = 0x23, funct3 from 0x0 ~ 0x3

    public void Sb(XRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rs2) && IsShortReg(rs1) && isUInt2(offset)) {
                CSb(rs2, rs1, offset);
                return;
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x0, 0x23);
    }

    public void Sh(XRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rs2) && IsShortReg(rs1) && isUInt2(offset) && isAligned2(offset)) {
                CSh(rs2, rs1, offset);
                return;
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x1, 0x23);
    }

    public void Sw(XRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rs1 == SP && isUInt8(offset) && isAligned4(offset)) {
                CSwsp(rs2, offset);
                return;
            } else if (IsShortReg(rs2) && IsShortReg(rs1) && isUInt7(offset) && isAligned4(offset)) {
                CSw(rs2, rs1, offset);
                return;
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x2, 0x23);
    }

    public void Sd(XRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rs1 == SP && isUInt9(offset) && isAligned8(offset)) {
                CSdsp(rs2, offset);
                return;
            } else if (IsShortReg(rs2) && IsShortReg(rs1) && isUInt8(offset) && isAligned8(offset)) {
                CSd(rs2, rs1, offset);
                return;
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x3, 0x23);
    }

    // IMM ALU instructions (RV32I): opcode = 0x13, funct3 from 0x0 ~ 0x7

    public void Addi(XRegister rd, XRegister rs1, int imm12) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero) {
                if (rs1 == Zero && isInt6(imm12)) {
                    CLi(rd, imm12);
                    return;
                } else if (imm12 != 0) {
                    if (rd == rs1) {
                        // We're testing against clang's assembler and therefore
                        // if both c.addi and c.addi16sp are viable, we use the c.addi just like clang.
                        if (isInt6(imm12)) {
                            CAddi(rd, imm12);
                            return;
                        } else if (rd == SP && isInt10(imm12) && isAligned16(imm12)) {
                            CAddi16Sp(imm12);
                            return;
                        }
                    } else if (IsShortReg(rd) && rs1 == SP && isUInt10(imm12) && isAligned4(imm12)) {
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

    public void Slti(XRegister rd, XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x2, rd.index(), 0x13);
    }

    public void Sltiu(XRegister rd, XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x3, rd.index(), 0x13);
    }

    public void Xori(XRegister rd, XRegister rs1, int imm12) {
        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (rd == rs1 && IsShortReg(rd) && imm12 == -1) {
                CNot(rd);
                return;
            }
        }

        EmitI(imm12, rs1.index(), 0x4, rd.index(), 0x13);
    }

    public void Ori(XRegister rd, XRegister rs1, int imm12) {
        EmitI(imm12, rs1.index(), 0x6, rd.index(), 0x13);
    }

    public void Andi(XRegister rd, XRegister rs1, int imm12) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && IsShortReg(rd) && isInt6(imm12)) {
                CAndi(rd, imm12);
                return;
            }
        }

        EmitI(imm12, rs1.index(), 0x7, rd.index(), 0x13);
    }

    // 0x1 Split: 0x0(6b) + imm12(6b)
    public void Slli(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && rd != Zero && shamt != 0) {
                CSlli(rd, shamt);
                return;
            }
        }

        EmitI6(0x0, shamt, rs1, 0x1, rd, 0x13);
    }

    // 0x5 Split: 0x0(6b) + imm12(6b)
    public void Srli(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && IsShortReg(rd) && shamt != 0) {
                CSrli(rd, shamt);
                return;
            }
        }

        EmitI6(0x0, shamt, rs1, 0x5, rd, 0x13);
    }

    // 0x5 Split: 0x10(6b) + imm12(6b)
    public void Srai(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);

        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && IsShortReg(rd) && shamt != 0) {
                CSrai(rd, shamt);
                return;
            }
        }

        EmitI6(0x10, shamt, rs1, 0x5, rd, 0x13);
    }

    // ALU instructions (RV32I): opcode = 0x33, funct3 from 0x0 ~ 0x7

    public void Add(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero) {
                if (rs1 != Zero || rs2 != Zero) {
                    if (rs1 == Zero) {
                        CHECK_NE(rs2, Zero);
                        CMv(rd, rs2);
                        return;
                    } else if (rs2 == Zero) {
                        CHECK_NE(rs1, Zero);
                        CMv(rd, rs1);
                        return;
                    } else if (rd == rs1) {
                        CHECK_NE(rs2, Zero);
                        CAdd(rd, rs2);
                        return;
                    } else if (rd == rs2) {
                        CHECK_NE(rs1, Zero);
                        CAdd(rd, rs1);
                        return;
                    }
                } else {
                    // TODO: we use clang for testing assembler and unfortunately it (clang 18.0.1) does not
                    // support conversion from 'add rd, Zero, Zero' into 'c.li. rd, 0' so once clang supports it
                    // the lines below should be uncommented

                    // CLi(rd, 0);
                    // return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Sub(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && IsShortReg(rd) && IsShortReg(rs2)) {
                CSub(rd, rs2);
                return;
            }
        }

        EmitR(0x20, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Slt(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x02, rd.index(), 0x33);
    }

    public void Sltu(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x03, rd.index(), 0x33);
    }

    public void Xor(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (IsShortReg(rd)) {
                if (rd == rs1 && IsShortReg(rs2)) {
                    CXor(rd, rs2);
                    return;
                } else if (rd == rs2 && IsShortReg(rs1)) {
                    CXor(rd, rs1);
                    return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x04, rd.index(), 0x33);
    }

    public void Or(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (IsShortReg(rd)) {
                if (rd == rs1 && IsShortReg(rs2)) {
                    COr(rd, rs2);
                    return;
                } else if (rd == rs2 && IsShortReg(rs1)) {
                    COr(rd, rs1);
                    return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x06, rd.index(), 0x33);
    }

    public void And(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (IsShortReg(rd)) {
                if (rd == rs1 && IsShortReg(rs2)) {
                    CAnd(rd, rs2);
                    return;
                } else if (rd == rs2 && IsShortReg(rs1)) {
                    CAnd(rd, rs1);
                    return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x07, rd.index(), 0x33);
    }

    public void Sll(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x01, rd.index(), 0x33);
    }

    public void Srl(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x05, rd.index(), 0x33);
    }

    public void Sra(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x20, rs2.index(), rs1.index(), 0x05, rd.index(), 0x33);
    }

    // 32bit Imm ALU instructions (RV64I): opcode = 0x1b, funct3 from 0x0, 0x1, 0x5

    public void Addiw(XRegister rd, XRegister rs1, int imm12) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd != Zero && isInt6(imm12)) {
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

    public void Slliw(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x0, shamt, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Srliw(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x0, shamt, rs1.index(), 0x5, rd.index(), 0x1b);
    }

    public void Sraiw(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 32);
        EmitR(0x20, shamt, rs1.index(), 0x5, rd.index(), 0x1b);
    }

    // 32bit ALU instructions (RV64I): opcode = 0x3b, funct3 from 0x0 ~ 0x7

    public void Addw(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (IsShortReg(rd)) {
                if (rd == rs1 && IsShortReg(rs2)) {
                    CAddw(rd, rs2);
                    return;
                } else if (rd == rs2 && IsShortReg(rs1)) {
                    CAddw(rd, rs1);
                    return;
                }
            }
        }

        EmitR(0x0, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Subw(XRegister rd, XRegister rs1, XRegister rs2) {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            if (rd == rs1 && IsShortReg(rd) && IsShortReg(rs2)) {
                CSubw(rd, rs2);
                return;
            }
        }

        EmitR(0x20, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Sllw(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x1, rd.index(), 0x3b);
    }

    public void Srlw(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x0, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Sraw(XRegister rd, XRegister rs1, XRegister rs2) {
        EmitR(0x20, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    // Environment call and breakpoint (RV32I), opcode = 0x73

    public void Ecall() {
        EmitI(0x0, 0x0, 0x0, 0x0, 0x73);
    }

    public void Ebreak() {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            CEbreak();
            return;
        }

        EmitI(0x1, 0x0, 0x0, 0x0, 0x73);
    }

    // Fence instruction (RV32I): opcode = 0xf, funct3 = 0

    public void Fence(int pred, int succ) {
        CHECK(isUInt4(pred));
        CHECK(isUInt4(succ));
        EmitI(/* normal fence */ 0x0 << 8 | pred << 4 | succ, 0x0, 0x0, 0x0, 0xf);
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
        AssertExtensionsEnabled(Riscv64Extension.kZifencei);
        EmitI(0x0, 0x0, 0x1, 0x0, 0xf);
    }

    //__________________________ RV64 "Zifencei" Instructions  END ___________________________//

    //_____________________________ RV64 "M" Instructions  START _____________________________//

    // RV32M Standard Extension: opcode = 0x33, funct3 from 0x0 ~ 0x7

    public void Mul(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);

        if (IsExtensionEnabled(Riscv64Extension.kZcb)) {
            if (IsShortReg(rd)) {
                if (rd == rs1 && IsShortReg(rs2)) {
                    CMul(rd, rs2);
                    return;
                } else if (rd == rs2 && IsShortReg(rs1)) {
                    CMul(rd, rs1);
                    return;
                }
            }
        }

        EmitR(0x1, rs2.index(), rs1.index(), 0x0, rd.index(), 0x33);
    }

    public void Mulh(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Mulhsu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x2, rd.index(), 0x33);
    }

    public void Mulhu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x3, rd.index(), 0x33);
    }

    public void Div(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Divu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Rem(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Remu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    // RV64M Standard Extension: opcode = 0x3b, funct3 0x0 and from 0x4 ~ 0x7

    public void Mulw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Divw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x4, rd.index(), 0x3b);
    }

    public void Divuw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Remw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x6, rd.index(), 0x3b);
    }

    public void Remuw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kM);
        EmitR(0x1, rs2.index(), rs1.index(), 0x7, rd.index(), 0x3b);
    }

    //______________________________ RV64 "M" Instructions  END ______________________________//

    //_____________________________ RV64 "A" Instructions  START _____________________________//

    public void LrW(XRegister rd, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        CHECK(aqrl != AqRl.kRelease);
        EmitR4(0x2, aqrl.value(), 0x0, rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void LrD(XRegister rd, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        CHECK(aqrl != AqRl.kRelease);
        EmitR4(0x2, aqrl.value(), 0x0, rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void ScW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        CHECK(aqrl != AqRl.kAcquire);
        EmitR4(0x3, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void ScD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        CHECK(aqrl != AqRl.kAcquire);
        EmitR4(0x3, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoSwapW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x1, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoSwapD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x1, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoAddW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x0, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoAddD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x0, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoXorW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x4, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoXorD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x4, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoAndW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0xc, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoAndD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0xc, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoOrW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x8, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoOrD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x8, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMinW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x10, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMinD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x10, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMaxW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x14, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMaxD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x14, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMinuW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x18, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMinuD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x18, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    public void AmoMaxuW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x1c, aqrl.value(), rs2.index(), rs1.index(), 0x2, rd.index(), 0x2f);
    }

    public void AmoMaxuD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl) {
        AssertExtensionsEnabled(Riscv64Extension.kA);
        EmitR4(0x1c, aqrl.value(), rs2.index(), rs1.index(), 0x3, rd.index(), 0x2f);
    }

    //_____________________________ RV64 "A" Instructions  END _______________________________//

    //___________________________ RV64 "Zicsr" Instructions  START ___________________________//

    // "Zicsr" Standard Extension, opcode = 0x73, funct3 from 0x1 ~ 0x3 and 0x5 ~ 0x7

    public void Csrrw(XRegister rd, int csr, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x1, rd.index(), 0x73);
    }

    public void Csrrs(XRegister rd, int csr, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x73);
    }

    public void Csrrc(XRegister rd, int csr, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int offset = ToInt12(csr);
        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x73);
    }

    public void Csrrwi(XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x5, rd.index(), 0x73);
    }

    public void Csrrsi(XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x6, rd.index(), 0x73);
    }

    public void Csrrci(XRegister rd, int csr, int uimm5) {
        AssertExtensionsEnabled(Riscv64Extension.kZicsr);
        int i = ToInt12(csr);
        EmitI(i, uimm5, 0x7, rd.index(), 0x73);
    }

    //____________________________ RV64 "Zicsr" Instructions  END ____________________________//

    //_____________________________ RV64 "FD" Instructions  START ____________________________//

    // FP load/store instructions (RV32F+RV32D): opcode = 0x07, 0x27

    public void FLw(FRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kF);
        EmitI(offset, rs1.index(), 0x2, rd.index(), 0x07);
    }

    public void FLd(FRegister rd, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kD);

        if (IsExtensionEnabled(Riscv64Extension.kZcd)) {
            if (rs1 == SP && isUInt9(offset) && isAligned8(offset)) {
                CFLdsp(rd, offset);
                return;
            } else if (IsShortReg(rd) && IsShortReg(rs1) && isUInt8(offset) && isAligned8(offset)) {
                CFLd(rd, rs1, offset);
                return;
            }
        }

        EmitI(offset, rs1.index(), 0x3, rd.index(), 0x07);
    }

    public void FSw(FRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kF);
        EmitS(offset, rs2.index(), rs1.index(), 0x2, 0x27);
    }

    public void FSd(FRegister rs2, XRegister rs1, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kD);

        if (IsExtensionEnabled(Riscv64Extension.kZcd)) {
            if (rs1 == SP && isUInt9(offset) && isAligned8(offset)) {
                CFSdsp(rs2, offset);
                return;
            } else if (IsShortReg(rs2) && IsShortReg(rs1) && isUInt8(offset) && isAligned8(offset)) {
                CFSd(rs2, rs1, offset);
                return;
            }
        }

        EmitS(offset, rs2.index(), rs1.index(), 0x3, 0x27);
    }

    // FP FMA instructions (RV32F+RV32D): opcode = 0x43, 0x47, 0x4b, 0x4f

    public void FMAddS(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x43);
    }

    public void FMAddD(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x43);
    }

    public void FMSubS(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x47);
    }

    public void FMSubD(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x47);
    }

    public void FNMSubS(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4b);
    }

    public void FNMSubD(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4b);
    }

    public void FNMAddS(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR4(rs3.index(), 0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4f);
    }

    public void FNMAddD(
            FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR4(rs3.index(), 0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x4f);
    }

    // Simple FP instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b0XXXX0D

    public void FAddS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x0, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FAddD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x1, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSubS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x4, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSubD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x5, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FMulS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x8, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FMulD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x9, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FDivS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0xc, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FDivD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0xd, rs2.index(), rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSqrtS(FRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x2c, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSqrtD(FRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x2d, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FSgnjS(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FSgnjD(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FSgnjnS(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FSgnjnD(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FSgnjxS(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FSgnjxD(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x11, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FMinS(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x14, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMinD(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x15, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMaxS(FRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x14, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FMaxD(FRegister rd, FRegister rs1, FRegister rs2) {
        EmitR(0x15, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
        AssertExtensionsEnabled(Riscv64Extension.kD);
    }

    public void FCvtSD(FRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF, Riscv64Extension.kD);
        EmitR(0x20, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDS(FRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF, Riscv64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x21, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    // FP compare instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b101000D

    public void FEqS(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FEqD(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x2, rd.index(), 0x53);
    }

    public void FLtS(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FLtD(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FLeS(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x50, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FLeD(XRegister rd, FRegister rs1, FRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x51, rs2.index(), rs1.index(), 0x0, rd.index(), 0x53);
    }

    // FP conversion instructions (RV32F+RV32D+RV64F+RV64D): opcode = 0x53, funct7 = 0b110X00D

    public void FCvtWS(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x60, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWD(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x61, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWuS(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x60, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtWuD(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x61, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLS(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x60, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLD(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x61, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLuS(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x60, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtLuD(XRegister rd, FRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x61, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSW(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x68, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDW(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x69, 0x0, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSWu(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x68, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDWu(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        // Note: The `frm` is useless, the result can represent every value of the source exactly.
        EmitR(0x69, 0x1, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSL(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x68, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDL(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x69, 0x2, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtSLu(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x68, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    public void FCvtDLu(FRegister rd, XRegister rs1, FPRoundingMode frm) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x69, 0x3, rs1.index(), frm.value(), rd.index(), 0x53);
    }

    // FP move instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x0, funct7 = 0b111X00D

    public void FMvXW(XRegister rd, FRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x70, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvXD(XRegister rd, FRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x71, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvWX(FRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x78, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    public void FMvDX(FRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x79, 0x0, rs1.index(), 0x0, rd.index(), 0x53);
    }

    // FP classify instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x1, funct7 = 0b111X00D

    public void FClassS(XRegister rd, FRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kF);
        EmitR(0x70, 0x0, rs1.index(), 0x1, rd.index(), 0x53);
    }

    public void FClassD(XRegister rd, FRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kD);
        EmitR(0x71, 0x0, rs1.index(), 0x1, rd.index(), 0x53);
    }

    //_____________________________ RV64 "FD" Instructions  END ______________________________//

    //______________________________ RV64 "C" Instructions  START ____________________________//

    public void CLwsp(XRegister rd, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        int imm6 = ExtractOffset52_76(offset);
        EmitCI(0b010, rd.index(), imm6, 0b10);
    }

    public void CLdsp(XRegister rd, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        int imm6 = ExtractOffset53_86(offset);
        EmitCI(0b011, rd.index(), imm6, 0b10);
    }

    public void CFLdsp(FRegister rd, int offset) {
        AssertExtensionsEnabled(
                Riscv64Extension.kLoadStore, Riscv64Extension.kZcd, Riscv64Extension.kD);
        int imm6 = ExtractOffset53_86(offset);
        EmitCI(0b001, rd.index(), imm6, 0b10);
    }

    public void CSwsp(XRegister rs2, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int offset6 = ExtractOffset52_76(offset);
        EmitCSS(0b110, offset6, rs2.index(), 0b10);
    }

    public void CSdsp(XRegister rs2, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int offset6 = ExtractOffset53_86(offset);
        EmitCSS(0b111, offset6, rs2.index(), 0b10);
    }

    public void CFSdsp(FRegister rs2, int offset) {
        AssertExtensionsEnabled(
                Riscv64Extension.kLoadStore, Riscv64Extension.kZcd, Riscv64Extension.kD);
        int offset6 = ExtractOffset53_86(offset);
        EmitCSS(0b101, offset6, rs2.index(), 0b10);
    }

    public void CLw(XRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int imm5 = ExtractOffset52_6(offset);
        EmitCM(0b010, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CLd(XRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b011, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CFLd(FRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(
                Riscv64Extension.kLoadStore, Riscv64Extension.kZcd, Riscv64Extension.kD);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b001, imm5, rs1_s, rd_s.index(), 0b00);
    }

    public void CSw(XRegister rs2_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int imm5 = ExtractOffset52_6(offset);
        EmitCM(0b110, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CSd(XRegister rs2_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZca);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b111, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CFSd(FRegister rs2_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(
                Riscv64Extension.kLoadStore, Riscv64Extension.kZcd, Riscv64Extension.kD);
        int imm5 = ExtractOffset53_76(offset);
        EmitCM(0b101, imm5, rs1_s, rs2_s.index(), 0b00);
    }

    public void CLi(XRegister rd, int imm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        CHECK(isInt6(imm));
        int imm6 = EncodeInt6(imm);
        EmitCI(0b010, rd.index(), imm6, 0b01);
    }

    public void CLui(XRegister rd, int nzimm6) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        CHECK_NE(rd, SP);
        CHECK(IsImmCLuiEncodable(nzimm6));
        EmitCI(0b011, rd.index(), nzimm6 & MaskLeastSignificant(6), 0b01);
    }

    public void CAddi(XRegister rd, int nzimm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        CHECK_NE(nzimm, 0);
        int imm6 = EncodeInt6(nzimm);
        EmitCI(0b000, rd.index(), imm6, 0b01);
    }

    public void CAddiw(XRegister rd, int imm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        int imm6 = EncodeInt6(imm);
        EmitCI(0b001, rd.index(), imm6, 0b01);
    }

    public void CAddi16Sp(int nzimm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(nzimm, 0);
        CHECK(isAligned16(nzimm));
        CHECK(isInt10(nzimm));

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

    public void CAddi4Spn(XRegister rd_s, int nzuimm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(nzuimm, 0);
        CHECK(isAligned4(nzuimm));
        CHECK(isUInt10(nzuimm));

        // nzuimm[5:4|9:6|2|3]
        int uimm = (BitFieldExtract(nzuimm, 4, 2) << 6) |
                (BitFieldExtract(nzuimm, 6, 4) << 2) |
                (BitFieldExtract(nzuimm, 2, 1) << 1) |
                BitFieldExtract(nzuimm, 3, 1);

        EmitCIW(0b000, uimm, rd_s.index(), 0b00);
    }

    public void CSlli(XRegister rd, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK_NE(rd, Zero);
        EmitCI(0b000, rd.index(), shamt, 0b10);
    }

    public void CSrli(XRegister rd_s, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK(isUInt6(shamt));
        EmitCBArithmetic(0b100, 0b00, shamt, rd_s, 0b01);
    }

    public void CSrai(XRegister rd_s, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(shamt, 0);
        CHECK(isUInt6(shamt));
        EmitCBArithmetic(0b100, 0b01, shamt, rd_s, 0b01);
    }

    public void CAndi(XRegister rd_s, int imm) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK(isInt6(imm));
        EmitCBArithmetic(0b100, 0b10, imm, rd_s, 0b01);
    }

    public void CMv(XRegister rd, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        CHECK_NE(rs2, Zero);
        EmitCR(0b1000, rd, rs2, 0b10);
    }

    public void CAdd(XRegister rd, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rd, Zero);
        CHECK_NE(rs2, Zero);
        EmitCR(0b1001, rd, rs2, 0b10);
    }

    public void CAnd(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b11, rs2_s, 0b01);
    }

    public void COr(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b10, rs2_s, 0b01);
    }

    public void CXor(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b01, rs2_s, 0b01);
    }

    public void CSub(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100011, rd_s, 0b00, rs2_s, 0b01);
    }

    public void CAddw(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100111, rd_s, 0b01, rs2_s, 0b01);
    }

    public void CSubw(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCAReg(0b100111, rd_s, 0b00, rs2_s, 0b01);
    }

    // "Zcb" Standard Extension, part of "C", opcode = 0b00, 0b01, funct3 = 0b100.

    public void CLbu(XRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZcb);
        EmitCAReg(0b100000, rs1_s, EncodeOffset0_1(offset), rd_s, 0b00);
    }

    public void CLhu(XRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZcb);
        CHECK(isUInt2(offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100001, rs1_s, BitFieldExtract(offset, 1, 1), rd_s, 0b00);
    }

    public void CLh(XRegister rd_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZcb);
        CHECK(isUInt2(offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100001, rs1_s, 0b10 | BitFieldExtract(offset, 1, 1), rd_s, 0b00);
    }

    public void CSb(XRegister rs2_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZcb);
        EmitCAReg(0b100010, rs1_s, EncodeOffset0_1(offset), rs2_s, 0b00);
    }

    public void CSh(XRegister rs2_s, XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kZcb);
        CHECK(isUInt2(offset));
        CHECK_ALIGNED(offset, 2);
        EmitCAReg(0b100011, rs1_s, BitFieldExtract(offset, 1, 1), rs2_s, 0b00);
    }

    public void CZextB(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b000, 0b01);
    }

    public void CSextB(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb, Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b001, 0b01);
    }

    public void CZextH(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb, Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b010, 0b01);
    }

    public void CSextH(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb, Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b011, 0b01);
    }

    public void CZextW(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZba, Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b100, 0b01);
    }

    public void CNot(XRegister rd_rs1_s) {
        AssertExtensionsEnabled(Riscv64Extension.kZcb);
        EmitCAImm(0b100111, rd_rs1_s, 0b11, 0b101, 0b01);
    }

    public void CMul(XRegister rd_s, XRegister rs2_s) {
        AssertExtensionsEnabled(Riscv64Extension.kM, Riscv64Extension.kZcb);
        EmitCAReg(0b100111, rd_s, 0b10, rs2_s, 0b01);
    }

    public void CJ(int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCJ(0b101, offset, 0b01);
    }

    public void CJr(XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rs1, Zero);
        EmitCR(0b1000, rs1, Zero, 0b10);
    }

    public void CJalr(XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        CHECK_NE(rs1, Zero);
        EmitCR(0b1001, rs1, Zero, 0b10);
    }

    public void CBeqz(XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCBBranch(0b110, offset, rs1_s, 0b01);
    }

    public void CBnez(XRegister rs1_s, int offset) {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCBBranch(0b111, offset, rs1_s, 0b01);
    }

    public void CEbreak() {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCR(0b1001, Zero, Zero, 0b10);
    }

    public void CNop() {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        EmitCI(0b000, Zero.index(), 0, 0b01);
    }

    public void CUnimp() {
        AssertExtensionsEnabled(Riscv64Extension.kZca);
        Emit16(0x0);
    }

    //_____________________________ RV64 "C" Instructions  END _______________________________//

    //_____________________________ RV64 "Zba" Instructions  START ___________________________//

    public void AddUw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x4, rs2.index(), rs1.index(), 0x0, rd.index(), 0x3b);
    }

    public void Sh1Add(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x33);
    }

    public void Sh1AddUw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x2, rd.index(), 0x3b);
    }

    public void Sh2Add(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Sh2AddUw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x4, rd.index(), 0x3b);
    }

    public void Sh3Add(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Sh3AddUw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitR(0x10, rs2.index(), rs1.index(), 0x6, rd.index(), 0x3b);
    }

    public void SlliUw(XRegister rd, XRegister rs1, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZba);
        EmitI6(0x2, shamt, rs1, 0x1, rd, 0x1b);
    }

    //_____________________________ RV64 "Zba" Instructions  END _____________________________//

    //_____________________________ RV64 "Zbb" Instructions  START ___________________________//

    public void Andn(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    public void Orn(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Xnor(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x20, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Clz(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x0, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Clzw(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x0, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Ctz(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x1, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Ctzw(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x1, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Cpop(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x2, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void Cpopw(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x2, rs1.index(), 0x1, rd.index(), 0x1b);
    }

    public void Min(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x4, rd.index(), 0x33);
    }

    public void Minu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Max(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x6, rd.index(), 0x33);
    }

    public void Maxu(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x5, rs2.index(), rs1.index(), 0x7, rd.index(), 0x33);
    }

    public void Rol(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Rolw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x1, rd.index(), 0x3b);
    }

    public void Ror(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Rorw(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, rs2.index(), rs1.index(), 0x5, rd.index(), 0x3b);
    }

    public void Rori(XRegister rd, XRegister rs1, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        CHECK_LT(shamt, 64);
        EmitI6(0x18, shamt, rs1, 0x5, rd, 0x13);
    }

    public void Roriw(XRegister rd, XRegister rs1, int shamt) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        CHECK_LT(shamt, 32);
        EmitI6(0x18, shamt, rs1, 0x5, rd, 0x1b);
    }

    public void OrcB(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x14, 0x7, rs1.index(), 0x5, rd.index(), 0x13);
    }

    public void Rev8(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x35, 0x18, rs1.index(), 0x5, rd.index(), 0x13);
    }

    public void ZbbSextB(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x4, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void ZbbSextH(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x30, 0x5, rs1.index(), 0x1, rd.index(), 0x13);
    }

    public void ZbbZextH(XRegister rd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kZbb);
        EmitR(0x4, 0x0, rs1.index(), 0x4, rd.index(), 0x3b);
    }

    //_____________________________ RV64 "Zbb" Instructions  END ____________________________//

    //____________________________ RV64 "Zbs" Instructions  START ___________________________//

    public void Bclr(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitR(0x24, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Bclri(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitI6(0x12, shamt, rs1, 0x1, rd, 0x13);
    }

    public void Bext(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitR(0x24, rs2.index(), rs1.index(), 0x5, rd.index(), 0x33);
    }

    public void Bexti(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitI6(0x12, shamt, rs1, 0x5, rd, 0x13);
    }

    public void Binv(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitR(0x34, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Binvi(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitI6(0x1A, shamt, rs1, 0x1, rd, 0x13);
    }

    public void Bset(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitR(0x14, rs2.index(), rs1.index(), 0x1, rd.index(), 0x33);
    }

    public void Bseti(XRegister rd, XRegister rs1, int shamt) {
        CHECK_LT(shamt, 64);
        AssertExtensionsEnabled(Riscv64Extension.kZbs);
        EmitI6(0xA, shamt, rs1, 0x1, rd, 0x13);
    }

    //_____________________________ RV64 "Zbs" Instructions  END _____________________________//

    //______________________________ RVV "VSet" Instructions  START __________________________//

    public void VSetvli(XRegister rd, XRegister rs1, int vtypei) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(isUInt11(vtypei));
        EmitI(vtypei, rs1.index(), VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    public void VSetivli(XRegister rd, int uimm, int vtypei) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(isUInt10(vtypei));
        CHECK(isUInt5(uimm));
        EmitI((~0 << 10 | vtypei), uimm, VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    public void VSetvl(XRegister rd, XRegister rs1, XRegister rs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        EmitR(0x40, rs2.index(), rs1.index(), VAIEncoding.kOPCFG.value(), rd.index(), 0x57);
    }

    //_____________________________ RVV "VSet" Instructions  END _____________________________//

    //__________________________ RVV Load/Store Instructions  START __________________________//

    public void VLe8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLe16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLe32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLe64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSe8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSe16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSe32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSe64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLm(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01011, rs1.index(), VectorWidth.kMask.value(), vd.index(), 0x7);
    }

    public void VSm(VRegister vs3, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01011, rs1.index(), VectorWidth.kMask.value(), vs3.index(), 0x27);
    }

    public void VLe8ff(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLe16ff(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLe32ff(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLe64ff(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLse8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLse16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLse32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLse64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSse8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSse16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSse32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSse64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLoxei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSoxei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLseg2e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg2e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg2e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg2e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg3e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg3e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg3e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg3e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg4e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg4e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg4e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg4e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg5e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg5e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg5e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg5e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg6e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg6e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg6e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg6e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg7e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg7e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg7e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg7e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg8e8(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg8e16(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg8e32(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg8e64(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSseg2e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg2e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg2e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg2e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg3e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg3e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg3e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg3e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg4e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg4e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg4e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg4e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg5e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg5e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg5e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg5e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg6e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg6e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg6e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg6e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg7e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg7e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg7e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg7e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSseg8e8(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSseg8e16(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSseg8e32(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSseg8e64(VRegister vs3, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b00000, rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLseg2e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg2e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg2e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg2e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg3e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg3e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg3e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg3e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg4e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg4e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg4e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg4e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg5e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg5e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg5e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg5e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg6e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg6e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg6e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg6e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg7e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg7e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg7e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg7e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLseg8e8ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLseg8e16ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLseg8e32ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLseg8e64ff(VRegister vd, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, vm);
        EmitR(funct7, 0b10000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg2e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg2e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg2e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg2e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg3e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg3e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg3e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg3e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg4e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg4e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg4e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg4e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg5e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg5e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg5e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg5e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg6e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg6e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg6e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg6e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg7e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg7e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg7e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg7e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLsseg8e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLsseg8e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLsseg8e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLsseg8e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSsseg2e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg2e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg3e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg4e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg5e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg6e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg7e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSsseg8e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kStrided, vm);
        EmitR(funct7, rs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLuxseg2ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg2ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg3ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg4ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg5ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg6ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg7ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLuxseg8ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSuxseg2ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg2ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg3ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg4ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg5ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg6ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg7ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSuxseg8ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedUnordered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VLoxseg2ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg2ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg3ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg4ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg5ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg6ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg7ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VLoxseg8ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VSoxseg2ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg2ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg3ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k3, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg4ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg5ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k5, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg6ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k6, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg7ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k7, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k8.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k16.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k32.value(), vs3.index(), 0x27);
    }

    public void VSoxseg8ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kIndexedOrdered, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VectorWidth.k64.value(), vs3.index(), 0x27);
    }

    public void VL1re8(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL1re16(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL1re32(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL1re64(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL2re8(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL2re16(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL2re32(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL2re64(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 2), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL4re8(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL4re16(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL4re32(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL4re64(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 4), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL8re8(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k8.value(), vd.index(), 0x7);
    }

    public void VL8re16(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k16.value(), vd.index(), 0x7);
    }

    public void VL8re32(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k32.value(), vd.index(), 0x7);
    }

    public void VL8re64(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        CHECK_EQ((vd.index() % 8), 0);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.k64.value(), vd.index(), 0x7);
    }

    public void VL1r(VRegister vd, XRegister rs1) {
        VL1re8(vd, rs1);
    }

    public void VL2r(VRegister vd, XRegister rs1) {
        VL2re8(vd, rs1);
    }

    public void VL4r(VRegister vd, XRegister rs1) {
        VL4re8(vd, rs1);
    }

    public void VL8r(VRegister vd, XRegister rs1) {
        VL8re8(vd, rs1);
    }

    public void VS1r(VRegister vs3, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k1, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS2r(VRegister vs3, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k2, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS4r(VRegister vs3, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k4, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    public void VS8r(VRegister vs3, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kLoadStore, Riscv64Extension.kV);
        final int funct7 = EncodeRVVMemF7(Nf.k8, 0x0, MemAddressMode.kUnitStride, VM.kUnmasked);
        EmitR(funct7, 0b01000, rs1.index(), VectorWidth.kWholeR.value(), vs3.index(), 0x27);
    }

    //___________________________ RVV Load/Store Instructions  END ___________________________//

    //___________________________ RVV Arithmetic Instructions  START _________________________//

    public void VAdd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAdd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAdd_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRsub_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNeg_v(VRegister vd, VRegister vs2) {
        VRsub_vx(vd, vs2, Zero, VM.kUnmasked);
    }

    public void VMinu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMinu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMin_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMin_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMaxu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMaxu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMax_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMax_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAnd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAnd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAnd_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VOr_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VOr_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VOr_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VXor_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VXor_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VXor_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNot_v(VRegister vd, VRegister vs2, VM vm) {
        VXor_vi(vd, vs2, -1, vm);
    }

    public void VRgather_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VRgather_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VRgather_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001100, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSlideup_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSlideup_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VRgatherei16_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSlidedown_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSlidedown_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VAdc_vvm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VAdc_vxm(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VAdc_vim(VRegister vd, VRegister vs2, int imm5) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010000, VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMadc_vvm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMadc_vxm(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMadc_vim(VRegister vd, VRegister vs2, int imm5) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMadc_vv(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMadc_vx(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kUnmasked);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMadc_vi(VRegister vd, VRegister vs2, int imm5) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010001, VM.kUnmasked);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSbc_vvm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSbc_vxm(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsbc_vvm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsbc_vxm(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsbc_vv(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsbc_vx(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010011, VM.kUnmasked);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMerge_vvm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, VM.kV0_t);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMerge_vxm(VRegister vd, VRegister vs2, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, VM.kV0_t);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMerge_vim(VRegister vd, VRegister vs2, int imm5) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, VM.kV0_t);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMv_vv(VRegister vd, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, VM.kUnmasked);
        EmitR(funct7, V0.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMv_vx(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, VM.kUnmasked);
        EmitR(funct7, V0.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMv_vi(VRegister vd, int imm5) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, VM.kUnmasked);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, V0.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMseq_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMseq_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMseq_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsne_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsne_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsne_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsltu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsltu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgtu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        VMsltu_vv(vd, vs1, vs2, vm);
    }

    public void VMslt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMslt_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        VMslt_vv(vd, vs1, vs2, vm);
    }

    public void VMsleu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsleu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsleu_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsgeu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        VMsleu_vv(vd, vs1, vs2, vm);
    }

    public void VMsltu_vi(VRegister vd, VRegister vs2, int aimm5, VM vm) {
        if (aimm5 < 1 || aimm5 > 6) {
            throw new IllegalArgumentException("Immediate should be between [1, 16]: " + aimm5);
        }
        CHECK(isUInt4(aimm5 - 1));
        VMsleu_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsle_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VMsle_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsle_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsge_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        VMsle_vv(vd, vs1, vs2, vm);
    }

    public void VMslt_vi(VRegister vd, VRegister vs2, int aimm5, VM vm) {
        VMsle_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsgtu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgtu_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011110, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsgeu_vi(VRegister vd, VRegister vs2, int aimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        if (aimm5 < 1 || aimm5 > 6) {
            throw new IllegalArgumentException("Immediate should be between [1, 16]: " + aimm5);
        }
        CHECK(isUInt4(aimm5 - 1));
        VMsgtu_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VMsgt_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VMsgt_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VMsge_vi(VRegister vd, VRegister vs2, int aimm5, VM vm) {
        VMsgt_vi(vd, vs2, aimm5 - 1, vm);
    }

    public void VSaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSaddu_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSadd_vi(VRegister vd, VRegister vs2, int imm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        int vs1 = EncodeInt5(imm5);
        EmitR(funct7, vs2.index(), vs1, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSll_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSll_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSll_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSmul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void Vmv1r_v(VRegister vd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b100111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k1.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv2r_v(VRegister vd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_EQ(vd.index() % 2, 0);
        CHECK_EQ(vs2.index() % 2, 0);
        final int funct7 = EncodeRVVF7(0b100111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k2.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv4r_v(VRegister vd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_EQ(vd.index() % 4, 0);
        CHECK_EQ(vs2.index() % 4, 0);
        final int funct7 = EncodeRVVF7(0b100111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k4.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void Vmv8r_v(VRegister vd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_EQ(vd.index() % 8, 0);
        CHECK_EQ(vs2.index() % 8, 0);
        final int funct7 = EncodeRVVF7(0b100111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), Nf.k8.value(), VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSrl_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSrl_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSrl_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSra_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSra_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSra_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsrl_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsrl_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsrl_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VSsra_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VSsra_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VSsra_vi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNsrl_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNsrl_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNsrl_wi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNcvt_x_x_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        VNsrl_wx(vd, vs2, Zero, vm);
    }

    public void VNsra_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNsra_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNsra_wi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNclipu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNclipu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNclipu_wi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VNclip_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VNclip_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPIVX.value(), vd.index(), 0x57);
    }

    public void VNclip_wi(VRegister vd, VRegister vs2, int uimm5, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), uimm5, VAIEncoding.kOPIVI.value(), vd.index(), 0x57);
    }

    public void VWredsumu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VWredsum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPIVV.value(), vd.index(), 0x57);
    }

    public void VRedsum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedand_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedor_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedxor_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedminu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmin_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmaxu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRedmax_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VAsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VAsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VSlide1up_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VSlide1down_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VCompress_vm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMandn_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011000, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMand_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011001, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMmv_m(VRegister vd, VRegister vs2) {
        VMand_mm(vd, vs2, vs2);
    }

    public void VMor_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011010, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMxor_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011011, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMclr_m(VRegister vd) {
        VMxor_mm(vd, vd, vd);
    }

    public void VMorn_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011100, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMnand_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011101, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMnot_m(VRegister vd, VRegister vs2) {
        VMnand_mm(vd, vs2, vs2);
    }

    public void VMnor_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011110, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMxnor_mm(VRegister vd, VRegister vs2, VRegister vs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b011111, VM.kUnmasked);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMset_m(VRegister vd) {
        VMxnor_mm(vd, vd, vd);
    }

    public void VDivu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VDivu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VDiv_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VDiv_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VRemu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRemu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VRem_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VRem_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulhu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulhu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulhsu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulhsu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMulh_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMulh_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMadd_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VNmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VNmsub_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMacc_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VNmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VNmsac_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWcvtu_x_x_v(VRegister vd, VRegister vs, VM vm) {
        VWaddu_vx(vd, vs, Zero, vm);
    }

    public void VWadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWcvt_x_x_v(VRegister vd, VRegister vs, VM vm) {
        VWadd_vx(vd, vs, Zero, vm);
    }

    public void VWsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWaddu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWaddu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWadd_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWadd_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsubu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsubu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWsub_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWsub_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmulu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmulu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmulsu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmulsu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111010, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111011, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccu_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmaccu_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmacc_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccus_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VWmaccsu_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VWmaccsu_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VFadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFadd_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredusum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredosum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmin_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmin_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredmin_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmax_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmax_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b000110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFredmax_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b000111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnj_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnj_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFsgnjn_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnjn_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFneg_v(VRegister vd, VRegister vs) {
        VFsgnjn_vv(vd, vs, vs, VM.kUnmasked);
    }

    public void VFsgnjx_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsgnjx_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFabs_v(VRegister vd, VRegister vs) {
        VFsgnjx_vv(vd, vs, vs, VM.kUnmasked);
    }

    public void VFslide1up_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b001110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFslide1down_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b001111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmerge_vfm(VRegister vd, VRegister vs2, FRegister fs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK(vd != V0);
        final int funct7 = EncodeRVVF7(0b010111, VM.kV0_t);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmv_v_f(VRegister vd, FRegister fs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010111, VM.kUnmasked);
        EmitR(funct7, V0.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfeq_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfeq_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfle_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfle_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfge_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        VMfle_vv(vd, vs1, vs2, vm);
    }

    public void VMflt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMflt_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011011, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfgt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        VMflt_vv(vd, vs1, vs2, vm);
    }

    public void VMfne_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMfne_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfgt_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMfge_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b011111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFdiv_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFdiv_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFrdiv_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmul_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFrsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b100111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmadd_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmadd_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101001, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmsub_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmsub_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101011, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFnmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFnmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b101111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwredusum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110001, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b110010, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwredosum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b110011, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwadd_wf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwsub_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwsub_wf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b110110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmul_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111000, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111100, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwnmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwnmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111101, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111110, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFwnmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs1);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), vs1.index(), VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwnmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b111111, vm);
        EmitR(funct7, vs2.index(), fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VMv_s_x(VRegister vd, XRegister rs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, VM.kUnmasked);
        EmitR(funct7, 0b00000, rs1.index(), VAIEncoding.kOPMVX.value(), vd.index(), 0x57);
    }

    public void VMv_x_s(XRegister rd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, VM.kUnmasked);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VCpop_m(XRegister rd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VFirst_m(XRegister rd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, vm);
        EmitR(funct7, vs2.index(), 0b10001, VAIEncoding.kOPMVV.value(), rd.index(), 0x57);
    }

    public void VZext_vf8(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf8(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VZext_vf4(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00100, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf4(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00101, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VZext_vf2(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00110, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VSext_vf2(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00111, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VFmv_s_f(VRegister vd, FRegister fs1) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, VM.kUnmasked);
        EmitR(funct7, 0b00000, fs1.index(), VAIEncoding.kOPFVF.value(), vd.index(), 0x57);
    }

    public void VFmv_f_s(FRegister fd, VRegister vs2) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        final int funct7 = EncodeRVVF7(0b010000, VM.kUnmasked);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), fd.index(), 0x57);
    }

    public void VFcvt_xu_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_x_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_f_xu_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_f_x_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_rtz_xu_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFcvt_rtz_x_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b00111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_xu_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_x_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_xu_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_x_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_f_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_rtz_xu_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFwcvt_rtz_x_f_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b01111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_xu_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_x_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10001, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_xu_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10010, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_x_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10011, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_f_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rod_f_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10101, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rtz_xu_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10110, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFncvt_rtz_x_f_w(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010010, vm);
        EmitR(funct7, vs2.index(), 0b10111, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFsqrt_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFrsqrt7_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00100, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFrec7_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b00101, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VFclass_v(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010011, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPFVV.value(), vd.index(), 0x57);
    }

    public void VMsbf_m(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00001, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMsof_m(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00010, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VMsif_m(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b00011, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VIota_m(VRegister vd, VRegister vs2, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        CHECK(vd != vs2);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, vs2.index(), 0b10000, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    public void VId_v(VRegister vd, VM vm) {
        AssertExtensionsEnabled(Riscv64Extension.kV);
        CHECK_IMPLIES(vm == VM.kV0_t, vd != V0);
        final int funct7 = EncodeRVVF7(0b010100, vm);
        EmitR(funct7, V0.index(), 0b10001, VAIEncoding.kOPMVV.value(), vd.index(), 0x57);
    }

    //_________________________ RVV Arithmetic Instructions  END   ___________________________//

    //____________________________ RV64 MACRO Instructions  START ____________________________//

    // Pseudo instructions

    public void Nop() {
        Addi(Zero, Zero, 0);
    }

    public void Li(XRegister rd, long imm) {
        LoadImmediate(rd, imm, /*can_use_tmp=*/ false);
    }

    public void Mv(XRegister rd, XRegister rs) {
        Addi(rd, rs, 0);
    }

    public void Not(XRegister rd, XRegister rs) {
        Xori(rd, rs, -1);
    }

    public void Neg(XRegister rd, XRegister rs) {
        Sub(rd, Zero, rs);
    }

    public void NegW(XRegister rd, XRegister rs) {
        Subw(rd, Zero, rs);
    }

    public void SextB(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZbb)) {
            if (IsExtensionEnabled(Riscv64Extension.kZcb) && rd == rs && IsShortReg(rd)) {
                CSextB(rd);
            } else {
                ZbbSextB(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 8);
            Srai(rd, rd, kXlen - 8);
        }
    }

    public void SextH(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZbb)) {
            if (IsExtensionEnabled(Riscv64Extension.kZcb) && rd == rs && IsShortReg(rd)) {
                CSextH(rd);
            } else {
                ZbbSextH(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 16);
            Srai(rd, rd, kXlen - 16);
        }
    }

    public void SextW(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZca) && rd != Zero && (rd == rs || rs == Zero)) {
            if (rd == rs) {
                CAddiw(rd, 0);
            } else {
                CLi(rd, 0);
            }
        } else {
            Addiw(rd, rs, 0);
        }
    }

    public void ZextB(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZcb) && rd == rs && IsShortReg(rd)) {
            CZextB(rd);
        } else {
            Andi(rd, rs, 0xff);
        }
    }

    public void ZextH(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZbb)) {
            if (IsExtensionEnabled(Riscv64Extension.kZcb) && rd == rs && IsShortReg(rd)) {
                CZextH(rd);
            } else {
                ZbbZextH(rd, rs);
            }
        } else {
            Slli(rd, rs, kXlen - 16);
            Srli(rd, rd, kXlen - 16);
        }
    }

    public void ZextW(XRegister rd, XRegister rs) {
        if (IsExtensionEnabled(Riscv64Extension.kZba)) {
            if (IsExtensionEnabled(Riscv64Extension.kZcb) && rd == rs && IsShortReg(rd)) {
                CZextW(rd);
            } else {
                AddUw(rd, rs, Zero);
            }
        } else {
            Slli(rd, rs, kXlen - 32);
            Srli(rd, rd, kXlen - 32);
        }
    }

    public void Seqz(XRegister rd, XRegister rs) {
        Sltiu(rd, rs, 1);
    }

    public void Snez(XRegister rd, XRegister rs) {
        Sltu(rd, Zero, rs);
    }

    public void Sltz(XRegister rd, XRegister rs) {
        Slt(rd, rs, Zero);
    }

    public void Sgtz(XRegister rd, XRegister rs) {
        Slt(rd, Zero, rs);
    }

    public void FMvS(FRegister rd, FRegister rs) {
        FSgnjS(rd, rs, rs);
    }

    public void FAbsS(FRegister rd, FRegister rs) {
        FSgnjxS(rd, rs, rs);
    }

    public void FNegS(FRegister rd, FRegister rs) {
        FSgnjnS(rd, rs, rs);
    }

    public void FMvD(FRegister rd, FRegister rs) {
        FSgnjD(rd, rs, rs);
    }

    public void FAbsD(FRegister rd, FRegister rs) {
        FSgnjxD(rd, rs, rs);
    }

    public void FNegD(FRegister rd, FRegister rs) {
        FSgnjnD(rd, rs, rs);
    }

    public void Beqz(XRegister rs, int offset) {
        Beq(rs, Zero, offset);
    }

    public void Bnez(XRegister rs, int offset) {
        Bne(rs, Zero, offset);
    }

    public void Blez(XRegister rt, int offset) {
        Bge(Zero, rt, offset);
    }

    public void Bgez(XRegister rt, int offset) {
        Bge(rt, Zero, offset);
    }

    public void Bltz(XRegister rt, int offset) {
        Blt(rt, Zero, offset);
    }

    public void Bgtz(XRegister rt, int offset) {
        Blt(Zero, rt, offset);
    }

    public void Bgt(XRegister rs, XRegister rt, int offset) {
        Blt(rt, rs, offset);
    }

    public void Ble(XRegister rs, XRegister rt, int offset) {
        Bge(rt, rs, offset);
    }

    public void Bgtu(XRegister rs, XRegister rt, int offset) {
        Bltu(rt, rs, offset);
    }

    public void Bleu(XRegister rs, XRegister rt, int offset) {
        Bgeu(rt, rs, offset);
    }

    public void J(int offset) {
        Jal(Zero, offset);
    }

    public void Jal(int offset) {
        Jal(RA, offset);
    }

    public void Jr(XRegister rs) {
        Jalr(Zero, rs, 0);
    }

    public void Jalr(XRegister rs) {
        Jalr(RA, rs, 0);
    }

    public void Jalr(XRegister rd, XRegister rs) {
        Jalr(rd, rs, 0);
    }

    public void Ret() {
        Jalr(Zero, RA, 0);
    }

    public void RdCycle(XRegister rd) {
        Csrrs(rd, 0xc00, Zero);
    }

    public void RdTime(XRegister rd) {
        Csrrs(rd, 0xc01, Zero);
    }

    public void RdInstret(XRegister rd) {
        Csrrs(rd, 0xc02, Zero);
    }

    public void Csrr(XRegister rd, int csr) {
        Csrrs(rd, csr, Zero);
    }

    public void Csrw(int csr, XRegister rs) {
        Csrrw(Zero, csr, rs);
    }

    public void Csrs(int csr, XRegister rs) {
        Csrrs(Zero, csr, rs);
    }

    public void Csrc(int csr, XRegister rs) {
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

//public void Loadb(XRegister rd, XRegister rs1, int offset) {
//  LoadFromOffset(Riscv64Assembler::Lb, rd, rs1, offset);
//}
//
//public void Loadh(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Lh, rd, rs1, offset);
//}
//
//public void Loadw(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Lw, rd, rs1, offset);
//}
//
//public void Loadd(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Ld,rd, rs1, offset);
//}
//
//public void Loadbu(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Lbu,rd, rs1, offset);
//}
//
//public void Loadhu(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Lhu,rd, rs1, offset);
//}
//
//public void Loadwu(XRegister rd, XRegister rs1, int offset) {
//    LoadFromOffset(Riscv64Assembler::Lwu,rd, rs1, offset);
//}
//
//public void Storeb(XRegister rs2, XRegister rs1, int offset) {
//  StoreToOffset(Riscv64Assembler::Sb, rs2, rs1, offset);
//}
//
//public void Storeh(XRegister rs2, XRegister rs1, int offset) {
//  StoreToOffset(Riscv64Assembler::Sh,rs2, rs1, offset);
//}
//
//public void Storew(XRegister rs2, XRegister rs1, int offset) {
//  StoreToOffset(Riscv64Assembler::Sw,rs2, rs1, offset);
//}
//
//public void Stored(XRegister rs2, XRegister rs1, int offset) {
//  StoreToOffset(Riscv64Assembler::Sd,rs2, rs1, offset);
//}
//
//public void FLoadw(FRegister rd, XRegister rs1, int offset) {
//  FLoadFromOffset(Riscv64Assembler::FLw,rd, rs1, offset);
//}
//
//public void FLoadd(FRegister rd, XRegister rs1, int offset) {
//  FLoadFromOffset(Riscv64Assembler::FLd,rd, rs1, offset);
//}
//
//public void FStorew(FRegister rs2, XRegister rs1, int offset) {
//  FStoreToOffset(Riscv64Assembler::FSw,rs2, rs1, offset);
//}
//
//public void FStored(FRegister rs2, XRegister rs1, int offset) {
//  FStoreToOffset(Riscv64Assembler::FSd,rs2, rs1, offset);
//}
//
//public void LoadConst32(XRegister rd, int value) {
//  // No need to use a temporary register for 32-bit values.
//  LoadImmediate(rd, value, /*can_use_tmp=*/ false);
//}
//
//public void LoadConst64(XRegister rd, int64_t value) {
//  LoadImmediate(rd, value, /*can_use_tmp=*/ true);
//}
//
//template <typename ValueType, typename Addi, typename AddLarge>
//void AddConstImpl(Riscv64Assembler* assembler,
//                  XRegister rd,
//                  XRegister rs1,
//                  ValueType value,
//                  Addi&& addi,
//                  AddLarge&& add_large) {
//  ScratchRegisterScope srs(assembler);
//  // A temporary must be available for adjustment even if it's not needed.
//  // However, `rd` can be used as the temporary unless it's the same as `rs1` or SP.
//  CHECK_IMPLIES(rd == rs1 || rd == SP, srs.AvailableXRegisters() != 0);
//
//  if (isInt12(value)) {
//    addi(rd, rs1, value);
//    return;
//  }
//
//  constexpr int kPositiveValueSimpleAdjustment = 0x7ff;
//  constexpr int kHighestValueForSimpleAdjustment = 2 * kPositiveValueSimpleAdjustment;
//  constexpr int kNegativeValueSimpleAdjustment = -0x800;
//  constexpr int kLowestValueForSimpleAdjustment = 2 * kNegativeValueSimpleAdjustment;
//
//  if (rd != rs1 && rd != SP) {
//    srs.IncludeXRegister(rd);
//  }
//  XRegister tmp = srs.AllocateXRegister();
//  if (value >= 0 && value <= kHighestValueForSimpleAdjustment) {
//    addi(tmp, rs1, kPositiveValueSimpleAdjustment);
//    addi(rd, tmp, value - kPositiveValueSimpleAdjustment);
//  } else if (value < 0 && value >= kLowestValueForSimpleAdjustment) {
//    addi(tmp, rs1, kNegativeValueSimpleAdjustment);
//    addi(rd, tmp, value - kNegativeValueSimpleAdjustment);
//  } else {
//    add_large(rd, rs1, value, tmp);
//  }
//}

//public void AddConst32(XRegister rd, XRegister rs1, int value) {
//  CHECK_EQ((1 << rs1) & available_scratch_core_registers_, 0);
//  CHECK_EQ((1 << rd) & available_scratch_core_registers_, 0);
//  auto addiw = [&](XRegister rd, XRegister rs1, int value) { Addiw(rd, rs1, value); };
//  auto add_large = [&](XRegister rd, XRegister rs1, int value, XRegister tmp) {
//    LoadConst32(tmp, value);
//    Addw(rd, rs1, tmp);
//  };
//  AddConstImpl(this, rd, rs1, value, addiw, add_large);
//}
//
//public void AddConst64(XRegister rd, XRegister rs1, int64_t value) {
//  CHECK_EQ((1 << rs1) & available_scratch_core_registers_, 0);
//  CHECK_EQ((1 << rd) & available_scratch_core_registers_, 0);
//  auto addi = [&](XRegister rd, XRegister rs1, int value) { Addi(rd, rs1, value); };
//  auto add_large = [&](XRegister rd, XRegister rs1, int64_t value, XRegister tmp) {
//    // We may not have another scratch register for `LoadConst64()`, so use `Li()`.
//    // TODO(riscv64): Refactor `LoadImmediate()` so that we can reuse the code to detect
//    // when the code path using the scratch reg is beneficial, and use that path with a
//    // small modification - instead of adding the two parts togeter, add them individually
//    // to the input `rs1`. (This works as long as `rd` is not the same as `tmp`.)
//    Li(tmp, value);
//    Add(rd, rs1, tmp);
//  };
//  AddConstImpl(this, rd, rs1, value, addi, add_large);
//}

    public void Beqz(XRegister rs, Riscv64Label label, boolean is_bare) {
        Beq(rs, Zero, label, is_bare);
    }

    public void Bnez(XRegister rs, Riscv64Label label, boolean is_bare) {
        Bne(rs, Zero, label, is_bare);
    }

    public void Blez(XRegister rs, Riscv64Label label, boolean is_bare) {
        Ble(rs, Zero, label, is_bare);
    }

    public void Bgez(XRegister rs, Riscv64Label label, boolean is_bare) {
        Bge(rs, Zero, label, is_bare);
    }

    public void Bltz(XRegister rs, Riscv64Label label, boolean is_bare) {
        Blt(rs, Zero, label, is_bare);
    }

    public void Bgtz(XRegister rs, Riscv64Label label, boolean is_bare) {
        Bgt(rs, Zero, label, is_bare);
    }

    public void Beq(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondEQ, rs, rt);
    }

    public void Bne(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondNE, rs, rt);
    }

    public void Ble(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLE, rs, rt);
    }

    public void Bge(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGE, rs, rt);
    }

    public void Blt(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLT, rs, rt);
    }

    public void Bgt(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGT, rs, rt);
    }

    public void Bleu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLEU, rs, rt);
    }

    public void Bgeu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGEU, rs, rt);
    }

    public void Bltu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondLTU, rs, rt);
    }

    public void Bgtu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare) {
        Bcond(label, is_bare, BranchCondition.kCondGTU, rs, rt);
    }

    public void Jal(XRegister rd, Riscv64Label label, boolean is_bare) {
        Buncond(label, rd, is_bare);
    }

    public void J(Riscv64Label label, boolean is_bare) {
        Jal(Zero, label, is_bare);
    }

    public void Jal(Riscv64Label label, boolean is_bare) {
        Jal(RA, label, is_bare);
    }

//public void Loadw(XRegister rd, Literal  literal) {
//  CHECK_EQ(literal.getSize(), 4);
//  LoadLiteral(literal, rd, Branch.kLiteral);
//}
//
//public void Loadwu(XRegister rd, Literal  literal) {
//  CHECK_EQ(literal.getSize(), 4);
//  LoadLiteral(literal, rd, Branch.kLiteralUnsigned);
//}
//
//public void Loadd(XRegister rd, Literal  literal) {
//  CHECK_EQ(literal.getSize(), 8);
//  LoadLiteral(literal, rd, Branch.kLiteralLong);
//}
//
//public void FLoadw(FRegister rd, Literal  literal) {
//  CHECK_EQ(literal.getSize(), 4);
//  LoadLiteral(literal, rd, Branch.kLiteralFloat);
//}
//
//public void FLoadd(FRegister rd, Literal  literal) {
//  CHECK_EQ(literal.getSize(), 8);
//  LoadLiteral(literal, rd, Branch.kLiteralDouble);
//}

    public void Unimp() {
        if (IsExtensionEnabled(Riscv64Extension.kZca)) {
            CUnimp();
        } else {
            Emit32(0xC0001073);
        }
    }

    //______________________________ RV64 MACRO Instructions END _____________________________//
}
