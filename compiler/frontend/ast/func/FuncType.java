package frontend.ast.func;

import frontend.ast.ASTNode;

import java.util.Objects;

public class FuncType extends ASTNode {
    private final String funcType;

    public FuncType(int lineNum, String funcType) {
        super(lineNum);
        this.funcType = funcType;
    }

    public String getFuncType() {
        return funcType;
    }

    public boolean isInt(){
        return Objects.equals(funcType, "int");
    }
}
