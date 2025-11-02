package frontend.ast.block;

import frontend.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

/*
    语句块 Block → '{' { BlockItem } '}'
    1.花括号内重复0次
    2.花括号内重复多次
 */
public class Block extends ASTNode {
    private final List<BlockItem> blockItems = new ArrayList<>();

    public Block(int lineNum){
        super(lineNum);
    }

    public List<BlockItem> getBlockItems() {
        return blockItems;
    }

    public void addBlockItem(BlockItem blockItem){
        blockItems.add(blockItem);
    }
}
