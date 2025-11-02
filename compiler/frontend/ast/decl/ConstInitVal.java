package frontend.ast.decl;

import frontend.ast.ASTNode;
import frontend.ast.exp.ConstExp;

import java.util.ArrayList;
import java.util.List;

/*
    常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    1.常表达式初值
    2.一维数组初值
 */
public class ConstInitVal extends ASTNode {
    private  final List<ConstExp> constExps = new ArrayList<>();

    public ConstInitVal(int lineNum){
        super(lineNum);
    }

    public List<ConstExp> getConstExps() {
        return constExps;
    }

    public void addConstExp(ConstExp constExp){
        constExps.add(constExp);
    }
}
