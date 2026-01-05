package frontend;

import frontend.ast.CompUnit;
import frontend.ast.block.*;
import frontend.ast.decl.*;
import frontend.ast.exp.*;
import frontend.ast.exp.Number;
import frontend.ast.func.*;
import frontend.ast.stmt.*;
import frontend.error.Error;
import frontend.error.ErrorList;
import frontend.symbol.*;
import frontend.token.Token;

import java.util.ArrayList;
import java.util.List;

public class Visitor {
    private final CompUnit compUnit;
    private final SymbolManager symbolManager;
    private SymbolType currentReturnType = null;
    private boolean returnWithValue = false;
    private boolean inMainFunc = false;
    private int loopDepth=0;

    // =================== 入口 ===================
    public Visitor(CompUnit compUnit) {
        this.compUnit = compUnit;
        this.symbolManager = new SymbolManager();
    }

    public void analyze(){
        symbolManager.reset();
        insertBuiltinFunc();
        visitCompUnit();
    }

    // ================== 工具函数 ==================
    private void insertBuiltinFunc(){
        String name = "getint";
        SymbolType type = SymbolType.IntFunc;
        int id = symbolManager.getGlobalScope().getId();
        Symbol symbol = new Symbol(name,type,id,0,List.of());
        symbolManager.define(symbol);
    }

    private void addSymbol(Symbol symbol, int lineNum){
        if(!symbolManager.define(symbol)){
            Error error = new Error("b",lineNum);
            ErrorList.addErrors(error);
        }
    }

    public List<Symbol> getAllSymbols() {
        return symbolManager.getAllSymbols();
    }

    public SymbolManager getSymbolManager(){
        return symbolManager;
    }

