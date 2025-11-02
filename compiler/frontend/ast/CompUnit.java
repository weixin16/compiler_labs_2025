package frontend.ast;

import frontend.ast.decl.Decl;
import frontend.ast.func.FuncDef;
import frontend.ast.func.MainFuncDef;

import java.util.ArrayList;
import java.util.List;

/*
    编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    1.是否存在Decl
    2.是否存在FuncDef
 */
public class CompUnit extends ASTNode{
    private final List<Decl> decls = new ArrayList<>();
    private final List<FuncDef> funcDefs = new ArrayList<>();
    private MainFuncDef mainFuncDef;

    public CompUnit(int lineNum){
        super(lineNum);
    }

    public List<Decl> getDecls() {
        return decls;
    }

    public List<FuncDef> getFuncDefs() {
        return funcDefs;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }

    public void setMainFuncDef(MainFuncDef mainFuncDef) {
        this.mainFuncDef = mainFuncDef;
    }

    public void addDecl(Decl decl){
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef){
        funcDefs.add(funcDef);
    }
}
