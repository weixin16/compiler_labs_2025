package frontend;

public class Token {
    private final String lexeme;
    private final TokenType tokenType;
    private final int lineNum;

    public Token(String lexeme, TokenType tokenType, int lineNum) {
        this.lexeme = lexeme;
        this.tokenType = tokenType;
        this.lineNum = lineNum;
    }

    public String getLexeme() {
        return lexeme;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public int getLineNum() {
        return lineNum;
    }
}