    // ================== visit 函数 ==================
    // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    private void visitCompUnit(){
        for(Decl decl: compUnit.getDecls()){
            visitDecl(decl,true);
        }
        for (FuncDef funcDef : compUnit.getFuncDefs()){
            visitFuncDef(funcDef);
        }
        MainFuncDef mainFuncDef = compUnit.getMainFuncDef();
        visitMainFuncDef(mainFuncDef);
    }

    // 声明 Decl → ConstDecl | VarDecl
    private void visitDecl(Decl decl,boolean isGlobal){
        if(decl instanceof ConstDecl constDecl){
            visitConstDecl(constDecl);
        } else if (decl instanceof VarDecl varDecl){
            visitVarDecl(varDecl,isGlobal);
        }
    }

    // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private void visitConstDecl(ConstDecl constDecl){
        for (ConstDef constDef: constDecl.getConstDefs()){
            visitConstDef(constDef);
        }
    }

    // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    private void visitConstDef(ConstDef constDef){
        String name = constDef.getIdent().getLexeme();
        int lineNum = constDef.getLineNum();
        boolean isArray = constDef.isArray();

        boolean isConst = true;
        boolean isStatic = false;
        SymbolType type = SymbolType.VarType(isConst,isArray,isStatic);
        int scopeId = symbolManager.getCurrentScope().getId();

        Symbol symbol = new Symbol(name,type,scopeId,lineNum);
        addSymbol(symbol,lineNum);

        
        if(isArray){
            visitConstExp(constDef.getArray());
        }

        visitConstInitVal(constDef.getConstInitVal());
    }

    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    private void visitConstInitVal(ConstInitVal constInitVal){
        if (constInitVal==null) return;
        for(ConstExp constExp: constInitVal.getConstExps()){
            visitConstExp(constExp);
        }
    }

    // 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
    private void visitVarDecl(VarDecl varDecl,boolean isGlobal){
        boolean isStatic = varDecl.isStatic();

        for (VarDef varDef: varDecl.getVarDefs()) {
            visitVarDef(varDef,isGlobal,isStatic);
        }
    }

    // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
    public void visitVarDef(VarDef varDef,boolean isGlobal, boolean hasStatic){
        String name = varDef.getIdent().getLexeme();
        boolean isConst = false;
        boolean isArray = varDef.isArray();
        boolean isStatic = (!isGlobal) && hasStatic;
        SymbolType type = SymbolType.VarType(isConst, isArray, isStatic);
        int scopeId = symbolManager.getCurrentScope().getId();
        int lineNum = varDef.getLineNum();

        Symbol symbol = new Symbol(name,type,scopeId,lineNum);
        addSymbol(symbol,lineNum);

        if(isArray){
            visitConstExp(varDef.getArray());
        }
        visitInitVal(varDef.getInitVal());
    }

    // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    private void visitInitVal(InitVal initVal){
        if (initVal==null) return;
        for(Exp exp: initVal.getExps()){
            visitExp(exp);
        }
    }

    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private void visitFuncDef(FuncDef funcDef){
        String name = funcDef.getIdent().getLexeme();
        int lineNum = funcDef.getBody().getEndLine();
        boolean typeIsInt = funcDef.getFuncType().isInt();
        SymbolType funcType = SymbolType.FuncType(typeIsInt);
        int scopeId = symbolManager.getGlobalScope().getId();

        List<SymbolType> paramTypes = new ArrayList<>();
        if(funcDef.getParams()!=null) {
            for(FuncFParam funcFParam : funcDef.getParams().getFuncFParams()) {
                boolean isArray = funcFParam.isArray();
                SymbolType type = SymbolType.VarType(false,isArray,false);
                paramTypes.add(type);
            }
        }

        Symbol symbol = new Symbol(name,funcType,scopeId,lineNum,paramTypes);
        addSymbol(symbol,lineNum);

        SymbolType savedReturnType = currentReturnType;
        boolean saveInMain = inMainFunc;
        boolean saveReturnWithValue=returnWithValue;

        currentReturnType = funcType;
        inMainFunc = false;
        returnWithValue = false;

        symbolManager.pushScope();
        if (funcDef.getParams()!=null){
            visitFuncFParams(funcDef.getParams());
        }
        visitBlock(funcDef.getBody(),false);
        symbolManager.popScope();
        if (currentReturnType == SymbolType.IntFunc && !blockAlwaysReturn(funcDef.getBody())){
            ErrorList.addErrors(new Error("g",lineNum));
        }

        currentReturnType = savedReturnType;
        inMainFunc = saveInMain;
        returnWithValue = saveReturnWithValue;
    }

    private boolean blockAlwaysReturn(Block block) {
        if(block==null) return false;
        List<BlockItem> blockItems = block.getBlockItems();
        for (int i=blockItems.size()-1; i>=0; i--){
            BlockItem blockItem = blockItems.get(i);
            if (!blockItem.isDecl()){
                Stmt stmt = blockItem.getStmt();
                if (stmt!=null){
                    return stmtAlwaysReturn(stmt);
                }
            }
        }
        return false;
    }

    private boolean stmtAlwaysReturn(Stmt stmt){
        if (stmt instanceof ReturnStmt) return true;
        if (stmt instanceof BlockStmt blockStmt) return blockAlwaysReturn(blockStmt.getBlock());

        if (stmt instanceof IfStmt ifStmt){
            Stmt thenStmt = ifStmt.getThenStmt();
            Stmt elseStmt = ifStmt.getElseStmt();
            if (elseStmt==null) return false;
            return stmtAlwaysReturn(thenStmt) && stmtAlwaysReturn(elseStmt);
        }

        return false;
    }

    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(MainFuncDef mainFuncDef){
        if (mainFuncDef==null) return;
        Block block = mainFuncDef.getBody();
        int lineNum=mainFuncDef.getBody().getEndLine();

        SymbolType savedReturnType = currentReturnType;
        boolean savedInMain = inMainFunc;
        boolean saveReturnWithValue = returnWithValue;

        currentReturnType = SymbolType.IntFunc;
        inMainFunc = true;
        returnWithValue=false;

        symbolManager.pushScope();
        visitBlock(block,false);
        symbolManager.popScope();

        if(!blockAlwaysReturn(block)){
            ErrorList.addErrors(new Error("g", lineNum));
        }
        currentReturnType = savedReturnType;
        inMainFunc = savedInMain;
        returnWithValue = saveReturnWithValue;
    }

    // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    private void visitFuncFParams(FuncFParams funcFParams) {
        if (funcFParams == null) return;
        for (FuncFParam funcFParam : funcFParams.getFuncFParams()) {
            visitFuncFParam(funcFParam);
        }
    }

    // 函数形参 FuncFParam → BType Ident ['[' ']']
    private void visitFuncFParam(FuncFParam funcFParam) {
        if (funcFParam==null) return;
        Token ident = funcFParam.getIdent();

        String name=null;
        if(ident!= null){
            name = ident.getLexeme();
        }
        int lineNum = funcFParam.getLineNum();
        boolean isArray = funcFParam.isArray();
        boolean isConst = false;
        boolean isStatic = false;
        SymbolType type = SymbolType.VarType(isConst,isArray,isStatic);
        int scopeId = symbolManager.getCurrentScope().getId();
        Symbol symbol = new Symbol(name,type,scopeId,lineNum,true);
        addSymbol(symbol,lineNum);
    }

    // 语句块 Block → '{' { BlockItem } '}'
    // 语句块项 BlockItem → Decl | Stmt
    private void visitBlock(Block block, boolean newScope) {
        if (block ==null) return;
        if(newScope) symbolManager.pushScope();

        for(BlockItem blockItem : block.getBlockItems()) {
            if (blockItem.isDecl()) {
                visitDecl(blockItem.getDecl(),false);
            } else {
                visitStmt(blockItem.getStmt());
            }
        }

        if (newScope) symbolManager.popScope();
    }

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
    private void visitStmt(Stmt stmt) {
        if (stmt==null) return;
        if(stmt instanceof AssignStmt assignStmt){
            visitAssignStmt(assignStmt);
        } else if(stmt instanceof  ExpStmt expStmt){
            visitExpStmt(expStmt);
        } else if(stmt instanceof BlockStmt blockStmt) {
            visitBlockStmt(blockStmt);
        } else if(stmt instanceof IfStmt ifStmt) {
            visitIfStmt(ifStmt);
        } else if(stmt instanceof ForStmt forStmt){
            visitForStmt(forStmt);
        } else if(stmt instanceof BreakStmt breakStmt){
            visitBreakStmt(breakStmt);
        } else if(stmt instanceof ContinueStmt continueStmt){
            visitContinueStmt(continueStmt);
        } else if(stmt instanceof ReturnStmt returnStmt){
            visitReturnStmt(returnStmt);
        } else if(stmt instanceof  PrintfStmt printfStmt) {
            visitPrintfStmt(printfStmt);
        }
    }

    // 语句 Stmt → LVal '=' Exp ';'
    private void  visitAssignStmt(AssignStmt assignStmt){
        LVal lval = assignStmt.getLVal();
        Exp exp = assignStmt.getExp();
        visitLVal(lval,true);
        visitExp(exp);
    }

    // 语句 Stmt → [Exp] ';'
    private void visitExpStmt(ExpStmt expStmt){
        Exp exp = expStmt.getExp();
        if(exp!=null){
            visitExp(exp);
        }
    }

    // 语句 Stmt → Block
    private void visitBlockStmt(BlockStmt blockStmt){
        visitBlock(blockStmt.getBlock(),true);
    }

    // 语句 Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void visitIfStmt(IfStmt ifStmt){
        Cond cond = ifStmt.getCond();
        Stmt thenStmt = ifStmt.getThenStmt();
        Stmt elseStmt = ifStmt.getElseStmt();
        visitCond(cond);
        visitStmt(thenStmt);
        if(elseStmt!=null) {
            visitStmt(elseStmt);
        }
    }

    // 语句 Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    private void visitForStmt(ForStmt forStmt){
        for(ForStmtItem init: forStmt.getInit()){
            visitForStmtItem(init);
        }
        Cond cond = forStmt.getCond();
        if (cond!=null) {
            visitCond(cond);
        }

        loopDepth++;
        Stmt body = forStmt.getBody();
        if ((body!=null)){
            visitStmt(body);
        }
        loopDepth--;

        for (ForStmtItem update: forStmt.getUpdate()){
            visitForStmtItem(update);
        }
    }

    // 语句 Stmt → 'break' ';'
    private void visitBreakStmt(BreakStmt breakStmt){
        if (loopDepth==0){
            Error error = new Error("m",breakStmt.getLineNum());
            ErrorList.addErrors(error);
        }
    }

    // 语句 Stmt → 'continue' ';'
    private void visitContinueStmt(ContinueStmt continueStmt){
        if (loopDepth==0){
            Error error = new Error("m", continueStmt.getLineNum());
            ErrorList.addErrors(error);
        }
    }

    // 语句 Stmt → 'return' [Exp] ';'
    private void visitReturnStmt(ReturnStmt returnStmt){
        Exp exp=returnStmt.getExp();
        int lineNum = returnStmt.getLineNum();
        if(exp != null) visitExp(exp);
        if(currentReturnType == null) return;

        if(currentReturnType==SymbolType.VoidFunc){
            if(exp!=null){
                ErrorList.addErrors(new Error("f", lineNum));
            }
        } else if(currentReturnType==SymbolType.IntFunc){
            if (exp!=null){
                returnWithValue=true;
            }
        }
    }

    // 语句 Stmt → 'printf''('StringConst {','Exp}')'';'
    private void visitPrintfStmt(PrintfStmt printfStmt){
        int lineNum = printfStmt.getLineNum();
        String string = printfStmt.getString().getLexeme();
        List<Exp> exps = printfStmt.getExps();

        int n=0;
        for(int i=0; i<string.length()-1; i++){
            if (string.charAt(i)=='%' && string.charAt(i+1)=='d'){
                n++;
            }
        }

        for (Exp exp: exps){
            visitExp(exp);
        }

        if(n!=exps.size()){
            ErrorList.addErrors(new Error("l",lineNum));
        }
    }

    // 语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    private void visitForStmtItem(ForStmtItem forStmtItem) {
        LVal lVal=forStmtItem.getLVal();
        Exp exp= forStmtItem.getExp();
        visitLVal(lVal,true);
        visitExp(exp);
    }

    // 表达式 Exp → AddExp
    private void visitExp(Exp exp){
        if (exp instanceof AddExp addExp){
            visitAddExp(addExp);
        }
    }

    // 条件表达式 Cond → LOrExp
    private void visitCond(Cond cond){
        if(cond==null) return;
        LOrExp lOrExp = cond.getlOrExp();
        visitLOrEXP(lOrExp);
    }

    // 左值表达式 LVal → Ident ['[' Exp ']']
    private void visitLVal(LVal lVal,boolean asLeft){
        String name = lVal.getIdent().getLexeme();
        int lineNum = lVal.getLineNum();
        Symbol symbol = symbolManager.lookup(name);
        if (symbol==null) {
            ErrorList.addErrors(new Error("c",lineNum));
        } else {
            if (asLeft && symbol.isConst()){
                ErrorList.addErrors(new Error("h",lineNum));
            }
        }

        if (lVal.getIndex() !=null){
            visitExp(lVal.getIndex());
        }
    }

    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    private void visitPrimaryExp(PrimaryExp primaryExp){
        Exp exp = primaryExp.getExp();
        LVal lVal = primaryExp.getLVal();
        Number number = primaryExp.getNumber();
        if(exp!=null){
            visitExp(exp);
        } else if(lVal!=null){
            visitLVal(lVal,false);
        } else if(number!=null){
            visitNumber(number);
        }

    }

    // 数值 Number → IntConst
    private void visitNumber(Number number) {

    }

    // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private void visitUnaryExp(UnaryExp unaryExp) {
        PrimaryExp primaryExp = unaryExp.getPrimaryExp();
        Token ident = unaryExp.getIdent();

        //PrimaryExp
        if(primaryExp!=null) {
            visitPrimaryExp(primaryExp);
            return;
        }

        //Ident '(' [FuncRParams] ')'
        if(ident!=null){
            String name = ident.getLexeme();
            int lineNum = ident.getLineNum();

            FuncRParams funcRParams = unaryExp.getFuncRParams();
            List<Exp> exps = new ArrayList<>();
            if(funcRParams!=null){
                exps = funcRParams.getExps();
                visitFuncRParams(funcRParams);
            }

            Symbol symbol = symbolManager.lookup(name);
            if(symbol==null || !symbol.isFunc()){
                ErrorList.addErrors(new Error("c",lineNum));
                return;
            }

            List<SymbolType> paramTypes=symbol.getParamTypes();
            if(paramTypes.size()!=exps.size()){
                ErrorList.addErrors(new Error("d",lineNum));
                return;
            }

            boolean mismatch=false;
            for (int i=0; i<exps.size(); i++){
                SymbolType expected = paramTypes.get(i);
                Exp exp = exps.get(i);
                if(typeMismatch(expected,exp)) {
                    mismatch=true;
                    break;
                }
            }
            if (mismatch){
                ErrorList.addErrors(new Error("e",lineNum));
            }

            return;
        }

        //UnaryOp UnaryExp
        if(unaryExp.getUnaryExp()!=null){
            visitUnaryExp(unaryExp.getUnaryExp());
        }
    }

    private boolean typeMismatch(SymbolType expected, Exp exp){
        boolean expectedArr= (expected == SymbolType.IntArray);
        LVal lVal = null;
        if (exp instanceof LVal lv){
            lVal = lv;
        } else if(exp instanceof AddExp addExp){
            if (addExp.getMulExps().size()==1 && addExp.getOperators().isEmpty()){
                Exp exp1 = addExp.getMulExps().get(0);
                if (exp1 instanceof MulExp mulExp){
                    if(mulExp.getUnaryExps().size()==1 && mulExp.getOperators().isEmpty()){
                        Exp e2 = mulExp.getUnaryExps().get(0);
                        if (e2 instanceof UnaryExp unaryExp){
                            if (unaryExp.getPrimaryExp()!=null && unaryExp.getIdent()==null && unaryExp.getUnaryExp()==null){
                                PrimaryExp primaryExp = unaryExp.getPrimaryExp();
                                if (primaryExp.getLVal()!=null && primaryExp.getExp()==null && primaryExp.getNumber()==null){
                                    lVal=primaryExp.getLVal();
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean wholeArray = false;
        boolean constArray = false;

        if(lVal!=null){
            String name = lVal.getIdent().getLexeme();
            Symbol symbol=symbolManager.lookup(name);

            if (symbol==null)  return false;

            if(symbol.isArray() && lVal.getIndex()==null) {
                wholeArray =true;
                constArray = symbol.isConstArray();
            }
        }
        if(expectedArr){
            if (!wholeArray) return true;
            return constArray;
        } else {
            return wholeArray;
        }
    }

    // 函数实参表达式 FuncRParams → Exp { ',' Exp }
    private void visitFuncRParams(FuncRParams funcRParams){
        if(funcRParams==null) return;
        for(Exp exp: funcRParams.getExps()){
            visitExp(exp);
        }
    }

    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private void visitMulExp(MulExp mulExp){
        if(mulExp==null) return;
        for(Exp unary:mulExp.getUnaryExps()){
            if (unary instanceof UnaryExp unaryExp){
                visitUnaryExp(unaryExp);
            }
        }
    }

    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    private void visitAddExp(AddExp addExp){
        if (addExp==null) return;
        for (Exp mul : addExp.getMulExps()){
            if(mul instanceof MulExp mulExp){
                visitMulExp(mulExp);
            }
        }

    }

    //关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private void visitRelExp(RelExp relExp){
        if(relExp==null) return;
        for(Exp add:relExp.getAddExps()){
            if (add instanceof AddExp addExp){
                visitAddExp(addExp);
            }
        }
    }

    //相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private void visitEqExp(EqExp eqExp){
        if (eqExp==null) return;
        for (Exp rel: eqExp.getRelExps()){
            if (rel instanceof  RelExp relExp){
                visitRelExp(relExp);
            }
        }
    }

    //逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
    private void visitLAndExp(LAndExp lAndExp){
        if (lAndExp==null) return;
        for(Exp exp : lAndExp.getEqExps()){
            if(exp instanceof EqExp eqExp){
                visitEqExp(eqExp);
            }
        }
    }

    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    private void visitLOrEXP(LOrExp lOrExp) {
        if (lOrExp==null) return;
        for(Exp exp : lOrExp.getLAndExps()){
            if(exp instanceof LAndExp lAndExp){
                visitLAndExp(lAndExp);
            }
        }
    }

    // 常量表达式 ConstExp → AddExp
    private void visitConstExp(ConstExp constExp){
        if (constExp==null) return;
        visitAddExp(constExp.getAddExp());
    }

}
