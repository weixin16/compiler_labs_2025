package frontend.ast.func;

import frontend.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

/*
    函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    1.花括号内重复0次
    2.花括号内重复多次
 */
public class FuncFParams extends ASTNode {
    private final List<FuncFParam> funcFParams = new ArrayList<>();

    public FuncFParams(int lineNum){
        super(lineNum);
    }

    public List<FuncFParam> getFuncFParams() {
        return funcFParams;
    }

    public void addParam(FuncFParam param){
        funcFParams.add(param);
    }
}
