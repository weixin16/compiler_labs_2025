package frontend.ast.stmt;

import frontend.ast.block.Block;

/*
    Stmt → Block
 */
public class BlockStmt extends Stmt{
    private Block block;

    public BlockStmt(int lineNum, Block block) {
        super(lineNum);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }
}
