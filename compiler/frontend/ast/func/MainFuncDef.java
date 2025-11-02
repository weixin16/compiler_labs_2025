package frontend.ast.func;

import frontend.ast.ASTNode;
import frontend.ast.block.Block;

/*
    主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    存在main函数
 */
public class MainFuncDef extends ASTNode {
    private Block body;

    public MainFuncDef(int lineNum){
        super(lineNum);
    }

    public Block getBody() {
        return body;
    }

    public void setBody(Block body) {
        this.body = body;
    }
}
