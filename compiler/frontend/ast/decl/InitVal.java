package frontend.ast.decl;

import frontend.ast.ASTNode;
import frontend.ast.exp.Exp;

import java.util.ArrayList;
import java.util.List;

/*
    变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    1. 表达式初值
    2.一维数组初值
 */
public class InitVal extends ASTNode {
    private  final List<Exp> exps = new ArrayList<>();

    public InitVal(int lineNum){
        super(lineNum);
    }

    public List<Exp> getExps() {
        return exps;
    }

    public void addExp(Exp exp){
        exps.add(exp);
    }
}
