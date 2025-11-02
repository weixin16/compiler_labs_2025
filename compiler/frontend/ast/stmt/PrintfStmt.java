package frontend.ast.stmt;

import frontend.token.Token;
import frontend.ast.exp.Exp;

import java.util.ArrayList;
import java.util.List;

/*
    语句 Stmt → 'printf''('StringConst {','Exp}')'';'
    1.有Exp 2.无Exp
 */
public class PrintfStmt extends Stmt{
    private Token string;
    private final List<Exp> exps = new ArrayList<>();

    public PrintfStmt(int lineNum, Token string) {
        super(lineNum);
        this.string = string;
    }

    public Token getString() {
        return string;
    }

    public void setString(Token string) {
        this.string = string;
    }

    public List<Exp> getExps() {
        return exps;
    }

    public void addExp(Exp exp) {
        exps.add(exp);
    }
}
