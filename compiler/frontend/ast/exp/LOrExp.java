package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    1.LAndExp
    2.|| 均需覆盖
 */
public class LOrExp extends Exp{
    private final List<Exp> lAndExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public LOrExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getLAndExps() {
        return lAndExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addLAndExp(Exp exp){
        lAndExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }

}
