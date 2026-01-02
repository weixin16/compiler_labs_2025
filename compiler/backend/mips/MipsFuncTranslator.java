package backend.mips;

import backend.mips.instr.*;
import backend.register.RegAllocator;
import backend.register.Register;
import midend.ir.Quad;

import java.util.*;

public class MipsFuncTranslator {
    private final String funcName;
    private final List<Quad> quads;
    private final FrameLayout frame;
    private final List<MipsInstr> mipsCode = new ArrayList<>();
    private final Map<String, String> strLabelMap;
    private final RegAllocator regAllocator;

    public MipsFuncTranslator(String funcName, List<Quad> quads, Map<String, String> strLabelMap) {
        this.funcName = funcName;
        this.quads = quads;
        this.frame = FrameLayout.fromQuads(funcName,quads);
        this.strLabelMap = strLabelMap;
        this.regAllocator = new RegAllocator(frame,mipsCode);
    }

    public List<MipsInstr> translate() {
        emitFuncStart();
        for(Quad quad: quads){
            translateQuad(quad);
        }
        emitFuncEnd();
        return mipsCode;
    }

    private void emitFuncStart(){
        int frameSize = frame.getSize();
        mipsCode.add(new LabelInstr(funcName));
        if (frameSize>0){
            mipsCode.add(new ITypeInstr("addiu",Register.SP,Register.SP,-frameSize));
        }
        mipsCode.add(new LoadStoreInstr("sw", Register.RA,frameSize-4,Register.SP));
        mipsCode.add(new LoadStoreInstr("sw", Register.FP, frameSize-8, Register.SP));
        mipsCode.add(new MoveInstr(Register.FP,Register.SP)); // fp=sp

        List<String> params = frame.getParams();
        for (int i=0; i<params.size(); i++){
            String paramVar = params.get(i);
            Register dst = regAllocator.getRegForWrite(paramVar);

            Register src = null;
            if (i<4) {
                src = switch (i){
                    case 0 -> Register.A0;
                    case 1 -> Register.A1;
                    case 2 -> Register.A2;
                    default -> Register.A3;
                };
                mipsCode.add(new MoveInstr(dst,src));
            } else {
                int offset = frameSize + (i-4)*4;
                mipsCode.add(new LoadStoreInstr("lw", dst, offset, Register.FP));
            }
        }
    }

    private void emitFuncEnd(){
        int frameSize = frame.getSize();
        String retLabel = funcName+"_ret";
        mipsCode.add(new LabelInstr(retLabel));
        mipsCode.add(new MoveInstr(Register.SP, Register.FP));
        mipsCode.add(new LoadStoreInstr("lw", Register.FP,frameSize-8, Register.SP));
        mipsCode.add(new LoadStoreInstr("lw",Register.RA,frameSize-4,Register.SP));
        if (frameSize>0){
            mipsCode.add(new ITypeInstr("addiu",Register.SP,Register.SP,frameSize));
        }
        mipsCode.add(new JrInstr(Register.RA));

    }

    private static boolean isStaticName(String name){
        return name!=null && name.matches("s\\d+_.*");
    }

    private static boolean isGlobalName(String name){
        return name!=null && (name.startsWith("g_") || isStaticName(name));
    }

    private Register buildElemAddr(String arr, String idxOperand){
        Register index = regAllocator.getRegForRead(idxOperand);
        mipsCode.add(new SllInstr(Register.T8,index,2));

        if(isGlobalName(arr)){
            mipsCode.add(new LaInstr(Register.T9,arr));
        } else if(arr.startsWith("p")){
            Register param = regAllocator.getRegForRead(arr);
            mipsCode.add(new MoveInstr(Register.T9,param));
        } else {
            int offset = frame.getOffset(arr);
            mipsCode.add(new ITypeInstr("addiu",Register.T9,Register.FP,offset));
        }
        mipsCode.add(new RTypeInstr("addu",Register.T9,Register.T9,Register.T8));
        return Register.T9;
    }

