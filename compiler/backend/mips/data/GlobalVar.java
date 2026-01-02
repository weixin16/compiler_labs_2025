package backend.mips.data;

import java.util.ArrayList;
import java.util.List;

public class GlobalVar {
    private final String name;
    private final List<Integer> initValues = new ArrayList<>();

    public GlobalVar(String name) {
        this.name = name;
    }

    public void ensureSize(int size){
        while (initValues.size()<size){
            initValues.add(0);
        }
    }

    public void setInitAt(int index, int value){
        ensureSize(index+1);
        initValues.set(index,value);
    }

    @Override
    public String toString() {
        if (initValues.isEmpty()){
            return name + ": .word 0";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": .word ");
        for(int i=0; i<initValues.size();i++){
            if(i>0) sb.append(", ");
            sb.append(initValues.get(i));
        }
        return sb.toString();
    }
}
