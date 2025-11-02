package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    1.UnaryExp  2.*  3./  4.%   均需覆盖
 */
public class MulExp extends Exp{
    private final List<Exp> unaryExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public MulExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getUnaryExps() {
        return unaryExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addUnaryExp(Exp exp){
        unaryExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }
}
