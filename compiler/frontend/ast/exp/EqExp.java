package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    1.RelExp 2.== 3.!= 均需覆盖
 */
public class EqExp extends Exp{
    private final List<Exp> relExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public EqExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getRelExps() {
        return relExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addRelExp(Exp exp){
        relExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }
}
