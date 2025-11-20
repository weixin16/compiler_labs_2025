package frontend.ast.decl;

import frontend.token.Token;
import frontend.ast.exp.ConstExp;

/*
    变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
    包含普通常量、一维数组定义
 */
public class VarDef extends Decl {
    private Token ident;
    private ConstExp array;
    private InitVal initVal;

    public VarDef(int lineNum){
        super(lineNum);
    }

    public Token getIdent() {
        return ident;
    }

    public void setIdent(Token ident) {
        this.ident = ident;
    }

    public ConstExp getArray() {
        return array;
    }

    public void setArray(ConstExp array) {
        this.array = array;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    public boolean isArray(){
        return array!=null;
    }
}
