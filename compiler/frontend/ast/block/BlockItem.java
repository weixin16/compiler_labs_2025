package frontend.ast.block;

import frontend.ast.ASTNode;
import frontend.ast.decl.Decl;
import frontend.ast.stmt.Stmt;

/*
    语句块项 BlockItem → Decl | Stmt
    覆盖两种语句块项
 */
public class BlockItem extends ASTNode {
    private Decl decl;
    private Stmt stmt;

    public BlockItem(int lineNum){
        super(lineNum);
    }

    public BlockItem(Decl decl) {
        super(decl.getLineNum());
        this.decl = decl;
    }

    public BlockItem(Stmt stmt) {
        super(stmt.getLineNum());
        this.stmt = stmt;
    }

    public Decl getDecl() {
        return decl;
    }

    public void setDecl(Decl decl) {
        this.decl = decl;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    public  boolean isDecl(){
        return decl != null;
    }
}
