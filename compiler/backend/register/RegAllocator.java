package backend.register;

import backend.mips.FrameLayout;
import backend.mips.instr.*;

import java.util.*;

public class RegAllocator {
    private final FrameLayout frame;
    private final List<MipsInstr> mipsCode;
    private final List<Register> regs = new ArrayList<>();
    private final Map<String,Register> varToReg=new HashMap<>();
    private final Map<Register,String> regToVar=new HashMap<>();
    private final Set<String> dirtyVars = new HashSet<>();

    public RegAllocator(FrameLayout frame, List<MipsInstr> mipsCode) {
        this.frame = frame;
        this.mipsCode = mipsCode;
        initRegs();
    }

    private void initRegs(){
        for (Register reg : Register.values()){
            if (reg.allocatable()){
                regs.add(reg);
            }
        }
    }

    private void bind(Register reg, String var){
        regToVar.put(reg,var);
        varToReg.put(var,reg);
    }
    private static boolean isStaticName(String name){
        return name!=null && name.matches("s\\d+_.*");
    }

    private static boolean isGlobalName(String name){
        return name!=null && (name.startsWith("g_") || isStaticName(name));
    }

    private void markDirty(String var){
        if (var==null) return;
        if(var.startsWith("#")) return;
        dirtyVars.add(var);
    }

    private void markClean(String var){
        if (var==null) return;
        dirtyVars.remove(var);
    }

    private void spill(Register reg){
        String var = regToVar.get(reg);
        if (var==null) return;
        if (var.startsWith("#"))return;
        if (!dirtyVars.contains(var)) return;

        if (isGlobalName(var)){
            mipsCode.add(new SimpleInstr("  sw "+reg+", "+var));
        } else {
            int offset = frame.getOffset(var);
            mipsCode.add(new LoadStoreInstr("sw",reg,offset,Register.FP));
        }
        markClean(var);
    }

    private Register allocReg(String var, Register avoid){
        for (Register reg:regs){
            if(avoid!= null && reg==avoid) continue;
            if (!regToVar.containsKey(reg)){
                bind(reg,var);
                return reg;
            }
        }
        //没有空闲
        Register victim =null;
        for(Register reg: regs){
            if(avoid!= null && reg==avoid) continue;
            victim=reg;
            break;
        }

        if (victim==null) {
            victim = regs.get(0);
        }
        spill(victim);
        String oldVar = regToVar.get(victim);
        if (oldVar!=null){
            varToReg.remove(oldVar);
        }
        bind(victim,var);
        return victim;
    }

    private Register allocReg(String var){
        return allocReg(var, null );
    }

    public Register getRegForReadAvoid(String operand, Register avoid){
        if (operand == null) return Register.ZERO;

        if (isImm(operand)){
            String immKey = "#imm" + operand;
            Register reg = varToReg.get(immKey);
            if (reg == null){
                reg = allocReg(immKey, avoid);
            }
            mipsCode.add(new LiInstr(reg, Integer.parseInt(operand)));
            return reg;
        }

        Register reg = varToReg.get(operand);
        if (reg != null) return reg;

        reg = allocReg(operand, avoid);

        if (isGlobalName(operand)){
            mipsCode.add(new SimpleInstr("  lw " + reg + ", " + operand));
        } else {
            int offset = frame.getOffset(operand);
            mipsCode.add(new LoadStoreInstr("lw", reg, offset, Register.FP));
        }
        markClean(operand);
        return reg;
    }


    public void flushAll(){
        List<Register> regToVarKey=new ArrayList<>(regToVar.keySet());
        for(Register reg:regToVarKey){
            spill(reg);
        }
        varToReg.clear();
        regToVar.clear();
        dirtyVars.clear();
    }

    private boolean isImm(String str){
        if (str==null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public Register getRegForRead(String operand){
        if (operand==null) return Register.ZERO;

        if (isImm(operand)){
            String imm = "#imm" + operand;
            Register reg = varToReg.get(imm);
            if (reg==null) {
                reg = allocReg(imm);
            }
            mipsCode.add(new LiInstr(reg,Integer.parseInt(operand)));
            return reg;
        }
        Register reg = varToReg.get(operand);
        if (reg!=null) return reg;

        reg=allocReg(operand);
        if (operand.startsWith("g_") || operand.startsWith("s")){
            mipsCode.add(new SimpleInstr("  lw "+reg.toString()+", "+operand));
        } else {
            int offset = frame.getOffset(operand);
            mipsCode.add(new LoadStoreInstr("lw",reg,offset,Register.FP));
        }
        markClean(operand);
        return reg;
    }

    public Register getRegForWrite(String var){
        if (var==null) return null;

        Register reg = varToReg.get(var);
        markDirty(var);
        if (reg!=null) {
            return reg;
        }

        return allocReg(var);
    }

    public void beforeCall(){
        flushAll();
    }

    public void afterCall(String retVar){
        if (retVar==null) return;
        if (isGlobalName(retVar)){
            mipsCode.add(new SimpleInstr("  sw " + Register.V0+", "+retVar));
        } else {
            int offset = frame.getOffset(retVar);
            mipsCode.add(new LoadStoreInstr("sw",Register.V0,offset,Register.FP));
        }
    }
}
