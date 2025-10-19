package frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private String source;
    private int curPos=0;
    private String token;
    private TokenType tokenType;
    private final Map<String, TokenType> reserveWords = new HashMap<>();
    private final Map<Character, TokenType> singleCharSymbols = new HashMap<>();
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private int lineNum=1;
    private boolean hasError;
    
    public void analyze(String sourceCode) {
        initLexer(sourceCode);
        lexerAnalyze();
    }

    public void initReserveWords(){
        reserveWords.put("const", TokenType.CONSTTK);
        reserveWords.put("int",TokenType.INTTK);
        reserveWords.put("static", TokenType.STATICTK);
        reserveWords.put("break", TokenType.BREAKTK);
        reserveWords.put("continue", TokenType.CONTINUETK);
        reserveWords.put("if", TokenType.IFTK);
        reserveWords.put("main", TokenType.MAINTK);
        reserveWords.put("else", TokenType.ELSETK);
        reserveWords.put("for",TokenType.FORTK);
        reserveWords.put("return", TokenType.RETURNTK);
        reserveWords.put("void",TokenType.VOIDTK);
        reserveWords.put("printf", TokenType.PRINTFTK);
    }

    public void initSingleCharSymbols(){
        singleCharSymbols.put('!', TokenType.NOT);
        singleCharSymbols.put('+', TokenType.PLUS);
        singleCharSymbols.put('-', TokenType.MINU);
        singleCharSymbols.put('*', TokenType.MULT);
        singleCharSymbols.put('%', TokenType.MOD);
        singleCharSymbols.put('<', TokenType.LSS);
        singleCharSymbols.put('>', TokenType.GRE);
        singleCharSymbols.put('=', TokenType.ASSIGN);
        singleCharSymbols.put(';', TokenType.SEMICN);
        singleCharSymbols.put(',', TokenType.COMMA);
        singleCharSymbols.put('(', TokenType.LPARENT);
        singleCharSymbols.put(')', TokenType.RPARENT);
        singleCharSymbols.put('[', TokenType.LBRACK);
        singleCharSymbols.put(']', TokenType.RBRACK);
        singleCharSymbols.put('{', TokenType.LBRACE);
        singleCharSymbols.put('}', TokenType.RBRACE);
    }

    public void initLexer(String sourceCode) {
        this.source = sourceCode + "\0\0";
        initReserveWords();
        initSingleCharSymbols();
    }

    public void lexerAnalyze(){
        while (curPos < source.length()-2) {
            next();
            if(tokenType!=null && !token.isEmpty()){
                tokens.add(new Token(token, tokenType, lineNum));
            }
        }
    }

    private void next() {
        token = "";
        tokenType = null;
        skipSpace();
        if (curPos >= source.length()-2) return;
        char c = source.charAt(curPos++);
        StringBuilder stringBuilder = new StringBuilder();
        //标识符
        if (Character.isLetter(c) || c == '_') {
            stringBuilder.append(c);
            while (curPos < source.length() && (Character.isLetterOrDigit(source.charAt(curPos)) || source.charAt(curPos) == '_')) {
                stringBuilder.append(source.charAt(curPos++));
            }
            token = stringBuilder.toString();
            tokenType = reserveWords.getOrDefault(token, TokenType.IDENFR);
        }
        //数字
        else if (Character.isDigit(c)) {
            stringBuilder.append(c);
            while (curPos < source.length() && Character.isDigit(source.charAt(curPos))) {
                stringBuilder.append(source.charAt(curPos++));
            }
            token = stringBuilder.toString();
            tokenType = TokenType.INTCON;
        }
        //字符串
        else if (c == '"') {
            stringBuilder.append(c);
            while (curPos < source.length() && source.charAt(curPos) != '"') {
                if(source.charAt(curPos) == '\n'){
                    lineNum++;
                }
                stringBuilder.append(source.charAt(curPos++));
            }
            if (curPos < source.length() && source.charAt(curPos) == '"') {
                stringBuilder.append(source.charAt(curPos++));
            }
            token = stringBuilder.toString();
            tokenType = TokenType.STRCON;
        }
        //比较运算符
        else if (c=='<' || c=='>' || c=='!' || c=='=') {
            stringBuilder.append(c);
            if (curPos < source.length() && source.charAt(curPos)=='=') {
                stringBuilder.append(source.charAt(curPos++));
                token = stringBuilder.toString();
                switch (token) {
                    case "<=":
                        tokenType = TokenType.LEQ;
                        break;
                    case ">=":
                        tokenType = TokenType.GEQ;
                        break;
                    case "!=":
                        tokenType = TokenType.NEQ;
                        break;
                    case "==":
                        tokenType = TokenType.EQL;
                        break;
                }
            } else {
                token = stringBuilder.toString();
                switch (token){
                    case "<": tokenType = TokenType.LSS; break;
                    case ">": tokenType = TokenType.GRE; break;
                    case "!": tokenType = TokenType.NOT; break;
                    case "=": tokenType = TokenType.ASSIGN; break;
                }
            }
        }
        //单字符符号
        else if (singleCharSymbols.containsKey(c)){
            token = String.valueOf(c);
            tokenType = singleCharSymbols.get(c);
        }
        // "&&", "||"
        else if (c == '&') {
            if(curPos<source.length() && source.charAt(curPos)=='&'){
                curPos++;
                token="&&";
                tokenType=TokenType.AND;
            } else {
                addError(lineNum,"a");
                tokenType=null;
            }
        } else if (c == '|') {
            if(curPos<source.length() && source.charAt(curPos)=='|'){
                curPos++;
                token="||";
                tokenType=TokenType.OR;
            } else {
                addError(lineNum,"a");
                tokenType=null;
            }
        }
        // '/'
        else if (c == '/') {
            if (curPos < source.length()) {
                //单行注释
                if (source.charAt(curPos) == '/') {
                    while (curPos < source.length() && source.charAt(curPos) != '\n') {
                        curPos++;
                    }
                    if (curPos < source.length() && source.charAt(curPos) == '\n') {
                        curPos++;
                        lineNum++;
                    }
                }
                //多行注释
                else if (source.charAt(curPos) == '*') {
                    curPos++;
                    while (curPos < source.length()) {
                        char nextC= source.charAt(curPos++);
                        if (curPos<source.length() && nextC== '*' && source.charAt(curPos)== '/') {
                            curPos++;
                            break;
                        }
                        if (nextC == '\n') {
                            lineNum++;
                        }
                    }
                    return;
                }
                //除号
                else {
                    token = "/";
                    tokenType = TokenType.DIV;
                }
            }
        }
        //其余未知字符
        else {
            tokenType=null;
        }

    }

    private void skipSpace(){
        while (curPos<source.length() && Character.isWhitespace(source.charAt(curPos))) {
            if (source.charAt(curPos)=='\n') {
                lineNum++;
            }
            curPos++;
        }
    }

    private void addError(Integer lineNum, String level){
        hasError=true;
        errors.add( lineNum + " " + level);
    }

    public List<Token> getTokens() {
        return tokens;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public boolean hasError() {
        return hasError;
    }
       
}

