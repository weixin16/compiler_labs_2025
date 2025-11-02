package frontend.ast.decl;

import java.util.ArrayList;
import java.util.List;

/*
    常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    1.花括号内重复0次
    2.花括号内重复多次
 */
public class ConstDecl extends Decl {
    private BType bType;
    private final List<ConstDef> constDefs = new ArrayList<>();

    public ConstDecl(int lineNum){
        super(lineNum);
    }

    public BType getBType() {
        return bType;
    }

    public void setBType(BType bType) {
        this.bType = bType;
    }

    public List<ConstDef> getConstDefs() {
        return constDefs;
    }

    public void addConstDef (ConstDef constDef){
        constDefs.add(constDef);
    }
}
