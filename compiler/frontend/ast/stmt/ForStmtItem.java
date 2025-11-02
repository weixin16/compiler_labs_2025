package frontend.ast.stmt;

import frontend.ast.ASTNode;
import frontend.ast.exp.Exp;
import frontend.ast.exp.LVal;

public class ForStmtItem extends ASTNode {
    private LVal lVal;
    private Exp exp;

    public ForStmtItem(int lineNum, LVal lVal, Exp exp) {
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
