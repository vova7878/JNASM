package com.v7878.jnasm.riscv64;

public interface RV64AssemblerI {
    // According to "The RISC-V Instruction Set Manual"

    // LUI/AUIPC (RV32I, with sign-extension on RV64I), opcode = 0x17, 0x37
    // Note: These take a 20-bit unsigned value to align with the clang assembler for testing,
    // but the value stored in the register shall actually be sign-extended to 64 bits.
    void Lui(XRegister rd, /* unsigned */ int uimm20);

    void Auipc(XRegister rd, /* unsigned */ int uimm20);

    // Jump instructions (RV32I), opcode = 0x67, 0x6f
    void Jal(XRegister rd, int offset);

    void Jalr(XRegister rd, XRegister rs1, int offset);

    // Branch instructions (RV32I), opcode = 0x63, funct3 from 0x0 ~ 0x1 and 0x4 ~ 0x7
    void Beq(XRegister rs1, XRegister rs2, int offset);

    void Bne(XRegister rs1, XRegister rs2, int offset);

    void Blt(XRegister rs1, XRegister rs2, int offset);

    void Bge(XRegister rs1, XRegister rs2, int offset);

    void Bltu(XRegister rs1, XRegister rs2, int offset);

    void Bgeu(XRegister rs1, XRegister rs2, int offset);

    // Load instructions (RV32I+RV64I): opcode = 0x03, funct3 from 0x0 ~ 0x6
    void Lb(XRegister rd, XRegister rs1, int offset);

    void Lh(XRegister rd, XRegister rs1, int offset);

    void Lw(XRegister rd, XRegister rs1, int offset);

    void Ld(XRegister rd, XRegister rs1, int offset);

    void Lbu(XRegister rd, XRegister rs1, int offset);

    void Lhu(XRegister rd, XRegister rs1, int offset);

    void Lwu(XRegister rd, XRegister rs1, int offset);

    // Store instructions (RV32I+RV64I): opcode = 0x23, funct3 from 0x0 ~ 0x3
    void Sb(XRegister rs2, XRegister rs1, int offset);

    void Sh(XRegister rs2, XRegister rs1, int offset);

    void Sw(XRegister rs2, XRegister rs1, int offset);

    void Sd(XRegister rs2, XRegister rs1, int offset);

    // IMM ALU instructions (RV32I): opcode = 0x13, funct3 from 0x0 ~ 0x7
    void Addi(XRegister rd, XRegister rs1, int imm12);

    void Slti(XRegister rd, XRegister rs1, int imm12);

    void Sltiu(XRegister rd, XRegister rs1, int imm12);

    void Xori(XRegister rd, XRegister rs1, int imm12);

    void Ori(XRegister rd, XRegister rs1, int imm12);

    void Andi(XRegister rd, XRegister rs1, int imm12);

    void Slli(XRegister rd, XRegister rs1, int shamt);

    void Srli(XRegister rd, XRegister rs1, int shamt);

    void Srai(XRegister rd, XRegister rs1, int shamt);

    // ALU instructions (RV32I): opcode = 0x33, funct3 from 0x0 ~ 0x7
    void Add(XRegister rd, XRegister rs1, XRegister rs2);

    void Sub(XRegister rd, XRegister rs1, XRegister rs2);

    void Slt(XRegister rd, XRegister rs1, XRegister rs2);

    void Sltu(XRegister rd, XRegister rs1, XRegister rs2);

    void Xor(XRegister rd, XRegister rs1, XRegister rs2);

    void Or(XRegister rd, XRegister rs1, XRegister rs2);

    void And(XRegister rd, XRegister rs1, XRegister rs2);

    void Sll(XRegister rd, XRegister rs1, XRegister rs2);

    void Srl(XRegister rd, XRegister rs1, XRegister rs2);

    void Sra(XRegister rd, XRegister rs1, XRegister rs2);

    // 32bit Imm ALU instructions (RV64I): opcode = 0x1b, funct3 from 0x0, 0x1, 0x5
    void Addiw(XRegister rd, XRegister rs1, int imm12);

    void Slliw(XRegister rd, XRegister rs1, int shamt);

    void Srliw(XRegister rd, XRegister rs1, int shamt);

    void Sraiw(XRegister rd, XRegister rs1, int shamt);

    // 32bit ALU instructions (RV64I): opcode = 0x3b, funct3 from 0x0 ~ 0x7
    void Addw(XRegister rd, XRegister rs1, XRegister rs2);

    void Subw(XRegister rd, XRegister rs1, XRegister rs2);

    void Sllw(XRegister rd, XRegister rs1, XRegister rs2);

    void Srlw(XRegister rd, XRegister rs1, XRegister rs2);

    void Sraw(XRegister rd, XRegister rs1, XRegister rs2);

    // Environment call and breakpoint (RV32I), opcode = 0x73
    void Ecall();

    void Ebreak();

    // Fence instruction (RV32I): opcode = 0xf, funct3 = 0
    void Fence(FenceType pred, FenceType succ);

    void FenceTso();

    // "Zifencei" Standard Extension, opcode = 0xf, funct3 = 1
    void FenceI();

    // RV32M Standard Extension: opcode = 0x33, funct3 from 0x0 ~ 0x7
    void Mul(XRegister rd, XRegister rs1, XRegister rs2);

    void Mulh(XRegister rd, XRegister rs1, XRegister rs2);

    void Mulhsu(XRegister rd, XRegister rs1, XRegister rs2);

    void Mulhu(XRegister rd, XRegister rs1, XRegister rs2);

    void Div(XRegister rd, XRegister rs1, XRegister rs2);

    void Divu(XRegister rd, XRegister rs1, XRegister rs2);

    void Rem(XRegister rd, XRegister rs1, XRegister rs2);

    void Remu(XRegister rd, XRegister rs1, XRegister rs2);

    // RV64M Standard Extension: opcode = 0x3b, funct3 0x0 and from 0x4 ~ 0x7
    void Mulw(XRegister rd, XRegister rs1, XRegister rs2);

    void Divw(XRegister rd, XRegister rs1, XRegister rs2);

    void Divuw(XRegister rd, XRegister rs1, XRegister rs2);

    void Remw(XRegister rd, XRegister rs1, XRegister rs2);

    void Remuw(XRegister rd, XRegister rs1, XRegister rs2);

    // RV32A/RV64A Standard Extension
    void LrW(XRegister rd, XRegister rs1, AqRl aqrl);

    void LrD(XRegister rd, XRegister rs1, AqRl aqrl);

