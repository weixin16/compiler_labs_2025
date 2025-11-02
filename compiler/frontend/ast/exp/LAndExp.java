package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
    1.EqExp 2.&&  均需覆盖
 */
public class LAndExp extends Exp{
    private final List<Exp> eqExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public LAndExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getEqExps() {
        return eqExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addEqExp(Exp exp){
        eqExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }
}
