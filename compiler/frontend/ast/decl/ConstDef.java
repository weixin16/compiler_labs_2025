package frontend.ast.decl;

import frontend.token.Token;
import frontend.ast.exp.ConstExp;

/*
    常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    包含普通变量、一维数组两种情况
 */
public class ConstDef extends Decl {
    private Token ident;
    private ConstExp array;
    private ConstInitVal constInitVal;

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

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    public void setConstInitVal(ConstInitVal constInitVal) {
        this.constInitVal = constInitVal;
    }

    public ConstDef(int lineNum){
        super(lineNum);
    }

    public boolean isArray(){
        return array!=null;
    }
}
