package frontend.ast.stmt;

import frontend.ast.exp.Exp;
import frontend.ast.exp.LVal;

/*
    Stmt → LVal '=' Exp ';'
    每种类型的语句都要覆盖
 */
public class AssignStmt extends Stmt{
    private LVal lVal;
    private Exp exp;

    public AssignStmt(int lineNum, LVal lVal, Exp exp) {
        super(lineNum);
        this.lVal = lVal;
        this.exp = exp;
    }

    public LVal getLVal() {
        return lVal;
    }

    public void setLVal(LVal lVal) {
        this.lVal = lVal;
    }

    public Exp getExp() {
        return exp;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }
}
