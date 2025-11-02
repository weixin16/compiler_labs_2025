package frontend.ast.stmt;

import frontend.ast.exp.Cond;

/*
    Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    1.有else 2.无else
 */
public class IfStmt extends Stmt {
    private Cond cond;
    private Stmt thenStmt;
    private Stmt elseStmt;

    public IfStmt(int lineNum) {
        super(lineNum);
    }

    public Cond getCond() {
        return cond;
    }

    public void setCond(Cond cond) {
        this.cond = cond;
    }

    public Stmt getThenStmt() {
        return thenStmt;
    }

    public void setThenStmt(Stmt thenStmt) {
        this.thenStmt = thenStmt;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }

    public void setElseStmt(Stmt elseStmt) {
        this.elseStmt = elseStmt;
    }
}
