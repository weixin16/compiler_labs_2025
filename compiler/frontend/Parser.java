package frontend;

import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.token.Token;
import frontend.token.TokenType;
import frontend.ast.CompUnit;
import frontend.ast.decl.*;
import frontend.ast.func.*;
import frontend.ast.stmt.*;
import frontend.ast.exp.*;
import frontend.ast.exp.Number;
import frontend.ast.block.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> outputs = new ArrayList<>();
    private final boolean debug = true;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    // =================== 入口 ===================
    public CompUnit analyze() throws IOException {
        return parseCompUnit();
    }

    // ================== 工具函数 ==================
    private Token peek(Integer offset){
        int index = pos + offset;
        if(index<0) index=0;
        if(index >= tokens.size()){
            index=tokens.size()-1;
        }
        return  tokens.get(index);
    }


    private Token peek(){
        return peek(0);
    }

    private Token next(){
        Token cur = tokens.get(pos);
        if(cur.getTokenType() != TokenType.EOF) pos++;
        return cur;
    }

    private void error(TokenType expected){
        String errorType=null;
        switch (expected){
            case SEMICN -> errorType="i";
            case RPARENT -> errorType="j";
            case RBRACK -> errorType="k";
            default -> {}
        }
        if(errorType!=null){
            frontend.error.Error error = new Error(errorType, errorLine());
            ErrorList.addErrors(error);
        }

        Token t = peek();
        while (t != null && !isSyncPoint(t.getTokenType(), expected)) {
            next();  // 跳过非同步点
            t = peek();
        }
    }

    private void addOutput(Token token){
        if(debug){
            outputs.add(token.getTokenType() + " " + token.getLexeme());
        }
    }

    private boolean isSyncPoint(TokenType type, TokenType expected) {
        if (type == TokenType.RBRACE) return true;
        if (startsStmtOrDecl(type)) return true;

        return switch (expected) {
            case SEMICN -> type == TokenType.SEMICN;
            case RPARENT -> type == TokenType.RPARENT || type == TokenType.SEMICN || type==TokenType.RBRACK;
            case RBRACK -> type == TokenType.RBRACK || type == TokenType.SEMICN ||
                    type == TokenType.COMMA || type == TokenType.ASSIGN  || type==TokenType.RPARENT ;
            default -> false;
        };
    }

    private boolean startsStmtOrDecl(TokenType tt) {
        return tt==TokenType.CONSTTK || tt==TokenType.STATICTK || tt==TokenType.VOIDTK || tt==TokenType.INTTK ||
                tt==TokenType.IFTK   || tt==TokenType.FORTK    || tt==TokenType.BREAKTK || tt==TokenType.CONTINUETK ||
                tt==TokenType.RETURNTK || tt==TokenType.PRINTFTK ||
                tt==TokenType.LBRACE || tt==TokenType.IDENFR ||
                tt==TokenType.LPARENT || tt==TokenType.INTCON ||
                tt==TokenType.PLUS || tt==TokenType.MINU || tt==TokenType.NOT;
    }


    private Token consume(TokenType expected) {
        Token token = peek();
        if (token!=null && token.getTokenType()==expected) {
            next();
            addOutput(token);
            return token;
        }
        return null;
    }

    private boolean match (TokenType... types){
        Token token=peek();
        if (token==null) return false;
        for (TokenType type : types){
            if (token.getTokenType()==type){
                return true;
            }
        }
        return false;
    }

    private void printNT(String name){
        if(!debug) return;
        if(name.equals("<BlockItem>") || name.equals("<Decl>") || name.equals("<BType>")) return;
        outputs.add(name);
    }

    private int lineNum() {
        if (pos < tokens.size()) {
            return tokens.get(pos).getLineNum();
        }
        return tokens.isEmpty() ? 1 : tokens.get(tokens.size()-1).getLineNum();
    }

    private int errorLine(){
        if (pos <= 0) return 1;
        return tokens.get(pos - 1).getLineNum();
    }

    // ================== 编译单元 CompUnit ==================
    /*
        CompUnit → {Decl} {FuncDef} MainFuncDef
        1.是否存在Decl 2.是否存在FuncDef
     */
    private CompUnit parseCompUnit(){
        CompUnit compUnit = new CompUnit(lineNum());
        while (isDeclStart()) {
            compUnit.addDecl(parseDecl());
        }
        while (isFuncDefStart()){
            compUnit.addFuncDef(parseFuncDef());
        }

        compUnit.setMainFuncDef(parseMainFuncDef());
        printNT("<CompUnit>");
        return compUnit;
    }

    private boolean isDeclStart(){
        Token t0 = peek(0);
        Token t1 = peek(1);
        Token t2 = peek(2);
        if(t0==null) return false;
        if(t0.getTokenType()==TokenType.CONSTTK || t0.getTokenType()==TokenType.STATICTK){
            return true;
        }
        if (t0.getTokenType() == TokenType.INTTK) {
            if (t1 == null) return false;
            if (t1.getTokenType() == TokenType.MAINTK) return false; //MainDef

            if (t1.getTokenType() == TokenType.IDENFR) {
                if (t2 == null || t2.getTokenType() != TokenType.LPARENT) {
                    return true;       // VarDecl（含数组/非数组）
                }
                return false;  // FuncDef
            }
        }
        return false;
    }

    private boolean isFuncDefStart() {
        Token t0 = peek(0);
        Token t1 = peek(1);
        Token t2 = peek(2);
        if (t0==null || t1==null || t2==null) return false;
        return (t0.getTokenType() == TokenType.VOIDTK || t0.getTokenType() == TokenType.INTTK) &&
                t1.getTokenType() == TokenType.IDENFR && t2.getTokenType() == TokenType.LPARENT;
    }

    // ==================== 声明 Decl ===================
    /*
        Decl → ConstDecl | VarDecl
        覆盖两种声明
     */
    private Decl parseDecl(){
        if(match(TokenType.CONSTTK)){
            return parseConstDecl();
        } else {
            return parseVarDecl();
        }
    }

    /*
        常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        1.花括号内重复0次
        2.花括号内重复多次
     */
    private ConstDecl parseConstDecl(){
        ConstDecl constDecl = new ConstDecl(lineNum());

        consume(TokenType.CONSTTK);

        BType bType = parseBtype();
        constDecl.setBType(bType);

        constDecl.addConstDef(parseConstDef());

        while (match(TokenType.COMMA)) {
            consume(TokenType.COMMA);
            constDecl.addConstDef(parseConstDef());
        }

        if(!match(TokenType.SEMICN)){
            error(TokenType.SEMICN);
        } else {
            consume(TokenType.SEMICN);
        }

        printNT("<ConstDecl>");
        return constDecl;
    }

    /*
        基本类型 BType → 'int'
     */
    private BType parseBtype(){
        consume(TokenType.INTTK);
        return new BType(lineNum());
    }

    /*
        常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
        包含普通变量、一维数组两种情况
     */
    private ConstDef parseConstDef(){
        ConstDef constDef = new ConstDef(lineNum());
        Token ident = consume(TokenType.IDENFR);
        constDef.setIdent(ident);

        if(match(TokenType.LBRACK)){
            consume(TokenType.LBRACK);
            constDef.setArray(parseConstExp());

            if(!match(TokenType.RBRACK)){
                error(TokenType.RBRACK);
            } else {
                consume(TokenType.RBRACK);
            }

        }

        consume(TokenType.ASSIGN);
        constDef.setConstInitVal(parseConstInitVal());

        printNT("<ConstDef>");
        return constDef;
    }

    /*
        常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
        1.常表达式初值
        2.一维数组初值
     */
    private ConstInitVal parseConstInitVal(){
        ConstInitVal constInitVal = new ConstInitVal(lineNum());
        if(match(TokenType.LBRACE)){
            consume(TokenType.LBRACE);
            if(!match(TokenType.RBRACE)){
                constInitVal.addConstExp(parseConstExp());
                while (match(TokenType.COMMA)){
                    consume(TokenType.COMMA);
                    constInitVal.addConstExp(parseConstExp());
                }
            }
            consume(TokenType.RBRACE);
        } else {
            constInitVal.addConstExp(parseConstExp());
        }
        printNT("<ConstInitVal>");
        return constInitVal;
    }

    /*
        变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
        1.花括号内重复0次
        2.花括号内重复多次
     */
    private VarDecl parseVarDecl(){
        VarDecl varDecl = new VarDecl(lineNum());
        boolean isStatic = false;
        if (match(TokenType.STATICTK)){
            isStatic=true;
            consume(TokenType.STATICTK);
        }
        varDecl.setStatic(isStatic);

        BType bType=parseBtype();
        varDecl.setbType(bType);

        varDecl.addVarDef(parseVarDef());

        while (match(TokenType.COMMA)){
            consume(TokenType.COMMA);
            varDecl.addVarDef(parseVarDef());
        }

        if(!match(TokenType.SEMICN)){
            error(TokenType.SEMICN);
            return varDecl;
        } else {
            consume(TokenType.SEMICN);
        }

        printNT("<VarDecl>");
        return varDecl;
    }

    /*
        变量定义 VarDef → Ident [ '[' ConstExp ']' ]
                       | Ident [ '[' ConstExp ']' ] '=' InitVal
        包含普通常量、一维数组定义
     */
    private VarDef parseVarDef(){
        VarDef varDef = new VarDef(lineNum());
        Token ident = consume(TokenType.IDENFR);
        if (ident == null) {
            printNT("<VarDef>");
            return varDef;
        }
        varDef.setIdent(ident);

        if (match(TokenType.LBRACK)) {
            if (consume(TokenType.LBRACK) == null) {
                printNT("<VarDef>");
                return varDef;
            }
            varDef.setArray(parseConstExp());
            if(!match(TokenType.RBRACK)){
                error(TokenType.RBRACK);
            } else {
                consume(TokenType.RBRACK);
            }
        }

        if (match(TokenType.ASSIGN)) {
            if (consume(TokenType.ASSIGN) == null) {
                printNT("<VarDef>");
                return varDef;
            }
            varDef.setInitVal(parseInitVal());
        }

        printNT("<VarDef>");
        return varDef;
    }

    /* 
        变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' 
        1.表达式初值 
        2.一维数组初值 
    */
    private InitVal parseInitVal(){
        InitVal initVal = new InitVal(lineNum());
        if(match(TokenType.LBRACE)){
            consume(TokenType.LBRACE);
            if(!match(TokenType.RBRACE)){
                initVal.addExp(parseExp());
                while (match(TokenType.COMMA)){
                    consume(TokenType.COMMA);
                    initVal.addExp(parseExp());
                }
            }
            consume(TokenType.RBRACE);
        } else {
            initVal.addExp(parseExp());
        }
        printNT("<InitVal>");
        return initVal;
    }

    // ==================== 函数 Func ===================

    /*
        函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block 
        1.无形参 2.有形参
     */
    private FuncDef parseFuncDef(){
        FuncDef funcDef = new FuncDef(lineNum());

        FuncType funcType = parseFuncType();
        funcDef.setFuncType(funcType);

        Token ident = consume(TokenType.IDENFR);
        funcDef.setIdent(ident);

        consume(TokenType.LPARENT);
        //有形参
        if (!match(TokenType.RPARENT)) {
            funcDef.setParams(parseFuncFParams());
        }

        if(!match(TokenType.RPARENT)){
            error(TokenType.RPARENT);
        } else {
            consume(TokenType.RPARENT);
        }

        funcDef.setBody(parseBlock());

        printNT("<FuncDef>");
        return funcDef;
    }

    /*
        主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block 
        存在main函数
     */
    private MainFuncDef parseMainFuncDef(){
        MainFuncDef mainFuncDef = new MainFuncDef(lineNum());

        consume(TokenType.INTTK);
        consume(TokenType.MAINTK);
        if(consume(TokenType.LPARENT)==null) return mainFuncDef;
        if(!match(TokenType.RPARENT)){
            error(TokenType.RPARENT);
        } else {
            consume(TokenType.RPARENT);
        }

        mainFuncDef.setBody(parseBlock());
        printNT("<MainFuncDef>");
        return mainFuncDef;
    }

    /*
        函数类型 FuncType → 'void' | 'int' 
        覆盖两种类型的函数
     */
    private FuncType parseFuncType(){
        String type;
        if(match(TokenType.VOIDTK)){
            consume(TokenType.VOIDTK);
            type = "void";
        } else {
            consume(TokenType.INTTK);
            type = "int";
        }
        FuncType funcType = new FuncType(lineNum(), type);
        printNT("<FuncType>");
        return funcType;
    }

    /*
        函数形参表 FuncFParams → FuncFParam { ',' FuncFParam } 
        1.花括号内重复0次 
        2.花括号内重复多次
     */
    private FuncFParams parseFuncFParams(){
        FuncFParams funcFParams = new FuncFParams(lineNum());
        funcFParams.addParam(parseFuncFParam());

        while (match(TokenType.COMMA)){
            consume(TokenType.COMMA);
            funcFParams.addParam(parseFuncFParam());
        }

        printNT("<FuncFParams>");
        return funcFParams;
    }

    /*
        函数形参 FuncFParam → BType Ident ['[' ']'] 
        1.普通变量 2.一维数组变量
     */
    private FuncFParam parseFuncFParam(){
        FuncFParam funcFParam = new FuncFParam(lineNum());

        BType bType = parseBtype();
        funcFParam.setBtype(bType);

        Token ident = consume(TokenType.IDENFR);
        funcFParam.setIdent(ident);

        if(match(TokenType.LBRACK)){
            if (consume(TokenType.LBRACK) != null) {
                funcFParam.setArray(true);
                if(!match(TokenType.RBRACK)){
                    error(TokenType.RBRACK);
                } else {
                    consume(TokenType.RBRACK);
                }

            }
        }

        printNT("<FuncFParam>");
        return funcFParam;
    }

    /*
        函数实参表达式 FuncRParams → Exp { ',' Exp }
        1.花括号内重复0次
        2.花括号内重复多次
        3.Exp需要覆盖数组传参和部分数组传参
    */
    private FuncRParams parseFuncRParams(){
        FuncRParams funcRParams = new FuncRParams(lineNum());
        funcRParams.addExp(parseExp());

        while (match(TokenType.COMMA)){
            consume(TokenType.COMMA);
            funcRParams.addExp(parseExp());
        }

        printNT("<FuncRParams>");
        return funcRParams;
    }

    // ==================== 语句块 Block ===================
    /*
        语句块 Block → '{' { BlockItem } '}' 
        1.花括号内重复0次 2.花括号内重复多次
     */
    private Block parseBlock(){
        Block block = new Block(lineNum());
        consume(TokenType.LBRACE);
        while (!match(TokenType.RBRACE) && peek()!=null){
            block.addBlockItem(parseBlockItem());
        }
        block.setEndLine(lineNum());
        consume(TokenType.RBRACE);
        printNT("<Block>");
        return block;
    }

    /*
        语句块项 BlockItem → Decl | Stmt
        覆盖两种语句块项
     */
    private BlockItem parseBlockItem(){
        if(match(TokenType.CONSTTK, TokenType.INTTK, TokenType.STATICTK)){
            return new BlockItem(parseDecl());
        } else {
            return new BlockItem(parseStmt());
        }
    }

    // ==================== 语句 Stmt ===================
    /*
        语句 Stmt → LVal '=' Exp ';'
                    | [Exp] ';'
                    | Block
                    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                    | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                    | 'break' ';'
                    | 'continue' ';'
                    | 'return' [Exp] ';'
                    | 'printf''('StringConst {','Exp}')'';'
     */
    private Stmt parseStmt(){
        if(isAssignStmt()){
            LVal lVal = parseLVal();
            consume(TokenType.ASSIGN);
            Exp exp = parseExp();
            if(!match(TokenType.SEMICN)){
                error(TokenType.SEMICN);
            } else {
                consume(TokenType.SEMICN);
            }
            printNT("<Stmt>");
            return new AssignStmt(lineNum(), lVal, exp);
        }

        if (match(TokenType.LBRACE)){
            Block block = parseBlock();
            printNT("<Stmt>");
            return new BlockStmt(block.getLineNum(), block);
        }

        if(match(TokenType.IFTK)){
            IfStmt ifStmt = parseIfStmt();
            printNT("<Stmt>");
            return ifStmt;
        }

        if(match(TokenType.FORTK)){
            ForStmt forStmt = parseForStmt();
            printNT("<Stmt>");
            return forStmt;
        }

        if(match(TokenType.BREAKTK)){
            int lineNum=lineNum();
            consume(TokenType.BREAKTK);
            if (!match(TokenType.SEMICN)){
                error(TokenType.SEMICN);
            } else {
                consume(TokenType.SEMICN);
            }
            printNT("<Stmt>");
            return new BreakStmt(lineNum);
        }

        if(match(TokenType.CONTINUETK)){
            int lineNum=lineNum();
            consume(TokenType.CONTINUETK);
            if (!match(TokenType.SEMICN)){
                error(TokenType.SEMICN);
            } else {
                consume(TokenType.SEMICN);
            }
            printNT("<Stmt>");
            return new ContinueStmt(lineNum);
        }

        if(match(TokenType.RETURNTK)){
            int lineNum=lineNum();
            consume(TokenType.RETURNTK);
            Exp exp = null;
            if(!match(TokenType.SEMICN)){
                exp = parseExp();
            }
            if (!match(TokenType.SEMICN)){
                error(TokenType.SEMICN);
            } else {
                consume(TokenType.SEMICN);
            }
            printNT("<Stmt>");
            return new ReturnStmt(lineNum, exp);
        }

        if(match(TokenType.PRINTFTK)){
            return parsePrintfStmt();
        }

        /*
            [Exp] ';' // 有无Exp两种情况；printf函数调用
         */
        if(match(TokenType.SEMICN)){
            consume(TokenType.SEMICN);
            printNT("<Stmt>");
            return new ExpStmt(lineNum(),null);
        }

        Exp exp = parseExp();
        if(!match(TokenType.SEMICN)){
            if(match(TokenType.RPARENT)){
                return new ExpStmt(lineNum(),exp);
            }
            error(TokenType.SEMICN);
            return new ExpStmt(lineNum(),exp);
        }

        consume(TokenType.SEMICN);
        printNT("<Stmt>");
        return new ExpStmt(lineNum(),exp);
    }

    //Stmt → LVal '=' Exp ';'
    private boolean isAssignStmt() {
        Token t0 = peek(0);
        if (t0 == null || t0.getTokenType() != TokenType.IDENFR) return false;

        int i = 1;
        Token t = peek(i);
        if (t == null) return false;

        // 跳过可能的数组索引 [ Exp ]
        if (t.getTokenType() == TokenType.LBRACK) {
            i++;
            int depth = 1;
            while (true) {
                t = peek(i);
                if (t == null) return false;
                if (t.getTokenType() == TokenType.LBRACK) depth++;
                else if (t.getTokenType() == TokenType.RBRACK) depth--;
                else if (t.getTokenType() == TokenType.ASSIGN) {
                    // 提前遇到 =，说明 ] 缺失，但仍可能是赋值
                    return true;
                }
                if (depth == 0) {
                    i++; // 跳过 ]
                    break;
                }
                i++;
            }
            t = peek(i);
            if (t == null) return false;
        }

        // 现在 t 是 ] 后的 token
        return t.getTokenType() == TokenType.ASSIGN;
    }

    /*
        Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ] 
        1.有else 2.无else
     */
    private IfStmt parseIfStmt(){
        IfStmt ifStmt = new IfStmt(lineNum());
        consume(TokenType.IFTK);
        consume(TokenType.LPARENT);

        ifStmt.setCond(parseCond());
        if(!match(TokenType.RPARENT)){
            error(TokenType.RPARENT);
        }else {
            consume(TokenType.RPARENT);
        }

        Stmt thenStmt = parseStmt();
        ifStmt.setThenStmt(thenStmt);

        if(match(TokenType.ELSETK)){
            consume(TokenType.ELSETK);
            Stmt elseStmt = parseStmt();
            ifStmt.setElseStmt(elseStmt);
        }
        return ifStmt;
    }

    /*
        Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        1. 无缺省，1种情况
        2. ForStmt与Cond中缺省一个，3种情况
        3. ForStmt与Cond中缺省两个，3种情况
        4. ForStmt与Cond全部缺省，1种情况
    */
    private ForStmt parseForStmt(){
        ForStmt forStmt = new ForStmt(lineNum());
        consume(TokenType.FORTK);
        consume(TokenType.LPARENT);

        // [ForStmt]
        if(!match(TokenType.SEMICN)){
            forStmt.addInit(parseForStmtItem());
            while (match(TokenType.COMMA)){
                consume(TokenType.COMMA);
                forStmt.addInit(parseForStmtItem());
            }
            printNT("<ForStmt>");
        }
        if(!match(TokenType.SEMICN)){
            error(TokenType.SEMICN);
        } else {
            consume(TokenType.SEMICN);
        }

        // [Cond]
        if(!match(TokenType.SEMICN)){
            Cond cond = parseCond();
            forStmt.setCond(cond);
        }
        if(!match(TokenType.SEMICN)){
            error(TokenType.SEMICN);
        } else {
            consume(TokenType.SEMICN);
        }

        // [ForStmt]
        if(!match(TokenType.RPARENT)){
            forStmt.addUpdate(parseForStmtItem());
            while (match(TokenType.COMMA)){
                consume(TokenType.COMMA);
                forStmt.addUpdate(parseForStmtItem());
            }
            printNT("<ForStmt>");
        }

        if(!match(TokenType.RPARENT)){
            error(TokenType.RPARENT);
        } else{
            consume(TokenType.RPARENT);
        }

        Stmt body = parseStmt();
        forStmt.setBody(body);
        return forStmt;
    }

    private ForStmtItem parseForStmtItem(){
        LVal lVal = parseLVal();
        consume(TokenType.ASSIGN);
        Exp exp = parseExp();
        return new ForStmtItem(lineNum(), lVal, exp);
    }

    /*
        语句 Stmt → 'printf' '('StringConst {','Exp}')' ';'
        1.有Exp 2.无Exp
    */
    private PrintfStmt parsePrintfStmt(){
        consume(TokenType.PRINTFTK);
        consume(TokenType.LPARENT);
        Token stringConst = consume(TokenType.STRCON);
        PrintfStmt printfStmt = new PrintfStmt(lineNum(), stringConst);

        while (match(TokenType.COMMA)){
            consume(TokenType.COMMA);
            printfStmt.addExp(parseExp());
        }

        if(!match(TokenType.RPARENT)){
            error(TokenType.RPARENT);
        } else {
            consume(TokenType.RPARENT);
        }

        if(!match(TokenType.SEMICN)){
            error(TokenType.SEMICN);
        } else {
            consume(TokenType.SEMICN);
        }

        printNT("<Stmt>");
        return printfStmt;
    }

    // =================== 表达式 Exp ===================
    /*
        表达式 Exp → AddExp // 存在即可
     */
    private Exp parseExp(){
        Exp exp = parseAddExp();
        printNT("<Exp>");
        return exp;
    }

    /*
        条件表达式 Cond → LOrExp  //存在即可
    */
    private Cond parseCond(){
        Cond cond = new Cond(lineNum());
        cond.setlOrExp(parseLOrExp());
        printNT("<Cond>");
        return cond;
    }

    /*
        左值表达式 LVal → Ident ['[' Exp ']']
        1.普通变量、常量
        2.一维数组
    */
    private LVal parseLVal(){
        LVal lVal = new LVal(lineNum());
        Token ident = consume(TokenType.IDENFR);
        lVal.setIdent(ident);

        if(match(TokenType.LBRACK)){
            consume(TokenType.LBRACK);
            lVal.setIndex(parseExp());
            if(!match(TokenType.RBRACK)){
                error(TokenType.RBRACK);
            } else {
                consume(TokenType.RBRACK);
            }
        }
        printNT("<LVal>");
        return lVal;
    }

    /*
        基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    */
    private PrimaryExp parsePrimaryExp(){
        PrimaryExp primaryExp = new PrimaryExp(lineNum());
        if(match(TokenType.LPARENT)){
            consume(TokenType.LPARENT);
            primaryExp.setExp(parseExp());
            if(!match(TokenType.RPARENT)){
                error(TokenType.RPARENT);
            } else {
                consume(TokenType.RPARENT);
            }
        } else if (match(TokenType.IDENFR)){
            primaryExp.setLVal(parseLVal());
        } else if(match(TokenType.INTCON)){
            primaryExp.setNumber(parseNumber());
        }
        printNT("<PrimaryExp>");
        return primaryExp;
    }

    /*
        数值 Number → IntConst // 存在即可
    */
    private Number parseNumber(){
        Token number = consume(TokenType.INTCON);
        Number num = null;
        if (number != null) {
            num = new Number(number);
        }
        printNT("<Number>");
        return num;
    }

    /*
        一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        3种情况均需覆盖,函数调用也需要覆盖FuncRParams的不同情况
    */
    private boolean startsExp(TokenType tokenType){
        return tokenType==TokenType.IDENFR || tokenType==TokenType.INTCON || tokenType==TokenType.LPARENT ||
                tokenType==TokenType.PLUS || tokenType==TokenType.MINU || tokenType==TokenType.NOT;
    }

    private UnaryExp parseUnaryExp(){
       UnaryExp unaryExp = new UnaryExp(lineNum());

       // Ident '(' [FuncRParams] ')'
       if(peek(0)!=null && peek(0).getTokenType()==TokenType.IDENFR &&
          peek(1)!=null && peek(1).getTokenType()==TokenType.LPARENT){
           Token ident = consume(TokenType.IDENFR);
           consume(TokenType.LPARENT);
           FuncRParams funcRParams = null;
           // func()
           if(match(TokenType.RPARENT)){
               consume(TokenType.RPARENT);
           }
           // func(exp, exp, ...)
           else if (peek()!=null && startsExp(peek().getTokenType())){
               funcRParams = parseFuncRParams();
               if(!match(TokenType.RPARENT)){
                   error(TokenType.RPARENT);
               } else {
                   consume(TokenType.RPARENT);
               }
           }
           //func(
           else {
               ErrorList.addErrors(new Error("j", errorLine()));
           }
           unaryExp.setCall(ident, funcRParams);
           printNT("<UnaryExp>");
           return unaryExp;
       }
       
       // UnaryOp UnaryExp
       if(match(TokenType.PLUS, TokenType.MINU, TokenType.NOT)){
           Token operator = next();
           outputs.add(operator.getTokenType() + " " + operator.getLexeme());
           printNT("<UnaryOp>");
           UnaryExp operand = parseUnaryExp();       // 递归
           unaryExp.setUnary(operator, operand);
           printNT("<UnaryExp>");
           return unaryExp;
        }
           
        // PrimaryExp
        unaryExp.setPrimaryExp(parsePrimaryExp());
        printNT("<UnaryExp>");
        return unaryExp;
    }

    /*
        乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        1.UnaryExp  2.*  3./  4.%   均需覆盖
    */
    private MulExp parseMulExp(){
        MulExp mulExp = new MulExp(lineNum());
        mulExp.addUnaryExp(parseUnaryExp());
        printNT("<MulExp>");

        while (match(TokenType.MULT, TokenType.DIV, TokenType.MOD)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            mulExp.addOperator(operator);
            mulExp.addUnaryExp(parseUnaryExp());
            printNT("<MulExp>");
        }

        return mulExp;
    }

    /*
        加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
        1.MulExp  2.+ 需覆盖  3.- 需覆盖
    */
    private AddExp parseAddExp(){
        AddExp addExp = new AddExp(lineNum());
        addExp.addMulExp(parseMulExp());
        printNT("<AddExp>");

        while (match(TokenType.PLUS, TokenType.MINU)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            addExp.addOperator(operator);
            addExp.addMulExp(parseMulExp());
            printNT("<AddExp>");
        }
        return addExp;
    }

    /*
        关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        1.AddExp  2.<  3.>  4.<=  5.>=    均需覆盖
    */
    private RelExp parseRelExp(){
        RelExp relExp = new RelExp(lineNum());
        relExp.addAddExp(parseAddExp());
        printNT("<RelExp>");

        while (match(TokenType.LSS, TokenType.GRE, TokenType.LEQ, TokenType.GEQ)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            relExp.addOperator(operator);
            relExp.addAddExp(parseAddExp());
            printNT("<RelExp>");
        }

        return relExp;
    }

    /*
        相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
        1.RelExp 2.== 3.!= 均需覆盖
    */
    private EqExp parseEqExp(){
        EqExp eqExp = new EqExp(lineNum());
        eqExp.addRelExp(parseRelExp());
        printNT("<EqExp>");

        while (match(TokenType.EQL, TokenType.NEQ)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            eqExp.addOperator(operator);
            eqExp.addRelExp(parseRelExp());
            printNT("<EqExp>");
        }

        return eqExp;
    }

    /*
        逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
        1.EqExp 2.&&  均需覆盖
    */
    private LAndExp parseLAndExp(){
        LAndExp lAndExp = new LAndExp(lineNum());
        lAndExp.addEqExp(parseEqExp());
        printNT("<LAndExp>");

        while (match(TokenType.AND)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            lAndExp.addOperator(operator);
            lAndExp.addEqExp(parseEqExp());
            printNT("<LAndExp>");
        }
        return lAndExp;
    }

    /*
        逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
        1.LAndExp
        2.|| 均需覆盖
        LOrExp  → LAndExp LOrExp'
        LOrExp' → '||' LAndExp LOrExp' | ε
    */
    private LOrExp parseLOrExp(){
        LOrExp lOrExp = new LOrExp(lineNum());
        lOrExp.addLAndExp(parseLAndExp());
        printNT("<LOrExp>");

        while (match(TokenType.OR)){
            Token operator = next();
            outputs.add(operator.getTokenType() + " " + operator.getLexeme());
            lOrExp.addOperator(operator);
            lOrExp.addLAndExp(parseLAndExp());
            printNT("<LOrExp>");
        }

        return lOrExp;
    }

    /*
        常量表达式 ConstExp → AddExp
        注：使用的 Ident 必须是常量 // 存在即可
    */
    private ConstExp parseConstExp(){
        ConstExp constExp = new ConstExp(lineNum());
        constExp.setAddExp(parseAddExp());
        printNT("<ConstExp>");
        return constExp;
    }

}