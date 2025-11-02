package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    1.MulExp  2.+ 需覆盖  3.- 需覆盖
 */
public class AddExp extends Exp {
    private final List<Exp> mulExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public AddExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getMulExps() {
        return mulExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addMulExp(Exp exp){
        mulExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }
}
