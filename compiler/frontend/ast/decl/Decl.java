package frontend.ast.decl;

import frontend.ast.ASTNode;

/*
    声明 Decl → ConstDecl | VarDecl
    覆盖两种声明
 */
public class Decl extends ASTNode {
    protected Decl(int lineNum){
        super(lineNum);
    }
}
