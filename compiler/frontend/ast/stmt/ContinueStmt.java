package frontend.ast.stmt;

//语句 Stmt → 'continue' ';'
public class ContinueStmt extends Stmt{
    public ContinueStmt(int lineNum) {
        super(lineNum);
    }
}
