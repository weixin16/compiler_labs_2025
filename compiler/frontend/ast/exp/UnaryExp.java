package frontend.ast.exp;


import frontend.token.Token;
import frontend.ast.func.FuncRParams;

/*
    一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    3种情况均需覆盖,函数调用也需要覆盖FuncRParams的不同情况
 */
public class UnaryExp extends Exp {
    private PrimaryExp primaryExp;
    private Token ident;
    private FuncRParams funcRParams;
    private Token unaryOp;
    private UnaryExp unaryExp;

    public UnaryExp(int lineNum){
        super(lineNum);
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public void setPrimaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
    }

    public void setCall(Token ident, FuncRParams funcFParams) {
        this.ident = ident;
        this.funcRParams = funcFParams;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public Token getIdent() {
        return ident;
    }

    public void setUnary(Token unaryOp, UnaryExp unaryExp) {
        this.unaryOp = unaryOp;
        this.unaryExp = unaryExp;
    }

    public Token getUnaryOp() {
        return unaryOp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }
}
