package frontend.ast.exp;

import frontend.ast.ASTNode;

/*
    条件表达式 Cond → LOrExp  //存在即可
 */
public class Cond extends ASTNode {
    private LOrExp lOrExp;

    public Cond(int lineNum){
        super(lineNum);
    }

    public LOrExp getlOrExp() {
        return lOrExp;
    }

    public void setlOrExp(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
    }
}
