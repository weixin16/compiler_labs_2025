package frontend.ast.stmt;

import frontend.ast.exp.Exp;

/*
    Stmt → [Exp] ';' // 有无Exp两种情况；printf函数调用
 */
public class ExpStmt extends Stmt {
    private Exp exp;

    public ExpStmt(int lineNum, Exp exp) {
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
