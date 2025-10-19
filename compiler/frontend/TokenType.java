package frontend;

public enum TokenType {
    IDENFR, INTCON, STRCON,
    CONSTTK, INTTK, STATICTK, 
    BREAKTK, CONTINUETK, 
    MAINTK, VOIDTK, RETURNTK,
    IFTK, ELSETK, FORTK, NOT, AND, OR,
    PLUS, MINU, MULT, DIV, MOD,
    LSS, LEQ, GRE, GEQ, EQL, NEQ, ASSIGN,
    SEMICN, COMMA, 
    LPARENT, RPARENT, LBRACK, RBRACK, LBRACE, RBRACE,
    PRINTFTK;

    @Override
    public String toString() {
        return name();
    }
}
