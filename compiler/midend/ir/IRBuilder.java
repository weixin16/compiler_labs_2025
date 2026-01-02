package midend.ir;

import java.util.ArrayList;
import java.util.List;

public class IRBuilder {
    private final List<Quad> quads = new ArrayList<>();
    private final List<Quad> globals = new ArrayList<>();
    private int tempN=0;
    private int labelN=0;

    public String newTemp() {
        return "t"+(++tempN);
    }

    public String newLabel(String name){
        String label="";
        if (name!=null){
            label=name.replaceAll("[^A=Za-z0-9]","_");
        }
        if (!label.isEmpty()){
            label="_"+label;
        }
        return "L"+(++labelN)+label;
    }

    public Quad lastQuad(){
        if (quads.isEmpty()) return null;
        return quads.get(quads.size()-1);
    }

    public void emit(String op, String arg1, String arg2, String res) {
        quads.add(new Quad(op, arg1, arg2, res));
    }

    public void emitGlobal(String op, String arg1, String arg2, String res) {
        globals.add(new Quad(op, arg1, arg2, res));
    }


    public List<Quad> getIr(){
        List<Quad> ir = new ArrayList<>(globals.size()+quads.size());
        ir.addAll(globals);
        ir.addAll(quads);
        return ir;
    }
}
