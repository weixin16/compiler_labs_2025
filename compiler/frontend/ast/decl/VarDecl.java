package frontend.ast.decl;

import java.util.ArrayList;
import java.util.List;

/*
    变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
    1.花括号内重复0次
    2.花括号内重复多次
 */
public class VarDecl extends Decl {
    private BType bType;
    private final List<VarDef> varDefs = new ArrayList<>();
    private boolean isStatic = false;

    public VarDecl(int lineNum) {
        super(lineNum);
    }

    public BType getbType() {
        return bType;
    }

    public void setbType(BType bType) {
        this.bType = bType;
    }

    public List<VarDef> getVarDefs() {
        return varDefs;
    }

    public void addVarDef (VarDef varDef){
        varDefs.add(varDef);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }
}
