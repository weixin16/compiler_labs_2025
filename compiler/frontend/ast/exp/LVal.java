package frontend.ast.exp;

import frontend.token.Token;

/*
    左值表达式 LVal → Ident ['[' Exp ']']
    1.普通变量、常量
    2.一维数组
 */
public class LVal extends Exp {
    private Token ident;
    private Exp index;

    public LVal(int lineNum){
        super(lineNum);
    }

    public Token getIdent() {
        return ident;
    }

    public void setIdent(Token ident) {
        this.ident = ident;
    }

    public Exp getIndex() {
        return index;
    }

    public void setIndex(Exp index) {
        this.index = index;
    }
}
