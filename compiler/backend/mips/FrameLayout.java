package backend.mips;

import midend.ir.Quad;

import java.util.*;

public class FrameLayout {
    private final Map<String, Integer> offsetMap;
    private final int size;
    private final boolean isMain;
    private final List<String> params;

    public FrameLayout(Map<String, Integer> offsetMap, int size, boolean isMain, List<String> params) {
        this.offsetMap = offsetMap;
        this.size = size;
        this.isMain = isMain;
        this.params = params;
    }

    public int getOffset(String name){
        Integer offset = offsetMap.get(name);
        if (offset==null){
            throw  new IllegalStateException("No offset for: "+name);
        }
        return offset;
    }

    public int getSize() {
        return size;
    }

    public boolean isMain() {
        return isMain;
    }

    public List<String> getParams() {
        return params;
    }

    public static FrameLayout fromQuads(String funcName, List<Quad> quads){
        boolean isMain = funcName.equals("main");
        LinkedHashMap<String, Integer> varSizeMap = new LinkedHashMap<>();
        LinkedHashSet<String> paramSet = new LinkedHashSet<>();

        for (Quad quad:quads){
            if(quad.op.equals("fparam") || quad.op.equals("fparam_arr")){
                String param = quad.arg1;
                if (param!=null){
                    paramSet.add(param);
                    varSizeMap.putIfAbsent(param,1);
                }
                continue;
            }

            collectOperand(quad.arg1,varSizeMap,paramSet);
            collectOperand(quad.arg2,varSizeMap,paramSet);
            collectOperand(quad.res,varSizeMap,paramSet);

            switch (quad.op){
                case "decl" ->{
                    String name = quad.arg1;
                    String size = quad.arg2;
                    if (name==null) break;
                    if (name.startsWith("v") || name.startsWith("t")){
                        int slots =1;
                        if (size!=null && isImm(size)){
                            slots=Math.max(1,Integer.parseInt(size));
                        }
                        varSizeMap.put(name,slots);
                    }
                }
                case "storearr" ->{
                    String arr = quad.res;
                    String index = quad.arg2;
                    updateArraySize(varSizeMap,arr,index);
                }
                case "loadarr" ->{
                    String arr = quad.arg1;
                    String index = quad.arg2;
                    updateArraySize(varSizeMap,arr,index);
                }
                default -> {}
            }
        }

        Map<String,Integer> offsetMap = new HashMap<>();
        int offset = 0;
        for (Map.Entry<String,Integer> entry: varSizeMap.entrySet()){
            String var = entry.getKey();
            int slots = entry.getValue();
            offsetMap.put(var,offset);
            offset+=slots*4;
        }

        int size = offset + 4 + 4;  //$ra & old $fp
        List<String> paramList = new ArrayList<>(paramSet);

        return new FrameLayout(offsetMap,size,isMain,paramList);
    }

    public static boolean isImm(String str){
        try{
            Integer.parseInt(str);
            return true;
        } catch (Exception e){
            return false;
        }
    }
    private static boolean isStaticName(String name){
        return name!=null && name.matches("s\\d+_.*");
    }

    private static boolean isGlobalName(String name){
        return name!=null && (name.startsWith("g_") || isStaticName(name));
    }


    public static void collectOperand(String operand, Map<String, Integer>varSizeMap, Set<String> paramSet){
        if (operand==null) return;
        if (isImm(operand)) return;
        if (operand.startsWith("L") || isGlobalName(operand)) return;
        if (operand.startsWith("#")) return;

        if (operand.startsWith("p") || operand.startsWith("v")|| operand.startsWith("t")){
            varSizeMap.putIfAbsent(operand,1);
            if (operand.startsWith("p")){
                paramSet.add(operand);
            }
        }
    }

    private static void updateArraySize(Map<String,Integer> varSizeMap, String arr, String index){
        if (arr==null || index==null) return;
        if (arr.startsWith("p") || isGlobalName(arr)) return;
        if(arr.startsWith("#")) return;
        if (!isImm(index)) return;

        int idx = Integer.parseInt(index);
        int need = idx+1;
        int old = varSizeMap.getOrDefault(arr,1);
        if (need>old){
            varSizeMap.put(arr,need);
        }
    }
}
