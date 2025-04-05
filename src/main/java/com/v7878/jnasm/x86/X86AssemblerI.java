package com.v7878.jnasm.x86;

import com.v7878.jnasm.Label;
import com.v7878.jnasm.common_x86.X86Condition;
import com.v7878.jnasm.common_x86.X86NearLabel;

/*
  Emit Machine Instructions.
 */
// TODO: javadoc
public interface X86AssemblerI {
    void call(X86CpuRegister reg);

    void call(X86Address address);

    void call(Label label);

    void call(X86ExternalLabel label);

    void pushl(X86CpuRegister reg);

    void pushl(X86Address address);

    void pushl(X86Immediate imm);

    void popl(X86CpuRegister reg);

    void popl(X86Address address);

    void movl(X86CpuRegister dst, X86Immediate src);

    void movl(X86CpuRegister dst, X86CpuRegister src);

    void movl(X86CpuRegister dst, X86Address src);

    void movl(X86Address dst, X86CpuRegister src);

    void movl(X86Address dst, X86Immediate imm);

    void movl(X86Address dst, Label lbl);

    void movntl(X86Address dst, X86CpuRegister src);

    void blsi(X86CpuRegister dst, X86CpuRegister src);  // no addr variant (for now)

    void blsmsk(X86CpuRegister dst, X86CpuRegister src);  // no addr variant (for now)

    void blsr(X86CpuRegister dst, X86CpuRegister src);  // no addr varianr (for now)

    void bswapl(X86CpuRegister dst);

    void bsfl(X86CpuRegister dst, X86CpuRegister src);

    void bsfl(X86CpuRegister dst, X86Address src);

    void bsrl(X86CpuRegister dst, X86CpuRegister src);

    void bsrl(X86CpuRegister dst, X86Address src);

    void popcntl(X86CpuRegister dst, X86CpuRegister src);

    void popcntl(X86CpuRegister dst, X86Address src);

    void rdtsc();

    void rorl(X86CpuRegister reg, X86Immediate imm);

    void rorl(X86CpuRegister operand, X86CpuRegister shifter);

    void roll(X86CpuRegister reg, X86Immediate imm);

    void roll(X86CpuRegister operand, X86CpuRegister shifter);

    void movzxb(X86CpuRegister dst, X86ByteRegister src);

    void movzxb(X86CpuRegister dst, X86Address src);

    void movsxb(X86CpuRegister dst, X86ByteRegister src);

    void movsxb(X86CpuRegister dst, X86Address src);

    void movb(X86CpuRegister dst, X86Address src);

    void movb(X86Address dst, X86ByteRegister src);

    void movb(X86Address dst, X86Immediate imm);

    void movzxw(X86CpuRegister dst, X86CpuRegister src);

    void movzxw(X86CpuRegister dst, X86Address src);

    void movsxw(X86CpuRegister dst, X86CpuRegister src);

    void movsxw(X86CpuRegister dst, X86Address src);

    void movw(X86CpuRegister dst, X86Address src);

    void movw(X86Address dst, X86CpuRegister src);

    void movw(X86Address dst, X86Immediate imm);

    void leal(X86CpuRegister dst, X86Address src);

    void cmovl(X86Condition condition, X86CpuRegister dst, X86CpuRegister src);

    void cmovl(X86Condition condition, X86CpuRegister dst, X86Address src);

    void setb(X86Condition condition, X86CpuRegister dst);

    void movaps(X86XmmRegister dst, X86XmmRegister src);     // move

    void movaps(X86XmmRegister dst, X86Address src);  // load aligned

    void movups(X86XmmRegister dst, X86Address src);  // load unaligned

    void movaps(X86Address dst, X86XmmRegister src);  // store aligned

    void movups(X86Address dst, X86XmmRegister src);  // store unaligned

    void vmovaps(X86XmmRegister dst, X86XmmRegister src);     // move

    void vmovaps(X86XmmRegister dst, X86Address src);  // load aligned

    void vmovups(X86XmmRegister dst, X86Address src);  // load unaligned

    void vmovaps(X86Address dst, X86XmmRegister src);  // store aligned

    void vmovups(X86Address dst, X86XmmRegister src);  // store unaligned

    void movss(X86XmmRegister dst, X86Address src);

    void movss(X86Address dst, X86XmmRegister src);

    void movss(X86XmmRegister dst, X86XmmRegister src);

    void movd(X86XmmRegister dst, X86CpuRegister src);

    void movd(X86CpuRegister dst, X86XmmRegister src);

    void addss(X86XmmRegister dst, X86XmmRegister src);

    void addss(X86XmmRegister dst, X86Address src);

