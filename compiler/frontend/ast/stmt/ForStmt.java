package frontend.ast.stmt;

import frontend.ast.exp.Cond;

import java.util.ArrayList;
import java.util.List;

/*
    Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    1. 无缺省，1种情况
    2. ForStmt与Cond中缺省一个，3种情况
    3. ForStmt与Cond中缺省两个，3种情况
    4. ForStmt与Cond全部缺省，1种情况
 */
public class ForStmt extends Stmt{
    private final List<ForStmtItem> init = new ArrayList<>();
    private Cond cond;
    private final List<ForStmtItem> update=new ArrayList<>();
    private Stmt body;

    public ForStmt(int lineNum) {
        super(lineNum);
    }

    public List<ForStmtItem> getInit() {
        return init;
    }

    public Cond getCond() {
        return cond;
    }

    public void setCond(Cond cond) {
        this.cond = cond;
    }

    public List<ForStmtItem> getUpdate() {
        return update;
    }

    public Stmt getBody() {
        return body;
    }

    public void setBody(Stmt body) {
        this.body = body;
    }

    public void addInit (ForStmtItem item) {
        init.add(item);
    }

    public void addUpdate(ForStmtItem item) {
        update.add(item);
    }
}
