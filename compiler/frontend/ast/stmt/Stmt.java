package frontend.ast.stmt;

import frontend.ast.ASTNode;

//语句父类
public class Stmt extends ASTNode {
    protected Stmt(int lineNum){
        super(lineNum);
    }
}
