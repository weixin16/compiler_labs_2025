package backend.mips;

import backend.mips.data.GlobalVar;
import backend.mips.data.StringConst;
import backend.mips.instr.MipsInstr;
import midend.ir.Quad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MipsGenerator {
    private final List<Quad> ir;
    private final Map<String, GlobalVar> globalVars = new LinkedHashMap<>();
    private final Map<String, StringConst> stringConsts = new LinkedHashMap<>();
    private final Map<String,String> strLabelMap = new LinkedHashMap<>();
    private int strCount =0;

    public MipsGenerator(List<Quad> ir) {
        this.ir = ir;
    }

    public List<String> generate(){
        List<String> mipsCode = new ArrayList<>();
        collectGlobalAndStr();

        mipsCode.add(".data");
        for (GlobalVar globalVar:globalVars.values()){
            mipsCode.add(globalVar.toString());
        }
        for (StringConst strConst: stringConsts.values()){
            mipsCode.add(strConst.toString());
        }

        mipsCode.add(".text");
        mipsCode.add("  move $fp, $sp");
        mipsCode.add("  jal main");
        mipsCode.add("  li $v0, 10");
        mipsCode.add("  syscall");

        int i=0, n=ir.size();
        while (i<n){
            Quad quad = ir.get(i);
            if (quad.op.equals("func")){
                String funcName = quad.arg1;
                int start = i+1;
                int j=start;
                while (j<n && !ir.get(j).op.equals("endfunc")) j++;
                int end =j;

                List<Quad> funcBody = ir.subList(start,end);
                MipsFuncTranslator mipsFuncTranslator = new MipsFuncTranslator(funcName,funcBody,strLabelMap);
                List < MipsInstr> instrs = mipsFuncTranslator.translate();
                for (MipsInstr instr : instrs){
                    mipsCode.add(instr.toString());
                }

                i =j+1;
            } else {
                i++;
            }
        }

        return mipsCode;
    }

    private void collectGlobalAndStr() {
        for(Quad quad:ir){
            if (quad.op.equals("func")) break;
            switch (quad.op){
                case "gdecl" -> globalDecl(quad);
                case "ginit" -> globalInit(quad);
                case "ginitarr" -> globalInitArr(quad);
            }
        }

        for(Quad quad:ir){
            if(quad.op.equals("print_str") && quad.arg1!=null){
                if(!stringConsts.containsKey(quad.arg1)){
                    String label = ".str" +(strCount++);
                    stringConsts.put(quad.arg1,new StringConst(label, quad.arg1));
                    strLabelMap.put(quad.arg1, label);
                }
            }

        }
    }

    public static boolean isImm(String str){
        try{
            Integer.parseInt(str);
            return true;
        } catch (Exception e){
            return false;
        }
    }


    //ir: "gdecl " + arg1 + ", " arg2
    // gdecl name, size
    private void globalDecl(Quad quad){
        String name = quad.arg1;
        String size = quad.arg2;

        GlobalVar globalVar = globalVars.computeIfAbsent(name,GlobalVar::new);
        int s =1;
        if (size!=null && isImm(size)){
            s = Math.max(1,Integer.parseInt(size));
        }
        globalVar.ensureSize(s);
    }

    // ir："ginit " +arg1+ ", " + res;
    // ginit value, name
    private void globalInit(Quad quad){
        String dst = quad.res;
        String value = quad.arg1;

        GlobalVar globalVar = globalVars.computeIfAbsent(dst,GlobalVar::new);
        globalVar.ensureSize(1);
        if (isImm(value)){
            globalVar.setInitAt(0,Integer.parseInt(value));
        }
    }

    // ir："ginitarr"-> op +" " + arg1 + ", " + arg2 + ", "+res ;
    // ginitarr value, index, name
    private void globalInitArr(Quad quad){
        String arr = quad.res;
        String value = quad.arg1;
        String index = quad.arg2;
        if (!isImm(index)) return;

        int idx = Integer.parseInt(index);
        GlobalVar globalVar = globalVars.computeIfAbsent(arr,GlobalVar::new);

        globalVar.ensureSize(idx+1);
        if (isImm(value)){
            int val = Integer.parseInt(value);
            globalVar.setInitAt(idx,val);
        }
    }


}
