package frontend.ast.stmt;


//语句 Stmt → 'break' ';'
public class BreakStmt extends Stmt{
    public BreakStmt(int lineNum) {
        super(lineNum);
    }
}
