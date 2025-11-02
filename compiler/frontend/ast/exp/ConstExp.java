package frontend.ast.exp;

/*
    常量表达式 ConstExp → AddExp
    注：使用的 Ident 必须是常量 // 存在即可
 */
public class ConstExp extends Exp {
    private AddExp addExp;

    public ConstExp(int lineNum){
        super(lineNum);
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public void setAddExp(AddExp addExp) {
        this.addExp = addExp;
    }
}
