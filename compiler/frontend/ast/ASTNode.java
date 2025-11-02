package frontend.ast;

public abstract class ASTNode {

    protected int lineNum;


    public ASTNode(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }
}
