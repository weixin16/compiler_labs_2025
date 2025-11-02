package frontend.ast.exp;

import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

/*
    关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    1.AddExp
    2.<
    3.>
    4.<=
    5.>=
    均需覆盖
 */
public class RelExp extends Exp {
    private final List<Exp> AddExps = new ArrayList<>();
    private final List<Token> operators = new ArrayList<>();

    public RelExp(int lineNum){
        super(lineNum);
    }

    public List<Exp> getAddExps() {
        return AddExps;
    }

    public List<Token> getOperators() {
        return operators;
    }

    public void addAddExp(Exp exp){
        AddExps.add(exp);
    }

    public void addOperator(Token op){
        operators.add(op);
    }
}
