package com.v7878.jnasm.x86_64;

import com.v7878.jnasm.Label;
import com.v7878.jnasm.common_x86.X86Condition;
import com.v7878.jnasm.common_x86.X86NearLabel;

/*
  Emit Machine Instructions.
 */
// TODO: javadoc
public interface X86_64AssemblerI {
    void call(X86_64CpuRegister reg);

    void call(X86_64Address address);

    void call(Label label);

    void pushq(X86_64CpuRegister reg);

    void pushq(X86_64Address address);

    void pushq(X86_64Immediate imm);

    void popq(X86_64CpuRegister reg);

    void popq(X86_64Address address);

    void movq(X86_64CpuRegister dst, X86_64Immediate src);

    void movl(X86_64CpuRegister dst, X86_64Immediate src);

    void movq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movntl(X86_64Address dst, X86_64CpuRegister src);

    void movntq(X86_64Address dst, X86_64CpuRegister src);

    void movq(X86_64CpuRegister dst, X86_64Address src);

    void movl(X86_64CpuRegister dst, X86_64Address src);

    void movq(X86_64Address dst, X86_64CpuRegister src);

    void movq(X86_64Address dst, X86_64Immediate imm);

    void movl(X86_64Address dst, X86_64CpuRegister src);

    void movl(X86_64Address dst, X86_64Immediate imm);

    void cmov(X86Condition c, X86_64CpuRegister dst, X86_64CpuRegister src);  // This is the 64b version.

    void cmov(X86Condition c, X86_64CpuRegister dst, X86_64CpuRegister src, boolean is64bit);

    void cmov(X86Condition c, X86_64CpuRegister dst, X86_64Address src, boolean is64bit);

