package frontend.ast.func;

import frontend.token.Token;
import frontend.ast.ASTNode;
import frontend.ast.decl.BType;

/*
    函数形参 FuncFParam → BType Ident ['[' ']']
    1.普通变量 2.一维数组变量
 */
public class FuncFParam extends ASTNode {
    private BType btype;
    private Token ident;
    private boolean isArray = false;

    public FuncFParam(int lineNum) {
        super(lineNum);
    }

    public BType getBtype() {
        return btype;
    }

    public void setBtype(BType btype) {
        this.btype = btype;
    }

    public Token getIdent() {
        return ident;
    }

    public void setIdent(Token ident) {
        this.ident = ident;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }
}