    private void translateQuad(Quad quad){
        switch (quad.op) {
            case "add", "sub", "mul", "lt", "le", "gt", "ge", "eq", "ne"-> translateArithType(quad);
            case "div", "mod" -> translateDivMod(quad);
            case "neg" -> translateNeg(quad);
            case "not" -> translateNot(quad);
            case "move" ->translateMove(quad);
            case "load" -> translateLoad(quad);
            case "loadarr" -> translateLoadArr(quad);
            case "storearr" -> translateStoreArr(quad);
            case "label" -> translateLabel(quad);
            case "j" -> translateJ(quad);
            case "bez" -> translateBez(quad);
            case "param_val" -> translateParamVal(quad);
            case "param_addr" -> translateParamAddr(quad);
            case "decl", "fparam", "fparam_arr" -> { }
            case "call" -> translateCall(quad);
            case "ret" -> translateReturn(quad);
            case "print_int" -> translatePrintInt(quad);
            case "print_str" -> translatePrintStr(quad);
            case "getint" -> translateGetInt(quad);
            default -> { }
        }
    }

    private void translateArithType(Quad quad){
        Register rs = regAllocator.getRegForRead(quad.arg1);
        Register rt = regAllocator.getRegForReadAvoid(quad.arg2,rs);
        Register rd = regAllocator.getRegForWrite(quad.res);

        switch (quad.op){
            case "add" -> mipsCode.add(new RTypeInstr("addu",rd,rs,rt));
            case "sub" -> mipsCode.add(new RTypeInstr("subu",rd,rs,rt));
            case "mul" -> mipsCode.add(new RTypeInstr("mul",rd,rs,rt));
            case "lt" -> mipsCode.add(new RTypeInstr("slt",rd,rs,rt));
            case "gt" -> mipsCode.add(new RTypeInstr("sgt",rd,rs,rt));
            case "le" -> mipsCode.add(new RTypeInstr("sle",rd,rs,rt));
            case "ge" -> mipsCode.add(new RTypeInstr("sge",rd,rs,rt));
            case "eq" -> mipsCode.add(new RTypeInstr("seq",rd,rs,rt));
            case "ne" -> mipsCode.add(new RTypeInstr("sne",rd,rs,rt));
        }
    }

    private void translateDivMod(Quad quad){
        Register rs = regAllocator.getRegForRead(quad.arg1);
        Register rt = regAllocator.getRegForReadAvoid(quad.arg2,rs);
        Register rd = regAllocator.getRegForWrite(quad.res);

        mipsCode.add(new DivModInstr("div",rs,rt));
        switch (quad.op) {
            case "div" -> mipsCode.add(new DivModInstr("mflo",rd));
            case "mod" -> mipsCode.add(new DivModInstr("mfhi",rd));
        }
    }

    private void translateNeg(Quad quad){
        Register src = regAllocator.getRegForRead(quad.arg1);
        Register dst = regAllocator.getRegForWrite(quad.res);
        mipsCode.add(new RTypeInstr("subu",dst,Register.ZERO,src));
    }

    private void translateNot(Quad quad){
        Register src = regAllocator.getRegForRead(quad.arg1);
        Register dst = regAllocator.getRegForWrite(quad.res);
        mipsCode.add(new RTypeInstr("seq",dst,src,Register.ZERO));
    }

    private void translateMove(Quad quad){
        Register src = regAllocator.getRegForRead(quad.arg1);
        Register dst = regAllocator.getRegForWrite(quad.res);
        mipsCode.add(new MoveInstr(dst,src));
    }

    private void translateLoad(Quad quad){
        Register src = regAllocator.getRegForRead(quad.arg1);
        Register dst = regAllocator.getRegForWrite(quad.res);
        mipsCode.add(new MoveInstr(dst,src));
    }

    // "loadarr" -> res + " = " + arg1 + "[" + arg2 + "]"
    private void translateLoadArr(Quad quad){
        String arr = quad.arg1;
        Register addr = buildElemAddr(arr, quad.arg2);
        Register rd = regAllocator.getRegForWrite(quad.res);
        mipsCode.add(new LoadStoreInstr("lw",rd,0,addr));
    }

    // "storearr" -> res + "[" + arg2 +"]"+ " = " + arg1 ;
    private void translateStoreArr(Quad quad){
        String arr=quad.res;
        Register addr = buildElemAddr(arr, quad.arg2);
        Register value = regAllocator.getRegForRead(quad.arg1);
        mipsCode.add(new LoadStoreInstr("sw",value,0,addr));
    }