    void movzxb(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movzxb(X86_64CpuRegister dst, X86_64Address src);

    void movsxb(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movsxb(X86_64CpuRegister dst, X86_64Address src);

    void movb(X86_64CpuRegister dst, X86_64Address src);

    void movb(X86_64Address dst, X86_64CpuRegister src);

    void movb(X86_64Address dst, X86_64Immediate imm);

    void movzxw(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movzxw(X86_64CpuRegister dst, X86_64Address src);

    void movsxw(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movsxw(X86_64CpuRegister dst, X86_64Address src);

    void movw(X86_64CpuRegister dst, X86_64Address src);

    void movw(X86_64Address dst, X86_64CpuRegister src);

    void movw(X86_64Address dst, X86_64Immediate imm);

    void leaq(X86_64CpuRegister dst, X86_64Address src);

    void leal(X86_64CpuRegister dst, X86_64Address src);

    void movaps(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void movaps(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void movups(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void movaps(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void movups(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void vmovaps(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void vmovaps(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void vmovaps(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void vmovups(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void vmovups(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void movss(X86_64XmmRegister dst, X86_64Address src);

    void movss(X86_64Address dst, X86_64XmmRegister src);

    void movss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void movsxd(X86_64CpuRegister dst, X86_64CpuRegister src);

    void movsxd(X86_64CpuRegister dst, X86_64Address src);

    void movq(X86_64XmmRegister dst, X86_64CpuRegister src);

    void movq(X86_64CpuRegister dst, X86_64XmmRegister src);

    void movd(X86_64XmmRegister dst, X86_64CpuRegister src);

    void movd(X86_64CpuRegister dst, X86_64XmmRegister src);

    void addss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void addss(X86_64XmmRegister dst, X86_64Address src);

    void subss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void subss(X86_64XmmRegister dst, X86_64Address src);

    void mulss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void mulss(X86_64XmmRegister dst, X86_64Address src);

    void divss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void divss(X86_64XmmRegister dst, X86_64Address src);

    void addps(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void subps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void mulps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void divps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vmulps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vmulpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vdivps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vdivpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vaddps(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vsubps(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vsubpd(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vaddpd(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vfmadd213ss(X86_64XmmRegister accumulator, X86_64XmmRegister left, X86_64XmmRegister right);

    void vfmadd213sd(X86_64XmmRegister accumulator, X86_64XmmRegister left, X86_64XmmRegister right);

    void movapd(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void movapd(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void movupd(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void movapd(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void movupd(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void vmovapd(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void vmovapd(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void vmovapd(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void vmovupd(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void vmovupd(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void movsd(X86_64XmmRegister dst, X86_64Address src);

    void movsd(X86_64Address dst, X86_64XmmRegister src);

    void movsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void addsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void addsd(X86_64XmmRegister dst, X86_64Address src);

    void subsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void subsd(X86_64XmmRegister dst, X86_64Address src);

    void mulsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void mulsd(X86_64XmmRegister dst, X86_64Address src);

    void divsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void divsd(X86_64XmmRegister dst, X86_64Address src);

    void addpd(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void subpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void mulpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void divpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void movdqa(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void movdqa(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void movdqu(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void movdqa(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void movdqu(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void vmovdqa(X86_64XmmRegister dst, X86_64XmmRegister src);     // move

    void vmovdqa(X86_64XmmRegister dst, X86_64Address src);  // load aligned

    void vmovdqa(X86_64Address dst, X86_64XmmRegister src);  // store aligned

    void vmovdqu(X86_64XmmRegister dst, X86_64Address src);  // load unaligned

    void vmovdqu(X86_64Address dst, X86_64XmmRegister src);  // store unaligned

    void paddb(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void psubb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpaddb(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vpaddw(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void paddw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmullw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpmullw(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vpsubb(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vpsubw(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vpsubd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void paddd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmulld(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpmulld(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vpaddd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void paddq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpaddq(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void vpsubq(X86_64XmmRegister dst, X86_64XmmRegister add_left, X86_64XmmRegister add_right);

    void paddusb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void paddsb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void paddusw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void paddsw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubusb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubsb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubusw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psubsw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void cvtsi2ss(X86_64XmmRegister dst, X86_64CpuRegister src);  // Note: this is the r/m32 version.

    void cvtsi2ss(X86_64XmmRegister dst, X86_64CpuRegister src, boolean is64bit);

    void cvtsi2ss(X86_64XmmRegister dst, X86_64Address src, boolean is64bit);

    void cvtsi2sd(X86_64XmmRegister dst, X86_64CpuRegister src);  // Note: this is the r/m32 version.

    void cvtsi2sd(X86_64XmmRegister dst, X86_64CpuRegister src, boolean is64bit);

    void cvtsi2sd(X86_64XmmRegister dst, X86_64Address src, boolean is64bit);

    void cvtss2si(X86_64CpuRegister dst, X86_64XmmRegister src);  // Note: this is the r32 version.

    void cvtss2sd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void cvtss2sd(X86_64XmmRegister dst, X86_64Address src);

    void cvtsd2si(X86_64CpuRegister dst, X86_64XmmRegister src);  // Note: this is the r32 version.

    void cvtsd2ss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void cvtsd2ss(X86_64XmmRegister dst, X86_64Address src);

    void cvttss2si(X86_64CpuRegister dst, X86_64XmmRegister src);  // Note: this is the r32 version.

    void cvttss2si(X86_64CpuRegister dst, X86_64XmmRegister src, boolean is64bit);

    void cvttsd2si(X86_64CpuRegister dst, X86_64XmmRegister src);  // Note: this is the r32 version.

    void cvttsd2si(X86_64CpuRegister dst, X86_64XmmRegister src, boolean is64bit);

    void cvtdq2ps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void cvtdq2pd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void comiss(X86_64XmmRegister a, X86_64XmmRegister b);

    void comiss(X86_64XmmRegister a, X86_64Address b);

    void comisd(X86_64XmmRegister a, X86_64XmmRegister b);

    void comisd(X86_64XmmRegister a, X86_64Address b);

    void ucomiss(X86_64XmmRegister a, X86_64XmmRegister b);

    void ucomiss(X86_64XmmRegister a, X86_64Address b);

    void ucomisd(X86_64XmmRegister a, X86_64XmmRegister b);

    void ucomisd(X86_64XmmRegister a, X86_64Address b);

    void roundsd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm);

    void roundss(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm);

    void sqrtsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void sqrtss(X86_64XmmRegister dst, X86_64XmmRegister src);

    void xorpd(X86_64XmmRegister dst, X86_64Address src);

    void xorpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void xorps(X86_64XmmRegister dst, X86_64Address src);

    void xorps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pxor(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void vpxor(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vxorps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vxorpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void andpd(X86_64XmmRegister dst, X86_64Address src);

    void andpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void andps(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void pand(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpand(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vandps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vandpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void andn(X86_64CpuRegister dst, X86_64CpuRegister src1, X86_64CpuRegister src2);

    void andnpd(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void andnps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pandn(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpandn(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vandnps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vandnpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void orpd(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void orps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void por(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpor(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vorps(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void vorpd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void pavgb(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void pavgw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psadbw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmaddwd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void vpmaddwd(X86_64XmmRegister dst, X86_64XmmRegister src1, X86_64XmmRegister src2);

    void phaddw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void phaddd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void haddps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void haddpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void phsubw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void phsubd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void hsubps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void hsubpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminsb(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void pmaxsb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminsw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmaxsw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmaxsd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminub(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void pmaxub(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminuw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmaxuw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pminud(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pmaxud(X86_64XmmRegister dst, X86_64XmmRegister src);

    void minps(X86_64XmmRegister dst, X86_64XmmRegister src);  // no addr variant (for now)

    void maxps(X86_64XmmRegister dst, X86_64XmmRegister src);

    void minpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void maxpd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpeqb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpeqw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpeqd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpeqq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpgtb(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpgtw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpgtd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void pcmpgtq(X86_64XmmRegister dst, X86_64XmmRegister src);  // SSE4.2

    void shufpd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm);

    void shufps(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm);

    void pshufd(X86_64XmmRegister dst, X86_64XmmRegister src, X86_64Immediate imm);

    void punpcklbw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpcklwd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpckldq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpcklqdq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpckhbw(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpckhwd(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpckhdq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void punpckhqdq(X86_64XmmRegister dst, X86_64XmmRegister src);

    void psllw(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void pslld(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psllq(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psraw(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psrad(X86_64XmmRegister reg, X86_64Immediate shift_count);
    // no psraq

    void psrlw(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psrld(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psrlq(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void psrldq(X86_64XmmRegister reg, X86_64Immediate shift_count);

    void flds(X86_64Address src);

    void fstps(X86_64Address dst);

    void fsts(X86_64Address dst);

    void fldl(X86_64Address src);

    void fstpl(X86_64Address dst);

    void fstl(X86_64Address dst);

    void fstsw();

    void fucompp();

    void fnstcw(X86_64Address dst);

    void fldcw(X86_64Address src);

    void fistpl(X86_64Address dst);

    void fistps(X86_64Address dst);

    void fildl(X86_64Address src);

    void filds(X86_64Address src);

    void fincstp();

    void ffree(X86_64Immediate index);

    void fsin();

    void fcos();

    void fptan();

    void fprem();

    void xchgb(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xchgb(X86_64CpuRegister reg, X86_64Address address);

    void xchgw(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xchgw(X86_64CpuRegister reg, X86_64Address address);

    void xchgl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xchgl(X86_64CpuRegister reg, X86_64Address address);

    void xchgq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xchgq(X86_64CpuRegister reg, X86_64Address address);

    void xaddb(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xaddb(X86_64Address address, X86_64CpuRegister reg);

    void xaddw(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xaddw(X86_64Address address, X86_64CpuRegister reg);

    void xaddl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xaddl(X86_64Address address, X86_64CpuRegister reg);

    void xaddq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xaddq(X86_64Address address, X86_64CpuRegister reg);

    void cmpb(X86_64Address address, X86_64Immediate imm);

    void cmpw(X86_64Address address, X86_64Immediate imm);

    void cmpl(X86_64CpuRegister reg, X86_64Immediate imm);

    void cmpl(X86_64CpuRegister reg0, X86_64CpuRegister reg1);

    void cmpl(X86_64CpuRegister reg, X86_64Address address);

    void cmpl(X86_64Address address, X86_64CpuRegister reg);

    void cmpl(X86_64Address address, X86_64Immediate imm);

    void cmpq(X86_64CpuRegister reg0, X86_64CpuRegister reg1);

    void cmpq(X86_64CpuRegister reg0, X86_64Immediate imm);

    void cmpq(X86_64CpuRegister reg0, X86_64Address address);

    void cmpq(X86_64Address address, X86_64Immediate imm);

    void testl(X86_64CpuRegister reg1, X86_64CpuRegister reg2);

    void testl(X86_64CpuRegister reg, X86_64Address address);

    void testl(X86_64CpuRegister reg, X86_64Immediate imm);

    void testq(X86_64CpuRegister reg1, X86_64CpuRegister reg2);

    void testq(X86_64CpuRegister reg, X86_64Address address);

    void testb(X86_64Address address, X86_64Immediate imm);

    void testl(X86_64Address address, X86_64Immediate imm);

    void andl(X86_64CpuRegister dst, X86_64Immediate imm);

    void andl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void andl(X86_64CpuRegister reg, X86_64Address address);

    void andq(X86_64CpuRegister dst, X86_64Immediate imm);

    void andq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void andq(X86_64CpuRegister reg, X86_64Address address);

    void andw(X86_64Address address, X86_64Immediate imm);

    void orl(X86_64CpuRegister dst, X86_64Immediate imm);

    void orl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void orl(X86_64CpuRegister reg, X86_64Address address);

    void orq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void orq(X86_64CpuRegister dst, X86_64Immediate imm);

    void orq(X86_64CpuRegister reg, X86_64Address address);

    void xorl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xorl(X86_64CpuRegister dst, X86_64Immediate imm);

    void xorl(X86_64CpuRegister reg, X86_64Address address);

    void xorq(X86_64CpuRegister dst, X86_64Immediate imm);

    void xorq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void xorq(X86_64CpuRegister reg, X86_64Address address);

    void addl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void addl(X86_64CpuRegister reg, X86_64Immediate imm);

    void addl(X86_64CpuRegister reg, X86_64Address address);

    void addl(X86_64Address address, X86_64CpuRegister reg);

    void addl(X86_64Address address, X86_64Immediate imm);

    void addw(X86_64CpuRegister reg, X86_64Immediate imm);

    void addw(X86_64Address address, X86_64Immediate imm);

    void addw(X86_64Address address, X86_64CpuRegister reg);

    void addq(X86_64CpuRegister reg, X86_64Immediate imm);

    void addq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void addq(X86_64CpuRegister dst, X86_64Address address);

    void subl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void subl(X86_64CpuRegister reg, X86_64Immediate imm);

    void subl(X86_64CpuRegister reg, X86_64Address address);

    void subq(X86_64CpuRegister reg, X86_64Immediate imm);

    void subq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void subq(X86_64CpuRegister dst, X86_64Address address);

    void cdq();

    void cqo();

    void idivl(X86_64CpuRegister reg);

    void idivq(X86_64CpuRegister reg);

    void divl(X86_64CpuRegister reg);

    void divq(X86_64CpuRegister reg);

    void imull(X86_64CpuRegister dst, X86_64CpuRegister src);

    void imull(X86_64CpuRegister reg, X86_64Immediate imm);

    void imull(X86_64CpuRegister dst, X86_64CpuRegister src, X86_64Immediate imm);

    void imull(X86_64CpuRegister reg, X86_64Address address);

    void imulq(X86_64CpuRegister src);

    void imulq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void imulq(X86_64CpuRegister reg, X86_64Immediate imm);

    void imulq(X86_64CpuRegister reg, X86_64Address address);

    void imulq(X86_64CpuRegister dst, X86_64CpuRegister reg, X86_64Immediate imm);

    void imull(X86_64CpuRegister reg);

    void imull(X86_64Address address);

    void mull(X86_64CpuRegister reg);

    void mull(X86_64Address address);

    void shll(X86_64CpuRegister reg, X86_64Immediate imm);

    void shll(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void shrl(X86_64CpuRegister reg, X86_64Immediate imm);

    void shrl(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void sarl(X86_64CpuRegister reg, X86_64Immediate imm);

    void sarl(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void shlq(X86_64CpuRegister reg, X86_64Immediate imm);

    void shlq(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void shrq(X86_64CpuRegister reg, X86_64Immediate imm);

    void shrq(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void sarq(X86_64CpuRegister reg, X86_64Immediate imm);

    void sarq(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void negl(X86_64CpuRegister reg);

    void negq(X86_64CpuRegister reg);

    void notl(X86_64CpuRegister reg);

    void notq(X86_64CpuRegister reg);

    void enter(X86_64Immediate imm);

    void leave();

    void ret();

    void ret(X86_64Immediate imm);

    void nop();

    void int3();

    void hlt();

    void j(X86Condition condition, Label label);

    void j(X86Condition condition, X86NearLabel label);

    void jrcxz(X86NearLabel label);

    void jmp(X86_64CpuRegister reg);

    void jmp(X86_64Address address);

    void jmp(Label label);

    void jmp(X86NearLabel label);

    X86_64AssemblerI lock();

    void cmpxchgb(X86_64Address address, X86_64CpuRegister reg);

    void cmpxchgw(X86_64Address address, X86_64CpuRegister reg);

    void cmpxchgl(X86_64Address address, X86_64CpuRegister reg);

    void cmpxchgq(X86_64Address address, X86_64CpuRegister reg);

    void mfence();

    X86_64AssemblerI gs();

    void setcc(X86Condition condition, X86_64CpuRegister dst);

    void bswapl(X86_64CpuRegister dst);

    void bswapq(X86_64CpuRegister dst);

    void bsfl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void bsfl(X86_64CpuRegister dst, X86_64Address src);

    void bsfq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void bsfq(X86_64CpuRegister dst, X86_64Address src);

    void blsi(X86_64CpuRegister dst, X86_64CpuRegister src);  // no addr variant (for now)

    void blsmsk(X86_64CpuRegister dst, X86_64CpuRegister src);  // no addr variant (for now)

    void blsr(X86_64CpuRegister dst, X86_64CpuRegister src);  // no addr variant (for now)

    void bsrl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void bsrl(X86_64CpuRegister dst, X86_64Address src);

    void bsrq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void bsrq(X86_64CpuRegister dst, X86_64Address src);

    void popcntl(X86_64CpuRegister dst, X86_64CpuRegister src);

    void popcntl(X86_64CpuRegister dst, X86_64Address src);

    void popcntq(X86_64CpuRegister dst, X86_64CpuRegister src);

    void popcntq(X86_64CpuRegister dst, X86_64Address src);

    void rdtsc();

    void rorl(X86_64CpuRegister reg, X86_64Immediate imm);

    void rorl(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void roll(X86_64CpuRegister reg, X86_64Immediate imm);

    void roll(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void rorq(X86_64CpuRegister reg, X86_64Immediate imm);

    void rorq(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void rolq(X86_64CpuRegister reg, X86_64Immediate imm);

    void rolq(X86_64CpuRegister operand, X86_64CpuRegister shifter);

    void repne_scasb();

    void repne_scasw();

    void repe_cmpsw();

    void repe_cmpsl();

    void repe_cmpsq();

    void rep_movsw();

    void rep_movsb();

    void rep_movsl();

    void ud2();
}