    void subss(X86XmmRegister dst, X86XmmRegister src);

    void subss(X86XmmRegister dst, X86Address src);

    void mulss(X86XmmRegister dst, X86XmmRegister src);

    void mulss(X86XmmRegister dst, X86Address src);

    void divss(X86XmmRegister dst, X86XmmRegister src);

    void divss(X86XmmRegister dst, X86Address src);

    void addps(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void subps(X86XmmRegister dst, X86XmmRegister src);

    void mulps(X86XmmRegister dst, X86XmmRegister src);

    void divps(X86XmmRegister dst, X86XmmRegister src);

    void vmulps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vmulpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vdivps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vdivpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vaddps(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vsubps(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vsubpd(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vaddpd(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vfmadd213ss(X86XmmRegister acc, X86XmmRegister left, X86XmmRegister right);

    void vfmadd213sd(X86XmmRegister acc, X86XmmRegister left, X86XmmRegister right);

    void movapd(X86XmmRegister dst, X86XmmRegister src);     // move

    void movapd(X86XmmRegister dst, X86Address src);  // load aligned

    void movupd(X86XmmRegister dst, X86Address src);  // load unaligned

    void movapd(X86Address dst, X86XmmRegister src);  // store aligned

    void movupd(X86Address dst, X86XmmRegister src);  // store unaligned

    void vmovapd(X86XmmRegister dst, X86XmmRegister src);     // move

    void vmovapd(X86XmmRegister dst, X86Address src);  // load aligned

    void vmovupd(X86XmmRegister dst, X86Address src);  // load unaligned

    void vmovapd(X86Address dst, X86XmmRegister src);  // store aligned

    void vmovupd(X86Address dst, X86XmmRegister src);  // store unaligned

    void movsd(X86XmmRegister dst, X86Address src);

    void movsd(X86Address dst, X86XmmRegister src);

    void movsd(X86XmmRegister dst, X86XmmRegister src);

    void movhpd(X86XmmRegister dst, X86Address src);

    void movhpd(X86Address dst, X86XmmRegister src);

    void addsd(X86XmmRegister dst, X86XmmRegister src);

    void addsd(X86XmmRegister dst, X86Address src);

    void subsd(X86XmmRegister dst, X86XmmRegister src);

    void subsd(X86XmmRegister dst, X86Address src);

    void mulsd(X86XmmRegister dst, X86XmmRegister src);

    void mulsd(X86XmmRegister dst, X86Address src);

    void divsd(X86XmmRegister dst, X86XmmRegister src);

    void divsd(X86XmmRegister dst, X86Address src);

    void addpd(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void subpd(X86XmmRegister dst, X86XmmRegister src);

    void mulpd(X86XmmRegister dst, X86XmmRegister src);

    void divpd(X86XmmRegister dst, X86XmmRegister src);

    void movdqa(X86XmmRegister dst, X86XmmRegister src);     // move

    void movdqa(X86XmmRegister dst, X86Address src);  // load aligned

    void movdqu(X86XmmRegister dst, X86Address src);  // load unaligned

    void movdqa(X86Address dst, X86XmmRegister src);  // store aligned

    void movdqu(X86Address dst, X86XmmRegister src);  // store unaligned

    void vmovdqa(X86XmmRegister dst, X86XmmRegister src);     // move

    void vmovdqa(X86XmmRegister dst, X86Address src);  // load aligned

    void vmovdqu(X86XmmRegister dst, X86Address src);  // load unaligned

    void vmovdqa(X86Address dst, X86XmmRegister src);  // store aligned

    void vmovdqu(X86Address dst, X86XmmRegister src);  // store unaligned

    void paddb(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void psubb(X86XmmRegister dst, X86XmmRegister src);

    void vpaddb(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vpaddw(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void paddw(X86XmmRegister dst, X86XmmRegister src);

    void psubw(X86XmmRegister dst, X86XmmRegister src);

    void pmullw(X86XmmRegister dst, X86XmmRegister src);

    void vpmullw(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vpsubb(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vpsubw(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vpsubd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void paddd(X86XmmRegister dst, X86XmmRegister src);

    void psubd(X86XmmRegister dst, X86XmmRegister src);

    void pmulld(X86XmmRegister dst, X86XmmRegister src);

    void vpmulld(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vpaddd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void paddq(X86XmmRegister dst, X86XmmRegister src);

    void psubq(X86XmmRegister dst, X86XmmRegister src);

    void vpaddq(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void vpsubq(X86XmmRegister dst, X86XmmRegister add_left, X86XmmRegister add_right);

    void paddusb(X86XmmRegister dst, X86XmmRegister src);

    void paddsb(X86XmmRegister dst, X86XmmRegister src);

    void paddusw(X86XmmRegister dst, X86XmmRegister src);

    void paddsw(X86XmmRegister dst, X86XmmRegister src);

    void psubusb(X86XmmRegister dst, X86XmmRegister src);

    void psubsb(X86XmmRegister dst, X86XmmRegister src);

    void psubusw(X86XmmRegister dst, X86XmmRegister src);

    void psubsw(X86XmmRegister dst, X86XmmRegister src);

    void cvtsi2ss(X86XmmRegister dst, X86CpuRegister src);

    void cvtsi2sd(X86XmmRegister dst, X86CpuRegister src);

    void cvtss2si(X86CpuRegister dst, X86XmmRegister src);

    void cvtss2sd(X86XmmRegister dst, X86XmmRegister src);

    void cvtsd2si(X86CpuRegister dst, X86XmmRegister src);

    void cvtsd2ss(X86XmmRegister dst, X86XmmRegister src);

    void cvttss2si(X86CpuRegister dst, X86XmmRegister src);

    void cvttsd2si(X86CpuRegister dst, X86XmmRegister src);

    void cvtdq2ps(X86XmmRegister dst, X86XmmRegister src);

    void cvtdq2pd(X86XmmRegister dst, X86XmmRegister src);

    void comiss(X86XmmRegister a, X86XmmRegister b);

    void comiss(X86XmmRegister a, X86Address b);

    void comisd(X86XmmRegister a, X86XmmRegister b);

    void comisd(X86XmmRegister a, X86Address b);

    void ucomiss(X86XmmRegister a, X86XmmRegister b);

    void ucomiss(X86XmmRegister a, X86Address b);

    void ucomisd(X86XmmRegister a, X86XmmRegister b);

    void ucomisd(X86XmmRegister a, X86Address b);

    void roundsd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm);

    void roundss(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm);

    void sqrtsd(X86XmmRegister dst, X86XmmRegister src);

    void sqrtss(X86XmmRegister dst, X86XmmRegister src);

    void xorpd(X86XmmRegister dst, X86Address src);

    void xorpd(X86XmmRegister dst, X86XmmRegister src);

    void xorps(X86XmmRegister dst, X86Address src);

    void xorps(X86XmmRegister dst, X86XmmRegister src);

    void pxor(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void vpxor(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vxorps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vxorpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void andpd(X86XmmRegister dst, X86XmmRegister src);

    void andpd(X86XmmRegister dst, X86Address src);

    void andps(X86XmmRegister dst, X86XmmRegister src);

    void andps(X86XmmRegister dst, X86Address src);

    void pand(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void vpand(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vandps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vandpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void andn(X86CpuRegister dst, X86CpuRegister src1, X86CpuRegister src2);  // no addr variant (for now)

    void andnpd(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void andnps(X86XmmRegister dst, X86XmmRegister src);

    void pandn(X86XmmRegister dst, X86XmmRegister src);

    void vpandn(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vandnps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vandnpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void orpd(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void orps(X86XmmRegister dst, X86XmmRegister src);

    void por(X86XmmRegister dst, X86XmmRegister src);

    void vpor(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vorps(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void vorpd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void pavgb(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void pavgw(X86XmmRegister dst, X86XmmRegister src);

    void psadbw(X86XmmRegister dst, X86XmmRegister src);

    void pmaddwd(X86XmmRegister dst, X86XmmRegister src);

    void vpmaddwd(X86XmmRegister dst, X86XmmRegister src1, X86XmmRegister src2);

    void phaddw(X86XmmRegister dst, X86XmmRegister src);

    void phaddd(X86XmmRegister dst, X86XmmRegister src);

    void haddps(X86XmmRegister dst, X86XmmRegister src);

    void haddpd(X86XmmRegister dst, X86XmmRegister src);

    void phsubw(X86XmmRegister dst, X86XmmRegister src);

    void phsubd(X86XmmRegister dst, X86XmmRegister src);

    void hsubps(X86XmmRegister dst, X86XmmRegister src);

    void hsubpd(X86XmmRegister dst, X86XmmRegister src);

    void pminsb(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void pmaxsb(X86XmmRegister dst, X86XmmRegister src);

    void pminsw(X86XmmRegister dst, X86XmmRegister src);

    void pmaxsw(X86XmmRegister dst, X86XmmRegister src);

    void pminsd(X86XmmRegister dst, X86XmmRegister src);

    void pmaxsd(X86XmmRegister dst, X86XmmRegister src);

    void pminub(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void pmaxub(X86XmmRegister dst, X86XmmRegister src);

    void pminuw(X86XmmRegister dst, X86XmmRegister src);

    void pmaxuw(X86XmmRegister dst, X86XmmRegister src);

    void pminud(X86XmmRegister dst, X86XmmRegister src);

    void pmaxud(X86XmmRegister dst, X86XmmRegister src);

    void minps(X86XmmRegister dst, X86XmmRegister src);  // no addr variant (for now)

    void maxps(X86XmmRegister dst, X86XmmRegister src);

    void minpd(X86XmmRegister dst, X86XmmRegister src);

    void maxpd(X86XmmRegister dst, X86XmmRegister src);

    void pcmpeqb(X86XmmRegister dst, X86XmmRegister src);

    void pcmpeqw(X86XmmRegister dst, X86XmmRegister src);

    void pcmpeqd(X86XmmRegister dst, X86XmmRegister src);

    void pcmpeqq(X86XmmRegister dst, X86XmmRegister src);

    void pcmpgtb(X86XmmRegister dst, X86XmmRegister src);

    void pcmpgtw(X86XmmRegister dst, X86XmmRegister src);

    void pcmpgtd(X86XmmRegister dst, X86XmmRegister src);

    void pcmpgtq(X86XmmRegister dst, X86XmmRegister src);  // SSE4.2

    void shufpd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm);

    void shufps(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm);

    void pshufd(X86XmmRegister dst, X86XmmRegister src, X86Immediate imm);

    void punpcklbw(X86XmmRegister dst, X86XmmRegister src);

    void punpcklwd(X86XmmRegister dst, X86XmmRegister src);

    void punpckldq(X86XmmRegister dst, X86XmmRegister src);

    void punpcklqdq(X86XmmRegister dst, X86XmmRegister src);

    void punpckhbw(X86XmmRegister dst, X86XmmRegister src);

    void punpckhwd(X86XmmRegister dst, X86XmmRegister src);

    void punpckhdq(X86XmmRegister dst, X86XmmRegister src);

    void punpckhqdq(X86XmmRegister dst, X86XmmRegister src);

    void psllw(X86XmmRegister reg, X86Immediate shift_count);

    void pslld(X86XmmRegister reg, X86Immediate shift_count);

    void psllq(X86XmmRegister reg, X86Immediate shift_count);

    void psraw(X86XmmRegister reg, X86Immediate shift_count);

    void psrad(X86XmmRegister reg, X86Immediate shift_count);
    // no psraq

    void psrlw(X86XmmRegister reg, X86Immediate shift_count);

    void psrld(X86XmmRegister reg, X86Immediate shift_count);

    void psrlq(X86XmmRegister reg, X86Immediate shift_count);

    void psrldq(X86XmmRegister reg, X86Immediate shift_count);

    void flds(X86Address src);

    void fstps(X86Address dst);

    void fsts(X86Address dst);

    void fldl(X86Address src);

    void fstpl(X86Address dst);

    void fstl(X86Address dst);

    void fstsw();

    void fucompp();

    void fnstcw(X86Address dst);

    void fldcw(X86Address src);

    void fistpl(X86Address dst);

    void fistps(X86Address dst);

    void fildl(X86Address src);

    void filds(X86Address src);

    void fincstp();

    void ffree(X86Immediate index);

    void fsin();

    void fcos();

    void fptan();

    void fprem();

    void xchgb(X86ByteRegister dst, X86ByteRegister src);

    void xchgb(X86ByteRegister reg, X86Address address);

    void xchgw(X86CpuRegister dst, X86CpuRegister src);

    void xchgw(X86CpuRegister reg, X86Address address);

    void xchgl(X86CpuRegister dst, X86CpuRegister src);

    void xchgl(X86CpuRegister reg, X86Address address);

    void cmpb(X86Address address, X86Immediate imm);

    void cmpw(X86Address address, X86Immediate imm);

    void cmpl(X86CpuRegister reg, X86Immediate imm);

    void cmpl(X86CpuRegister reg0, X86CpuRegister reg1);

    void cmpl(X86CpuRegister reg, X86Address address);

    void cmpl(X86Address address, X86CpuRegister reg);

    void cmpl(X86Address address, X86Immediate imm);

    void testl(X86CpuRegister reg1, X86CpuRegister reg2);

    void testl(X86CpuRegister reg, X86Immediate imm);

    void testl(X86CpuRegister reg1, X86Address address);

    void testb(X86Address dst, X86Immediate imm);

    void testl(X86Address dst, X86Immediate imm);

    void andl(X86CpuRegister dst, X86Immediate imm);

    void andl(X86CpuRegister dst, X86CpuRegister src);

    void andl(X86CpuRegister dst, X86Address address);

    void andw(X86Address address, X86Immediate imm);

    void orl(X86CpuRegister dst, X86Immediate imm);

    void orl(X86CpuRegister dst, X86CpuRegister src);

    void orl(X86CpuRegister dst, X86Address address);

    void xorl(X86CpuRegister dst, X86CpuRegister src);

    void xorl(X86CpuRegister dst, X86Immediate imm);

    void xorl(X86CpuRegister dst, X86Address address);

    void addl(X86CpuRegister dst, X86CpuRegister src);

    void addl(X86CpuRegister reg, X86Immediate imm);

    void addl(X86CpuRegister reg, X86Address address);

    void addl(X86Address address, X86CpuRegister reg);

    void addl(X86Address address, X86Immediate imm);

    void addw(X86Address address, X86Immediate imm);

    void addw(X86CpuRegister reg, X86Immediate imm);

    void adcl(X86CpuRegister dst, X86CpuRegister src);

    void adcl(X86CpuRegister reg, X86Immediate imm);

    void adcl(X86CpuRegister dst, X86Address address);

    void subl(X86CpuRegister dst, X86CpuRegister src);

    void subl(X86CpuRegister reg, X86Immediate imm);

    void subl(X86CpuRegister reg, X86Address address);

    void subl(X86Address address, X86CpuRegister src);

    void cdq();

    void idivl(X86CpuRegister reg);

    void divl(X86CpuRegister reg);

    void imull(X86CpuRegister dst, X86CpuRegister src);

    void imull(X86CpuRegister reg, X86Immediate imm);

    void imull(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm);

    void imull(X86CpuRegister reg, X86Address address);

    void imull(X86CpuRegister reg);

    void imull(X86Address address);

    void mull(X86CpuRegister reg);

    void mull(X86Address address);

    void sbbl(X86CpuRegister dst, X86CpuRegister src);

    void sbbl(X86CpuRegister reg, X86Immediate imm);

    void sbbl(X86CpuRegister reg, X86Address address);

    void sbbl(X86Address address, X86CpuRegister src);

    void incl(X86CpuRegister reg);

    void incl(X86Address address);

    void decl(X86CpuRegister reg);

    void decl(X86Address address);

    void shll(X86CpuRegister reg, X86Immediate imm);

    void shll(X86CpuRegister operand, X86CpuRegister shifter);

    void shll(X86Address address, X86Immediate imm);

    void shll(X86Address address, X86CpuRegister shifter);

    void shrl(X86CpuRegister reg, X86Immediate imm);

    void shrl(X86CpuRegister operand, X86CpuRegister shifter);

    void shrl(X86Address address, X86Immediate imm);

    void shrl(X86Address address, X86CpuRegister shifter);

    void sarl(X86CpuRegister reg, X86Immediate imm);

    void sarl(X86CpuRegister operand, X86CpuRegister shifter);

    void sarl(X86Address address, X86Immediate imm);

    void sarl(X86Address address, X86CpuRegister shifter);

    void shld(X86CpuRegister dst, X86CpuRegister src, X86CpuRegister shifter);

    void shld(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm);

    void shrd(X86CpuRegister dst, X86CpuRegister src, X86CpuRegister shifter);

    void shrd(X86CpuRegister dst, X86CpuRegister src, X86Immediate imm);

    void negl(X86CpuRegister reg);

    void notl(X86CpuRegister reg);

    void enter(X86Immediate imm);

    void leave();

    void ret();

    void ret(X86Immediate imm);

    void nop();

    void int3();

    void hlt();

    void j(X86Condition condition, Label label);

    void j(X86Condition condition, X86NearLabel label);

    void jecxz(X86NearLabel label);

    void jmp(X86CpuRegister reg);

    void jmp(X86Address address);

    void jmp(Label label);

    void jmp(X86NearLabel label);

    void repne_scasb();

    void repne_scasw();

    void repe_cmpsb();

    void repe_cmpsw();

    void repe_cmpsl();

    void rep_movsb();

    void rep_movsl();

    void rep_movsw();

    X86AssemblerI lock();

    void cmpxchgb(X86Address address, X86ByteRegister reg);

    void cmpxchgw(X86Address address, X86CpuRegister reg);

    void cmpxchgl(X86Address address, X86CpuRegister reg);

    void cmpxchg8b(X86Address address);

    void xaddb(X86Address address, X86ByteRegister reg);

    void xaddw(X86Address address, X86CpuRegister reg);

    void xaddl(X86Address address, X86CpuRegister reg);

    void mfence();

    X86AssemblerI fs();

    X86AssemblerI gs();
}