    void ScW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void ScD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoSwapW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoSwapD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoAddW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoAddD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoXorW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoXorD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoAndW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoAndD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoOrW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoOrD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMinW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMinD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMaxW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMaxD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMinuW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMinuD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMaxuW(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    void AmoMaxuD(XRegister rd, XRegister rs2, XRegister rs1, AqRl aqrl);

    // "Zicsr" Standard Extension, opcode = 0x73, funct3 from 0x1 ~ 0x3 and 0x5 ~ 0x7
    void Csrrw(XRegister rd, int /* 12-bit */ csr, XRegister rs1);

    void Csrrs(XRegister rd, int /* 12-bit */ csr, XRegister rs1);

    void Csrrc(XRegister rd, int /* 12-bit */ csr, XRegister rs1);

    void Csrrwi(XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrrsi(XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrrci(XRegister rd, int /* 12-bit */ csr, /* unsigned */ int uimm5);

    // FP load/store instructions (RV32F+RV32D): opcode = 0x07, 0x27
    void FLw(FRegister rd, XRegister rs1, int offset);

    void FLd(FRegister rd, XRegister rs1, int offset);

    void FSw(FRegister rs2, XRegister rs1, int offset);

    void FSd(FRegister rs2, XRegister rs1, int offset);

    // FP FMA instructions (RV32F+RV32D): opcode = 0x43, 0x47, 0x4b, 0x4f
    void FMAddS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FMAddD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FMSubS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FMSubD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FNMSubS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FNMSubD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FNMAddS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    void FNMAddD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3, FPRoundingMode frm);

    // FP FMA instruction helpers passing the default rounding mode.
    default void FMAddS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FMAddS(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FMAddD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FMAddD(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FMSubS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FMSubS(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FMSubD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FMSubD(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FNMSubS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FNMSubS(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FNMSubD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FNMSubD(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FNMAddS(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FNMAddS(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    default void FNMAddD(FRegister rd, FRegister rs1, FRegister rs2, FRegister rs3) {
        FNMAddD(rd, rs1, rs2, rs3, FPRoundingMode.kDefault);
    }

    // Simple FP instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b0XXXX0D
    void FAddS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FAddD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FSubS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FSubD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FMulS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FMulD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FDivS(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FDivD(FRegister rd, FRegister rs1, FRegister rs2, FPRoundingMode frm);

    void FSqrtS(FRegister rd, FRegister rs1, FPRoundingMode frm);

    void FSqrtD(FRegister rd, FRegister rs1, FPRoundingMode frm);

    void FSgnjS(FRegister rd, FRegister rs1, FRegister rs2);

    void FSgnjD(FRegister rd, FRegister rs1, FRegister rs2);

    void FSgnjnS(FRegister rd, FRegister rs1, FRegister rs2);

    void FSgnjnD(FRegister rd, FRegister rs1, FRegister rs2);

    void FSgnjxS(FRegister rd, FRegister rs1, FRegister rs2);

    void FSgnjxD(FRegister rd, FRegister rs1, FRegister rs2);

    void FMinS(FRegister rd, FRegister rs1, FRegister rs2);

    void FMinD(FRegister rd, FRegister rs1, FRegister rs2);

    void FMaxS(FRegister rd, FRegister rs1, FRegister rs2);

    void FMaxD(FRegister rd, FRegister rs1, FRegister rs2);

    void FCvtSD(FRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtDS(FRegister rd, FRegister rs1, FPRoundingMode frm);

    // Simple FP instruction helpers passing the default rounding mode.
    default void FAddS(FRegister rd, FRegister rs1, FRegister rs2) {
        FAddS(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FAddD(FRegister rd, FRegister rs1, FRegister rs2) {
        FAddD(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FSubS(FRegister rd, FRegister rs1, FRegister rs2) {
        FSubS(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FSubD(FRegister rd, FRegister rs1, FRegister rs2) {
        FSubD(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FMulS(FRegister rd, FRegister rs1, FRegister rs2) {
        FMulS(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FMulD(FRegister rd, FRegister rs1, FRegister rs2) {
        FMulD(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FDivS(FRegister rd, FRegister rs1, FRegister rs2) {
        FDivS(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FDivD(FRegister rd, FRegister rs1, FRegister rs2) {
        FDivD(rd, rs1, rs2, FPRoundingMode.kDefault);
    }

    default void FSqrtS(FRegister rd, FRegister rs1) {
        FSqrtS(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FSqrtD(FRegister rd, FRegister rs1) {
        FSqrtD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtSD(FRegister rd, FRegister rs1) {
        FCvtSD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtDS(FRegister rd, FRegister rs1) {
        FCvtDS(rd, rs1, FPRoundingMode.kIgnored);
    }

    // FP compare instructions (RV32F+RV32D): opcode = 0x53, funct7 = 0b101000D
    void FEqS(XRegister rd, FRegister rs1, FRegister rs2);

    void FEqD(XRegister rd, FRegister rs1, FRegister rs2);

    void FLtS(XRegister rd, FRegister rs1, FRegister rs2);

    void FLtD(XRegister rd, FRegister rs1, FRegister rs2);

    void FLeS(XRegister rd, FRegister rs1, FRegister rs2);

    void FLeD(XRegister rd, FRegister rs1, FRegister rs2);

    // FP conversion instructions (RV32F+RV32D+RV64F+RV64D): opcode = 0x53, funct7 = 0b110X00D
    void FCvtWS(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtWD(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtWuS(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtWuD(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtLS(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtLD(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtLuS(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtLuD(XRegister rd, FRegister rs1, FPRoundingMode frm);

    void FCvtSW(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtDW(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtSWu(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtDWu(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtSL(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtDL(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtSLu(FRegister rd, XRegister rs1, FPRoundingMode frm);

    void FCvtDLu(FRegister rd, XRegister rs1, FPRoundingMode frm);

    // FP conversion instruction helpers passing the default rounding mode.
    default void FCvtWS(XRegister rd, FRegister rs1) {
        FCvtWS(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtWD(XRegister rd, FRegister rs1) {
        FCvtWD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtWuS(XRegister rd, FRegister rs1) {
        FCvtWuS(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtWuD(XRegister rd, FRegister rs1) {
        FCvtWuD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtLS(XRegister rd, FRegister rs1) {
        FCvtLS(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtLD(XRegister rd, FRegister rs1) {
        FCvtLD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtLuS(XRegister rd, FRegister rs1) {
        FCvtLuS(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtLuD(XRegister rd, FRegister rs1) {
        FCvtLuD(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtSW(FRegister rd, XRegister rs1) {
        FCvtSW(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtDW(FRegister rd, XRegister rs1) {
        FCvtDW(rd, rs1, FPRoundingMode.kIgnored);
    }

    default void FCvtSWu(FRegister rd, XRegister rs1) {
        FCvtSWu(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtDWu(FRegister rd, XRegister rs1) {
        FCvtDWu(rd, rs1, FPRoundingMode.kIgnored);
    }

    default void FCvtSL(FRegister rd, XRegister rs1) {
        FCvtSL(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtDL(FRegister rd, XRegister rs1) {
        FCvtDL(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtSLu(FRegister rd, XRegister rs1) {
        FCvtSLu(rd, rs1, FPRoundingMode.kDefault);
    }

    default void FCvtDLu(FRegister rd, XRegister rs1) {
        FCvtDLu(rd, rs1, FPRoundingMode.kDefault);
    }

    // FP move instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x0, funct7 = 0b111X00D
    void FMvXW(XRegister rd, FRegister rs1);

    void FMvXD(XRegister rd, FRegister rs1);

    void FMvWX(FRegister rd, XRegister rs1);

    void FMvDX(FRegister rd, XRegister rs1);

    // TODO: FPClassMaskType enum
    // FP classify instructions (RV32F+RV32D): opcode = 0x53, funct3 = 0x1, funct7 = 0b111X00D
    void FClassS(XRegister rd, FRegister rs1);

    void FClassD(XRegister rd, FRegister rs1);

    // "C" Standard Extension, Compresseed Instructions
    void CLwsp(XRegister rd, int offset);

    void CLdsp(XRegister rd, int offset);

    void CFLdsp(FRegister rd, int offset);

    void CSwsp(XRegister rs2, int offset);

    void CSdsp(XRegister rs2, int offset);

    void CFSdsp(FRegister rs2, int offset);

    void CLw(XRegister rd_s, XRegister rs1_s, int offset);

    void CLd(XRegister rd_s, XRegister rs1_s, int offset);

    void CFLd(FRegister rd_s, XRegister rs1_s, int offset);

    void CSw(XRegister rs2_s, XRegister rs1_s, int offset);

    void CSd(XRegister rs2_s, XRegister rs1_s, int offset);

    void CFSd(FRegister rs2_s, XRegister rs1_s, int offset);

    void CLi(XRegister rd, int imm);

    void CLui(XRegister rd, /* special */ int nzimm6);

    void CAddi(XRegister rd, /* special */ int nzimm);

    void CAddiw(XRegister rd, int imm);

    void CAddi16Sp(/* special */ int nzimm);

    void CAddi4Spn(XRegister rd_s, /* special */ int nzuimm);

    void CSlli(XRegister rd, int shamt);

    void CSrli(XRegister rd_s, int shamt);

    void CSrai(XRegister rd_s, int shamt);

    void CAndi(XRegister rd_s, int imm);

    void CMv(XRegister rd, XRegister rs2);

    void CAdd(XRegister rd, XRegister rs2);

    void CAnd(XRegister rd_s, XRegister rs2_s);

    void COr(XRegister rd_s, XRegister rs2_s);

    void CXor(XRegister rd_s, XRegister rs2_s);

    void CSub(XRegister rd_s, XRegister rs2_s);

    void CAddw(XRegister rd_s, XRegister rs2_s);

    void CSubw(XRegister rd_s, XRegister rs2_s);

    // "Zcb" Standard Extension, part of "C", opcode = 0b00, 0b01, funct3 = 0b100.
    void CLbu(XRegister rd_s, XRegister rs1_s, int offset);

    void CLhu(XRegister rd_s, XRegister rs1_s, int offset);

    void CLh(XRegister rd_s, XRegister rs1_s, int offset);

    void CSb(XRegister rd_s, XRegister rs1_s, int offset);

    void CSh(XRegister rd_s, XRegister rs1_s, int offset);

    void CZextB(XRegister rd_rs1_s);

    void CSextB(XRegister rd_rs1_s);

    void CZextH(XRegister rd_rs1_s);

    void CSextH(XRegister rd_rs1_s);

    void CZextW(XRegister rd_rs1_s);

    void CNot(XRegister rd_rs1_s);

    void CMul(XRegister rd_s, XRegister rs2_s);
    // "Zcb" Standard Extension End; resume "C" Standard Extension.
    // TODO(riscv64): Reorder "Zcb" after remaining "C" instructions.

    void CJ(int offset);

    void CJr(XRegister rs1);

    void CJalr(XRegister rs1);

    void CBeqz(XRegister rs1_s, int offset);

    void CBnez(XRegister rs1_s, int offset);

    void CEbreak();

    void CNop();

    void CUnimp();

    // "Zba" Standard Extension, opcode = 0x1b, 0x33 or 0x3b, funct3 and funct7 varies.
    void AddUw(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh1Add(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh1AddUw(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh2Add(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh2AddUw(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh3Add(XRegister rd, XRegister rs1, XRegister rs2);

    void Sh3AddUw(XRegister rd, XRegister rs1, XRegister rs2);

    void SlliUw(XRegister rd, XRegister rs1, int shamt);

    // "Zbb" Standard Extension, opcode = 0x13, 0x1b, 0x33 or 0x3b, funct3 and funct7 varies.
    // Note: 32-bit sext.b, sext.h and zext.h from the Zbb extension are explicitly
    // prefixed with "Zbb" to differentiate them from the utility macros.
    void Andn(XRegister rd, XRegister rs1, XRegister rs2);

    void Orn(XRegister rd, XRegister rs1, XRegister rs2);

    void Xnor(XRegister rd, XRegister rs1, XRegister rs2);

    void Clz(XRegister rd, XRegister rs1);

    void Clzw(XRegister rd, XRegister rs1);

    void Ctz(XRegister rd, XRegister rs1);

    void Ctzw(XRegister rd, XRegister rs1);

    void Cpop(XRegister rd, XRegister rs1);

    void Cpopw(XRegister rd, XRegister rs1);

    void Min(XRegister rd, XRegister rs1, XRegister rs2);

    void Minu(XRegister rd, XRegister rs1, XRegister rs2);

    void Max(XRegister rd, XRegister rs1, XRegister rs2);

    void Maxu(XRegister rd, XRegister rs1, XRegister rs2);

    void Rol(XRegister rd, XRegister rs1, XRegister rs2);

    void Rolw(XRegister rd, XRegister rs1, XRegister rs2);

    void Ror(XRegister rd, XRegister rs1, XRegister rs2);

    void Rorw(XRegister rd, XRegister rs1, XRegister rs2);

    void Rori(XRegister rd, XRegister rs1, int shamt);

    void Roriw(XRegister rd, XRegister rs1, int shamt);

    void OrcB(XRegister rd, XRegister rs1);

    void Rev8(XRegister rd, XRegister rs1);

    void ZbbSextB(XRegister rd, XRegister rs1);

    void ZbbSextH(XRegister rd, XRegister rs1);

    void ZbbZextH(XRegister rd, XRegister rs1);

    // "Zbs" Standard Extension, opcode = 0x13, or 0x33, funct3 and funct7 varies.
    void Bclr(XRegister rd, XRegister rs1, XRegister rs2);

    void Bclri(XRegister rd, XRegister rs1, int shamt);

    void Bext(XRegister rd, XRegister rs1, XRegister rs2);

    void Bexti(XRegister rd, XRegister rs1, int shamt);

    void Binv(XRegister rd, XRegister rs1, XRegister rs2);

    void Binvi(XRegister rd, XRegister rs1, int shamt);

    void Bset(XRegister rd, XRegister rs1, XRegister rs2);

    void Bseti(XRegister rd, XRegister rs1, int shamt);

    //____________________________ RISC-V Vector Instructions  START _____________________________//

    // Vector Conguration-Setting Instructions, opcode = 0x57, funct3 = 0x3
    void VSetvli(XRegister rd, XRegister rs1, int vtypei);

    void VSetivli(XRegister rd, /* unsigned */ int uimm, int vtypei);

    void VSetvl(XRegister rd, XRegister rs1, XRegister rs2);

    static int VTypeiValue(VectorMaskAgnostic vma,
                           VectorTailAgnostic vta,
                           SelectedElementWidth sew,
                           LengthMultiplier lmul) {
        return (vma.value() << 7) | (vta.value() << 6)
                | (sew.value() << 3) | lmul.value();
    }

    // Vector Unit-Stride Load/Store Instructions
    void VLe8(VRegister vd, XRegister rs1, VM vm);

    void VLe16(VRegister vd, XRegister rs1, VM vm);

    void VLe32(VRegister vd, XRegister rs1, VM vm);

    void VLe64(VRegister vd, XRegister rs1, VM vm);

    void VLm(VRegister vd, XRegister rs1);

    void VSe8(VRegister vs3, XRegister rs1, VM vm);

    void VSe16(VRegister vs3, XRegister rs1, VM vm);

    void VSe32(VRegister vs3, XRegister rs1, VM vm);

    void VSe64(VRegister vs3, XRegister rs1, VM vm);

    void VSm(VRegister vs3, XRegister rs1);

    // Vector unit-stride fault-only-first Instructions
    void VLe8ff(VRegister vd, XRegister rs1);

    void VLe16ff(VRegister vd, XRegister rs1);

    void VLe32ff(VRegister vd, XRegister rs1);

    void VLe64ff(VRegister vd, XRegister rs1);

    // Vector Strided Load/Store Instructions
    void VLse8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLse16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLse32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLse64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VSse8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSse16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSse32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSse64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    // Vector Indexed Load/Store Instructions
    void VLoxei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VSoxei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    // Vector Segment Load/Store

    // Vector Unit-Stride Segment Loads/Stores

    void VLseg2e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e64(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e8(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e16(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e32(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e64(VRegister vd, XRegister rs1, VM vm);

    void VSseg2e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg2e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg2e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg2e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg3e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg3e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg3e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg3e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg4e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg4e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg4e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg4e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg5e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg5e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg5e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg5e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg6e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg6e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg6e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg6e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg7e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg7e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg7e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg7e64(VRegister vs3, XRegister rs1, VM vm);

    void VSseg8e8(VRegister vs3, XRegister rs1, VM vm);

    void VSseg8e16(VRegister vs3, XRegister rs1, VM vm);

    void VSseg8e32(VRegister vs3, XRegister rs1, VM vm);

    void VSseg8e64(VRegister vs3, XRegister rs1, VM vm);

    // Vector Unit-Stride Fault-only-First Segment Loads

    void VLseg2e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg2e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg3e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg4e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg5e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg6e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg7e64ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e8ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e16ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e32ff(VRegister vd, XRegister rs1, VM vm);

    void VLseg8e64ff(VRegister vd, XRegister rs1, VM vm);

    // Vector Strided Segment Loads/Stores

    void VLsseg2e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg2e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg2e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg2e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg3e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg3e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg3e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg3e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg4e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg4e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg4e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg4e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg5e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg5e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg5e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg5e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg6e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg6e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg6e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg6e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg7e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg7e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg7e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg7e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg8e8(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg8e16(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg8e32(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VLsseg8e64(VRegister vd, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg2e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg2e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg2e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg2e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg3e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg3e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg3e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg3e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg4e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg4e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg4e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg4e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg5e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg5e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg5e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg5e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg6e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg6e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg6e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg6e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg7e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg7e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg7e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg7e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg8e8(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg8e16(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg8e32(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    void VSsseg8e64(VRegister vs3, XRegister rs1, XRegister rs2, VM vm);

    // Vector Indexed-unordered Segment Loads/Stores

    void VLuxseg2ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg2ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg2ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg2ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg3ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg3ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg3ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg3ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg4ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg4ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg4ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg4ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg5ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg5ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg5ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg5ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg6ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg6ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg6ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg6ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg7ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg7ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg7ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg7ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg8ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg8ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg8ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLuxseg8ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg2ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg2ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg2ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg2ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg3ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg3ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg3ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg3ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg4ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg4ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg4ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg4ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg5ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg5ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg5ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg5ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg6ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg6ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg6ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg6ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg7ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg7ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg7ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg7ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg8ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg8ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg8ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSuxseg8ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    // Vector Indexed-ordered Segment Loads/Stores

    void VLoxseg2ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg2ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg2ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg2ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg3ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg3ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg3ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg3ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg4ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg4ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg4ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg4ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg5ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg5ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg5ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg5ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg6ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg6ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg6ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg6ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg7ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg7ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg7ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg7ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg8ei8(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg8ei16(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg8ei32(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VLoxseg8ei64(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg2ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg2ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg2ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg2ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg3ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg3ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg3ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg3ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg4ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg4ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg4ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg4ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg5ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg5ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg5ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg5ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg6ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg6ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg6ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg6ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg7ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg7ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg7ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg7ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg8ei8(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg8ei16(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg8ei32(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    void VSoxseg8ei64(VRegister vs3, XRegister rs1, VRegister vs2, VM vm);

    // Vector Whole Register Load/Store Instructions

    void VL1re8(VRegister vd, XRegister rs1);

    void VL1re16(VRegister vd, XRegister rs1);

    void VL1re32(VRegister vd, XRegister rs1);

    void VL1re64(VRegister vd, XRegister rs1);

    void VL2re8(VRegister vd, XRegister rs1);

    void VL2re16(VRegister vd, XRegister rs1);

    void VL2re32(VRegister vd, XRegister rs1);

    void VL2re64(VRegister vd, XRegister rs1);

    void VL4re8(VRegister vd, XRegister rs1);

    void VL4re16(VRegister vd, XRegister rs1);

    void VL4re32(VRegister vd, XRegister rs1);

    void VL4re64(VRegister vd, XRegister rs1);

    void VL8re8(VRegister vd, XRegister rs1);

    void VL8re16(VRegister vd, XRegister rs1);

    void VL8re32(VRegister vd, XRegister rs1);

    void VL8re64(VRegister vd, XRegister rs1);

    void VL1r(VRegister vd, XRegister rs1);  // Pseudoinstruction equal to VL1re8

    void VL2r(VRegister vd, XRegister rs1);  // Pseudoinstruction equal to VL2re8

    void VL4r(VRegister vd, XRegister rs1);  // Pseudoinstruction equal to VL4re8

    void VL8r(VRegister vd, XRegister rs1);  // Pseudoinstruction equal to VL8re8

    void VS1r(VRegister vs3, XRegister rs1);  // Store {vs3} to address in a1

    void VS2r(VRegister vs3, XRegister rs1);  // Store {vs3}-{vs3 + 1} to address in a1

    void VS4r(VRegister vs3, XRegister rs1);  // Store {vs3}-{vs3 + 3} to address in a1

    void VS8r(VRegister vs3, XRegister rs1);  // Store {vs3}-{vs3 + 7} to address in a1

    // Vector Arithmetic Instruction

    // Vector vadd instructions, funct6 = 0b000000
    void VAdd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAdd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VAdd_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vsub instructions, funct6 = 0b000010
    void VSub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vrsub instructions, funct6 = 0b000011
    void VRsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VRsub_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instruction over VRsub_vi
    void VNeg_v(VRegister vd, VRegister vs2);

    // Vector vminu instructions, funct6 = 0b000100
    void VMinu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMinu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmin instructions, funct6 = 0b000101
    void VMin_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMin_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmaxu instructions, funct6 = 0b000110
    void VMaxu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMaxu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmax instructions, funct6 = 0b000111
    void VMax_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMax_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vand instructions, funct6 = 0b001001
    void VAnd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAnd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VAnd_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vor instructions, funct6 = 0b001010
    void VOr_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VOr_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VOr_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vxor instructions, funct6 = 0b001011
    void VXor_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VXor_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VXor_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instruction over VXor_vi
    void VNot_v(VRegister vd, VRegister vs2, VM vm);

    // Vector vrgather instructions, funct6 = 0b001100
    void VRgather_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VRgather_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VRgather_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vslideup instructions, funct6 = 0b001110
    void VSlideup_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSlideup_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vrgatherei16 instructions, funct6 = 0b001110
    void VRgatherei16_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vslidedown instructions, funct6 = 0b001111
    void VSlidedown_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSlidedown_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vadc instructions, funct6 = 0b010000
    void VAdc_vvm(VRegister vd, VRegister vs2, VRegister vs1);

    void VAdc_vxm(VRegister vd, VRegister vs2, XRegister rs1);

    void VAdc_vim(VRegister vd, VRegister vs2, int imm5);

    // Vector vmadc instructions, funct6 = 0b010001
    void VMadc_vvm(VRegister vd, VRegister vs2, VRegister vs1);

    void VMadc_vxm(VRegister vd, VRegister vs2, XRegister rs1);

    void VMadc_vim(VRegister vd, VRegister vs2, int imm5);

    // Vector vmadc instructions, funct6 = 0b010001
    void VMadc_vv(VRegister vd, VRegister vs2, VRegister vs1);

    void VMadc_vx(VRegister vd, VRegister vs2, XRegister rs1);

    void VMadc_vi(VRegister vd, VRegister vs2, int imm5);

    // Vector vsbc instructions, funct6 = 0b010010
    void VSbc_vvm(VRegister vd, VRegister vs2, VRegister vs1);

    void VSbc_vxm(VRegister vd, VRegister vs2, XRegister rs1);

    // Vector vmsbc instructions, funct6 = 0b010011
    void VMsbc_vvm(VRegister vd, VRegister vs2, VRegister vs1);

    void VMsbc_vxm(VRegister vd, VRegister vs2, XRegister rs1);

    void VMsbc_vv(VRegister vd, VRegister vs2, VRegister vs1);

    void VMsbc_vx(VRegister vd, VRegister vs2, XRegister rs1);

    // Vector vmerge instructions, funct6 = 0b010111, vm = 0
    void VMerge_vvm(VRegister vd, VRegister vs2, VRegister vs1);

    void VMerge_vxm(VRegister vd, VRegister vs2, XRegister rs1);

    void VMerge_vim(VRegister vd, VRegister vs2, int imm5);

    // Vector vmv instructions, funct6 = 0b010111, vm = 1, vs2 = v0
    void VMv_vv(VRegister vd, VRegister vs1);

    void VMv_vx(VRegister vd, XRegister rs1);

    void VMv_vi(VRegister vd, int imm5);

    // Vector vmseq instructions, funct6 = 0b011000
    void VMseq_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMseq_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMseq_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vmsne instructions, funct6 = 0b011001
    void VMsne_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMsne_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMsne_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vmsltu instructions, funct6 = 0b011010
    void VMsltu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMsltu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Pseudo-instruction over VMsltu_vv
    void VMsgtu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vmslt instructions, funct6 = 0b011011
    void VMslt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMslt_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Pseudo-instruction over VMslt_vv
    void VMsgt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vmsleu instructions, funct6 = 0b011100
    void VMsleu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMsleu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMsleu_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instructions over VMsleu_*
    void VMsgeu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMsltu_vi(VRegister vd, VRegister vs2, int aimm5, VM vm);

    // Vector vmsle instructions, funct6 = 0b011101
    void VMsle_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMsle_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMsle_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instructions over VMsle_*
    void VMsge_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMslt_vi(VRegister vd, VRegister vs2, int aimm5, VM vm);

    // Vector vmsgtu instructions, funct6 = 0b011110
    void VMsgtu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMsgtu_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instruction over VMsgtu_vi
    void VMsgeu_vi(VRegister vd, VRegister vs2, int aimm5, VM vm);

    // Vector vmsgt instructions, funct6 = 0b011111
    void VMsgt_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VMsgt_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Pseudo-instruction over VMsgt_vi
    void VMsge_vi(VRegister vd, VRegister vs2, int aimm5, VM vm);

    // Vector vsaddu instructions, funct6 = 0b100000
    void VSaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSaddu_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vsadd instructions, funct6 = 0b100001
    void VSadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSadd_vi(VRegister vd, VRegister vs2, int imm5, VM vm);

    // Vector vssubu instructions, funct6 = 0b100010
    void VSsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vssub instructions, funct6 = 0b100011
    void VSsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vsll instructions, funct6 = 0b100101
    void VSll_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSll_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSll_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vsmul instructions, funct6 = 0b100111
    void VSmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSmul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmv<nr>r.v instructions, funct6 = 0b100111
    void Vmv1r_v(VRegister vd, VRegister vs2);

    void Vmv2r_v(VRegister vd, VRegister vs2);

    void Vmv4r_v(VRegister vd, VRegister vs2);

    void Vmv8r_v(VRegister vd, VRegister vs2);

    // Vector vsrl instructions, funct6 = 0b101000
    void VSrl_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSrl_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSrl_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vsra instructions, funct6 = 0b101001
    void VSra_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSra_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSra_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vssrl instructions, funct6 = 0b101010
    void VSsrl_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSsrl_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSsrl_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vssra instructions, funct6 = 0b101011
    void VSsra_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VSsra_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VSsra_vi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vnsrl instructions, funct6 = 0b101100
    void VNsrl_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VNsrl_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VNsrl_wi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Pseudo-instruction over VNsrl_wx
    void VNcvt_x_x_w(VRegister vd, VRegister vs2, VM vm);

    // Vector vnsra instructions, funct6 = 0b101101
    void VNsra_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VNsra_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VNsra_wi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vnclipu instructions, funct6 = 0b101110
    void VNclipu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VNclipu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VNclipu_wi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vnclip instructions, funct6 = 0b101111
    void VNclip_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VNclip_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    void VNclip_wi(VRegister vd, VRegister vs2, /* unsigned */ int uimm5, VM vm);

    // Vector vwredsumu instructions, funct6 = 0b110000
    void VWredsumu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vwredsum instructions, funct6 = 0b110001
    void VWredsum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredsum instructions, funct6 = 0b000000
    void VRedsum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredand instructions, funct6 = 0b000001
    void VRedand_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredor instructions, funct6 = 0b000010
    void VRedor_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredxor instructions, funct6 = 0b000011
    void VRedxor_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredminu instructions, funct6 = 0b000100
    void VRedminu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredmin instructions, funct6 = 0b000101
    void VRedmin_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredmaxu instructions, funct6 = 0b000110
    void VRedmaxu_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vredmax instructions, funct6 = 0b000111
    void VRedmax_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vaaddu instructions, funct6 = 0b001000
    void VAaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vaadd instructions, funct6 = 0b001001
    void VAadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vasubu instructions, funct6 = 0b001010
    void VAsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vasub instructions, funct6 = 0b001011
    void VAsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VAsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vslide1up instructions, funct6 = 0b001110
    void VSlide1up_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vslide1down instructions, funct6 = 0b001111
    void VSlide1down_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vcompress instructions, funct6 = 0b010111
    void VCompress_vm(VRegister vd, VRegister vs2, VRegister vs1);

    // Vector vmandn instructions, funct6 = 0b011000
    void VMandn_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Vector vmand instructions, funct6 = 0b011001
    void VMand_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Pseudo-instruction over VMand_mm
    void VMmv_m(VRegister vd, VRegister vs2);

    // Vector vmor instructions, funct6 = 0b011010
    void VMor_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Vector vmxor instructions, funct6 = 0b011011
    void VMxor_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Pseudo-instruction over VMxor_mm
    void VMclr_m(VRegister vd);

    // Vector vmorn instructions, funct6 = 0b011100
    void VMorn_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Vector vmnand instructions, funct6 = 0b011101
    void VMnand_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Pseudo-instruction over VMnand_mm
    void VMnot_m(VRegister vd, VRegister vs2);

    // Vector vmnor instructions, funct6 = 0b011110
    void VMnor_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Vector vmxnor instructions, funct6 = 0b011111
    void VMxnor_mm(VRegister vd, VRegister vs2, VRegister vs1);

    // Pseudo-instruction over VMxnor_mm
    void VMset_m(VRegister vd);

    // Vector vdivu instructions, funct6 = 0b100000
    void VDivu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VDivu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vdiv instructions, funct6 = 0b100001
    void VDiv_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VDiv_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vremu instructions, funct6 = 0b100010
    void VRemu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VRemu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vrem instructions, funct6 = 0b100011
    void VRem_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VRem_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmulhu instructions, funct6 = 0b100100
    void VMulhu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMulhu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmul instructions, funct6 = 0b100101
    void VMul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmulhsu instructions, funct6 = 0b100110
    void VMulhsu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMulhsu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmulh instructions, funct6 = 0b100111
    void VMulh_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMulh_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vmadd instructions, funct6 = 0b101001
    void VMadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VMadd_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vnmsub instructions, funct6 = 0b101011
    void VNmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VNmsub_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vmacc instructions, funct6 = 0b101101
    void VMacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VMacc_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vnmsac instructions, funct6 = 0b101111
    void VNmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VNmsac_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vwaddu instructions, funct6 = 0b110000
    void VWaddu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWaddu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Pseudo-instruction over VWaddu_vx
    void VWcvtu_x_x_v(VRegister vd, VRegister vs, VM vm);

    // Vector vwadd instructions, funct6 = 0b110001
    void VWadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWadd_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Pseudo-instruction over VWadd_vx
    void VWcvt_x_x_v(VRegister vd, VRegister vs, VM vm);

    // Vector vwsubu instructions, funct6 = 0b110010
    void VWsubu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWsubu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwsub instructions, funct6 = 0b110011
    void VWsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWsub_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwaddu.w instructions, funct6 = 0b110100
    void VWaddu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWaddu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwadd.w instructions, funct6 = 0b110101
    void VWadd_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWadd_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwsubu.w instructions, funct6 = 0b110110
    void VWsubu_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWsubu_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwsub.w instructions, funct6 = 0b110111
    void VWsub_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWsub_wx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwmulu instructions, funct6 = 0b111000
    void VWmulu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWmulu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwmulsu instructions, funct6 = 0b111010
    void VWmulsu_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWmulsu_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwmul instructions, funct6 = 0b111011
    void VWmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VWmul_vx(VRegister vd, VRegister vs2, XRegister rs1, VM vm);

    // Vector vwmaccu instructions, funct6 = 0b111100
    void VWmaccu_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VWmaccu_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vwmacc instructions, funct6 = 0b111101
    void VWmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VWmacc_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vwmaccus instructions, funct6 = 0b111110
    void VWmaccus_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vwmaccsu instructions, funct6 = 0b111111
    void VWmaccsu_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VWmaccsu_vx(VRegister vd, XRegister rs1, VRegister vs2, VM vm);

    // Vector vfadd instructions, funct6 = 0b000000
    void VFadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFadd_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfredusum instructions, funct6 = 0b000001
    void VFredusum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfsub instructions, funct6 = 0b000010
    void VFsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfredosum instructions, funct6 = 0b000011
    void VFredosum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfmin instructions, funct6 = 0b000100
    void VFmin_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFmin_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfredmin instructions, funct6 = 0b000101
    void VFredmin_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfmax instructions, funct6 = 0b000110
    void VFmax_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFmax_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfredmax instructions, funct6 = 0b000111
    void VFredmax_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfsgnj instructions, funct6 = 0b001000
    void VFsgnj_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFsgnj_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfsgnjn instructions, funct6 = 0b001001
    void VFsgnjn_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFsgnjn_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Pseudo-instruction over VFsgnjn_vv
    void VFneg_v(VRegister vd, VRegister vs);

    // Vector vfsgnjx instructions, funct6 = 0b001010
    void VFsgnjx_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFsgnjx_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Pseudo-instruction over VFsgnjx_vv
    void VFabs_v(VRegister vd, VRegister vs);

    // Vector vfslide1up instructions, funct6 = 0b001110
    void VFslide1up_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfslide1down instructions, funct6 = 0b001111
    void VFslide1down_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfmerge/vfmv instructions, funct6 = 0b010111
    void VFmerge_vfm(VRegister vd, VRegister vs2, FRegister fs1);

    void VFmv_v_f(VRegister vd, FRegister fs1);

    // Vector vmfeq instructions, funct6 = 0b011000
    void VMfeq_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMfeq_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vmfle instructions, funct6 = 0b011001
    void VMfle_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMfle_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Pseudo-instruction over VMfle_vv
    void VMfge_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vmflt instructions, funct6 = 0b011011
    void VMflt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMflt_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Pseudo-instruction over VMflt_vv
    void VMfgt_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vmfne instructions, funct6 = 0b011100
    void VMfne_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VMfne_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vmfgt instructions, funct6 = 0b011101
    void VMfgt_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vmfge instructions, funct6 = 0b011111
    void VMfge_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfdiv instructions, funct6 = 0b100000
    void VFdiv_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFdiv_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfrdiv instructions, funct6 = 0b100001
    void VFrdiv_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfmul instructions, funct6 = 0b100100
    void VFmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFmul_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfrsub instructions, funct6 = 0b100111
    void VFrsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfmadd instructions, funct6 = 0b101000
    void VFmadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFmadd_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfnmadd instructions, funct6 = 0b101001
    void VFnmadd_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFnmadd_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfmsub instructions, funct6 = 0b101010
    void VFmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFmsub_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfnmsub instructions, funct6 = 0b101011
    void VFnmsub_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFnmsub_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfmacc instructions, funct6 = 0b101100
    void VFmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfnmacc instructions, funct6 = 0b101101
    void VFnmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFnmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfmsac instructions, funct6 = 0b101110
    void VFmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfnmsac instructions, funct6 = 0b101111
    void VFnmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFnmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfwadd instructions, funct6 = 0b110000
    void VFwadd_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFwadd_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfwredusum instructions, funct6 = 0b110001
    void VFwredusum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfwsub instructions, funct6 = 0b110010
    void VFwsub_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFwsub_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfwredosum instructions, funct6 = 0b110011
    void VFwredosum_vs(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    // Vector vfwadd.w instructions, funct6 = 0b110100
    void VFwadd_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFwadd_wf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfwsub.w instructions, funct6 = 0b110110
    void VFwsub_wv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFwsub_wf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfwmul instructions, funct6 = 0b111000
    void VFwmul_vv(VRegister vd, VRegister vs2, VRegister vs1, VM vm);

    void VFwmul_vf(VRegister vd, VRegister vs2, FRegister fs1, VM vm);

    // Vector vfwmacc instructions, funct6 = 0b111100
    void VFwmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFwmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfwnmacc instructions, funct6 = 0b111101
    void VFwnmacc_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFwnmacc_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfwmsac instructions, funct6 = 0b111110
    void VFwmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFwmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector vfwnmsac instructions, funct6 = 0b111111
    void VFwnmsac_vv(VRegister vd, VRegister vs1, VRegister vs2, VM vm);

    void VFwnmsac_vf(VRegister vd, FRegister fs1, VRegister vs2, VM vm);

    // Vector VRXUNARY0 kind instructions, funct6 = 0b010000
    void VMv_s_x(VRegister vd, XRegister rs1);

    // Vector VWXUNARY0 kind instructions, funct6 = 0b010000
    void VMv_x_s(XRegister rd, VRegister vs2);

    void VCpop_m(XRegister rd, VRegister vs2, VM vm);

    void VFirst_m(XRegister rd, VRegister vs2, VM vm);

    // Vector VXUNARY0 kind instructions, funct6 = 0b010010
    void VZext_vf8(VRegister vd, VRegister vs2, VM vm);

    void VSext_vf8(VRegister vd, VRegister vs2, VM vm);

    void VZext_vf4(VRegister vd, VRegister vs2, VM vm);

    void VSext_vf4(VRegister vd, VRegister vs2, VM vm);

    void VZext_vf2(VRegister vd, VRegister vs2, VM vm);

    void VSext_vf2(VRegister vd, VRegister vs2, VM vm);

    // Vector VRFUNARY0 kind instructions, funct6 = 0b010000
    void VFmv_s_f(VRegister vd, FRegister fs1);

    // Vector VWFUNARY0 kind instructions, funct6 = 0b010000
    void VFmv_f_s(FRegister fd, VRegister vs2);

    // Vector VFUNARY0 kind instructions, funct6 = 0b010010
    void VFcvt_xu_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFcvt_x_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFcvt_f_xu_v(VRegister vd, VRegister vs2, VM vm);

    void VFcvt_f_x_v(VRegister vd, VRegister vs2, VM vm);

    void VFcvt_rtz_xu_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFcvt_rtz_x_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_xu_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_x_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_f_xu_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_f_x_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_f_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_rtz_xu_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFwcvt_rtz_x_f_v(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_xu_f_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_x_f_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_f_xu_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_f_x_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_f_f_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_rod_f_f_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_rtz_xu_f_w(VRegister vd, VRegister vs2, VM vm);

    void VFncvt_rtz_x_f_w(VRegister vd, VRegister vs2, VM vm);

    // Vector VFUNARY1 kind instructions, funct6 = 0b010011
    void VFsqrt_v(VRegister vd, VRegister vs2, VM vm);

    void VFrsqrt7_v(VRegister vd, VRegister vs2, VM vm);

    void VFrec7_v(VRegister vd, VRegister vs2, VM vm);

    void VFclass_v(VRegister vd, VRegister vs2, VM vm);

    // Vector VMUNARY0 kind instructions, funct6 = 0b010100
    void VMsbf_m(VRegister vd, VRegister vs2, VM vm);

    void VMsof_m(VRegister vd, VRegister vs2, VM vm);

    void VMsif_m(VRegister vd, VRegister vs2, VM vm);

    void VIota_m(VRegister vd, VRegister vs2, VM vm);

    void VId_v(VRegister vd, VM vm);

    //____________________________ RISC-V Vector Instructions  END ____________________________//

    //____________________________ RV64 MACRO Instructions  START _____________________________//
    // These pseudo instructions are from "RISC-V Assembly Programmer's Manual".

    void Nop();

    void Li(XRegister rd, long imm);

    void Mv(XRegister rd, XRegister rs);

    void Not(XRegister rd, XRegister rs);

    void Neg(XRegister rd, XRegister rs);

    void NegW(XRegister rd, XRegister rs);

    void SextB(XRegister rd, XRegister rs);

    void SextH(XRegister rd, XRegister rs);

    void SextW(XRegister rd, XRegister rs);

    void ZextB(XRegister rd, XRegister rs);

    void ZextH(XRegister rd, XRegister rs);

    void ZextW(XRegister rd, XRegister rs);

    void Seqz(XRegister rd, XRegister rs);

    void Snez(XRegister rd, XRegister rs);

    void Sltz(XRegister rd, XRegister rs);

    void Sgtz(XRegister rd, XRegister rs);

    void FMvS(FRegister rd, FRegister rs);

    void FAbsS(FRegister rd, FRegister rs);

    void FNegS(FRegister rd, FRegister rs);

    void FMvD(FRegister rd, FRegister rs);

    void FAbsD(FRegister rd, FRegister rs);

    void FNegD(FRegister rd, FRegister rs);

    // Branch pseudo instructions
    void Beqz(XRegister rs, int offset);

    void Bnez(XRegister rs, int offset);

    void Blez(XRegister rs, int offset);

    void Bgez(XRegister rs, int offset);

    void Bltz(XRegister rs, int offset);

    void Bgtz(XRegister rs, int offset);

    void Bgt(XRegister rs, XRegister rt, int offset);

    void Ble(XRegister rs, XRegister rt, int offset);

    void Bgtu(XRegister rs, XRegister rt, int offset);

    void Bleu(XRegister rs, XRegister rt, int offset);

    // Jump pseudo instructions
    void J(int offset);

    void Jal(int offset);

    void Jr(XRegister rs);

    void Jalr(XRegister rs);

    void Jalr(XRegister rd, XRegister rs);

    void Ret();

    // Pseudo instructions for accessing control and status registers
    void RdCycle(XRegister rd);

    void RdTime(XRegister rd);

    void RdInstret(XRegister rd);

    // TODO: CSRAddress enum
    void Csrr(XRegister rd, int /* 12-bit */ csr);

    void Csrw(int /* 12-bit */ csr, XRegister rs);

    void Csrs(int /* 12-bit */ csr, XRegister rs);

    void Csrc(int /* 12-bit */ csr, XRegister rs);

    void Csrwi(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrsi(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    void Csrci(int /* 12-bit */ csr, /* unsigned */ int uimm5);

    // Load/store macros for arbitrary 32-bit offsets.
    void Loadb(XRegister rd, XRegister rs1, int offset);

    void Loadh(XRegister rd, XRegister rs1, int offset);

    void Loadw(XRegister rd, XRegister rs1, int offset);

    void Loadd(XRegister rd, XRegister rs1, int offset);

    void Loadbu(XRegister rd, XRegister rs1, int offset);

    void Loadhu(XRegister rd, XRegister rs1, int offset);

    void Loadwu(XRegister rd, XRegister rs1, int offset);

    void Storeb(XRegister rs2, XRegister rs1, int offset);

    void Storeh(XRegister rs2, XRegister rs1, int offset);

    void Storew(XRegister rs2, XRegister rs1, int offset);

    void Stored(XRegister rs2, XRegister rs1, int offset);

    void FLoadw(FRegister rd, XRegister rs1, int offset);

    void FLoadd(FRegister rd, XRegister rs1, int offset);

    void FStorew(FRegister rs2, XRegister rs1, int offset);

    void FStored(FRegister rs2, XRegister rs1, int offset);

    // Macros for loading constants.
    void LoadConst32(XRegister rd, int value);

    void LoadConst64(XRegister rd, long value);

    // Macros for adding constants.
    void AddConst32(XRegister rd, XRegister rs1, int value);

    void AddConst64(XRegister rd, XRegister rs1, long value);

    // Jumps and branches to a label.
    void Beqz(XRegister rs, Riscv64Label label, boolean is_bare);

    void Bnez(XRegister rs, Riscv64Label label, boolean is_bare);

    void Blez(XRegister rs, Riscv64Label label, boolean is_bare);

    void Bgez(XRegister rs, Riscv64Label label, boolean is_bare);

    void Bltz(XRegister rs, Riscv64Label label, boolean is_bare);

    void Bgtz(XRegister rs, Riscv64Label label, boolean is_bare);

    void Beq(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bne(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Ble(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bge(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Blt(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bgt(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bleu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bgeu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bltu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Bgtu(XRegister rs, XRegister rt, Riscv64Label label, boolean is_bare);

    void Jal(XRegister rd, Riscv64Label label, boolean is_bare);

    void J(Riscv64Label label, boolean is_bare);

    void Jal(Riscv64Label label, boolean is_bare);

    // Literal load.
    void Loadw(XRegister rd, Literal literal);

    void Loadwu(XRegister rd, Literal literal);

    void Loadd(XRegister rd, Literal literal);

    void FLoadw(XRegister tmp, FRegister rd, Literal literal);

    void FLoadd(XRegister tmp, FRegister rd, Literal literal);

    // Illegal instruction that triggers SIGILL.
    void Unimp();

    //_____________________________ RV64 MACRO Instructions END _____________________________//
}
