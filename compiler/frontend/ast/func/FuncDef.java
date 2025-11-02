package frontend.ast.func;

import frontend.token.Token;
import frontend.ast.ASTNode;
import frontend.ast.block.Block;

/*
    函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    1.无形参
    2.有形参
 */
public class FuncDef extends ASTNode {
    private FuncType funcType;
    private Token ident;
    private FuncFParams params;
    private Block body;

    public FuncDef(int lineNum){
        super(lineNum);
    }

    public FuncFParams getParams() {
        return params;
    }

    public void setParams(FuncFParams params) {
        this.params = params;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public Token getIdent() {
        return ident;
    }

    public void setIdent(Token ident) {
        this.ident = ident;
    }

    public Block getBody() {
        return body;
    }

    public void setBody(Block body) {
        this.body = body;
    }
}