    private void translateLabel(Quad quad){
        regAllocator.flushAll();
        mipsCode.add(new LabelInstr(quad.res));
    }

    private void translateJ(Quad quad){
        regAllocator.flushAll();
        mipsCode.add(new JumpInstr("j",quad.res));
    }

    private void translateBez(Quad quad){
        Register reg = regAllocator.getRegForRead(quad.arg1);
        regAllocator.flushAll();
        mipsCode.add(new BeqzInstr(reg,quad.res));
    }

    private Register getAReg(int idx){
        return switch (idx){
            case 0 ->Register.A0;
            case 1 ->Register.A1;
            case 2 ->Register.A2;
            default ->Register.A3;
        };
    }

    private static class ArgInfo{
        final boolean isAddr;
        final String operand;

        public ArgInfo(boolean isAddr, String operand) {
            this.isAddr = isAddr;
            this.operand = operand;
        }
    }

    private final List<ArgInfo> pendingArgs = new ArrayList<>();


    private void translateParamVal(Quad quad){
        pendingArgs.add(new ArgInfo(false, quad.arg1));
    }

    private void translateParamAddr(Quad quad){
        pendingArgs.add(new ArgInfo(true, quad.arg1));
    }

    private Register materializeArg(ArgInfo argInfo){
        if (!argInfo.isAddr){
            return regAllocator.getRegForRead(argInfo.operand);
        }
        String arr = argInfo.operand;
        if(isGlobalName(arr)){
            mipsCode.add(new LaInstr(Register.T8,arr));
            return Register.T8;
        } else if(arr.startsWith("p")){
            return regAllocator.getRegForRead(arr);
        }else {
            int offset = frame.getOffset(arr);
            mipsCode.add(new ITypeInstr("addiu",Register.T8, Register.FP,offset));
            return Register.T8;
        }
    }

    private void translateCall(Quad quad){
        String callee = quad.arg1;
        regAllocator.beforeCall();

        int argCount=pendingArgs.size();
        int extra = Math.max(0,argCount-4); //第5个起
        int extraBytes = extra*4;
        if (extraBytes>0){
            mipsCode.add(new ITypeInstr("addiu", Register.SP,Register.SP,-extraBytes));
        }

        for(int i=0; i<argCount; i++){
            Register valReg = materializeArg(pendingArgs.get(i));
            if(i<4){
                Register aReg = getAReg(i);
                mipsCode.add(new MoveInstr(aReg, valReg));
            } else {
                int offset = (i-4)*4;
                mipsCode.add(new LoadStoreInstr("sw", valReg, offset,Register.SP));
            }
        }

        mipsCode.add(new JumpInstr("jal",callee));

        if (extraBytes>0){
            mipsCode.add(new ITypeInstr("addiu", Register.SP,Register.SP,extraBytes));
        }

        regAllocator.afterCall(quad.res);

        pendingArgs.clear();
    }

    private void translatePrintInt(Quad quad){
        Register reg = regAllocator.getRegForRead(quad.arg1);
        mipsCode.add(new MoveInstr(Register.A0,reg));
        mipsCode.add(new LiInstr(Register.V0, 1));
        mipsCode.add(new SyscallInstr());
    }

    private void translatePrintStr(Quad quad){
        String label = strLabelMap.getOrDefault(quad.arg1,quad.arg1);
        mipsCode.add(new LaInstr(Register.A0,label));
        mipsCode.add(new LiInstr(Register.V0, 4));
        mipsCode.add(new SyscallInstr());
    }

    private void translateGetInt(Quad quad){
        regAllocator.beforeCall();
        mipsCode.add(new LiInstr(Register.V0,5));
        mipsCode.add(new SyscallInstr());
        regAllocator.afterCall(quad.res);
    }

    private void translateReturn(Quad quad){
        if(quad.arg1!=null){
            Register src = regAllocator.getRegForRead(quad.arg1);
            mipsCode.add(new MoveInstr(Register.V0, src));
        } else {
            mipsCode.add(new MoveInstr(Register.V0, Register.ZERO));
        }
        regAllocator.flushAll();
        mipsCode.add(new JumpInstr("j",funcName+"_ret"));
    }

}
