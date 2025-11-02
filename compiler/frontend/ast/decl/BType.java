package frontend.ast.decl;

import frontend.ast.ASTNode;

/*
    基本类型 BType → 'int'
 */
public class BType extends ASTNode {
    private final String bType = "int";

    public BType(int lineNum){
        super(lineNum);
    }

    public String getBType() {
        return bType;
    }
}
