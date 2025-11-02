package frontend.ast.exp;

/*
    基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
 */
public class PrimaryExp extends Exp {
    private Exp exp;
    private LVal lVal;
    private Number number;

    public PrimaryExp(int lineNum){
        super(lineNum);
    }

    public Exp getExp() {
        return exp;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }

    public LVal getLVal() {
        return lVal;
    }

    public void setLVal(LVal lVal) {
        this.lVal = lVal;
    }

    public Number getNumber() {
        return number;
    }

    public void setNumber(Number number) {
        this.number = number;
    }
}
