package frontend.ast.func;

import frontend.ast.ASTNode;
import frontend.ast.exp.Exp;

import java.util.ArrayList;
import java.util.List;

/*
    函数实参表达式 FuncRParams → Exp { ',' Exp }
    1.花括号内重复0次
    2.花括号内重复多次
    3.Exp需要覆盖数组传参和部分数组传参
 */
public class FuncRParams extends ASTNode {
    private final List<Exp> exps = new ArrayList<>();

    public FuncRParams(int lineNum) {
        super(lineNum);
    }

    public List<Exp> getExps() {
        return exps;
    }

    public void addExp(Exp exp){
        exps.add(exp);
    }
}
