package frontend.ast.exp;

import frontend.token.Token;

/*
    数值 Number → IntConst // 存在即可
 */
public class Number extends Exp {
    private final Token num;

    public Number(Token token){
        super(token.getLineNum());
        this.num =token;
    }

    public Token getNum() {
        return num;
    }

    public int getValue(){
        return Integer.parseInt(num.getLexeme());
    }
}
