package frontend.ast.stmt;

import frontend.ast.exp.Exp;

/*
    'return' [Exp] ';'
    1.有Exp 2.无Exp
 */
public class ReturnStmt extends Stmt{
    private Exp exp;

    public ReturnStmt(int lineNum, Exp exp) {
        super(lineNum);
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }
}
