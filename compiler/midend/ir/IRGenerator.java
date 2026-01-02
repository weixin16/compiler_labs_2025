package midend.ir;

import frontend.ast.CompUnit;
import frontend.ast.block.Block;
import frontend.ast.block.BlockItem;
import frontend.ast.decl.*;
import frontend.ast.exp.*;
import frontend.ast.exp.Number;
import frontend.ast.func.*;
import frontend.ast.stmt.*;
import frontend.symbol.Symbol;
import frontend.symbol.SymbolManager;
import frontend.symbol.SymbolTable;
import frontend.token.Token;

import java.util.*;

public class IRGenerator {
    private final CompUnit compUnit;
    private final SymbolManager symbolManager;
    private final IRBuilder irBuilder;
    private SymbolTable currentScope;
    private final Map<SymbolTable, Integer> childIndex = new HashMap<>();
    private final List<String> breakLabelStack = new ArrayList<>();
    private final List<String> continueLabelStack = new ArrayList<>();
    private final Map<String, Integer> constScalar = new HashMap<>();
    private final Map<String, int[]> constArray = new HashMap<>();
    private final Deque<Map<String,Symbol>> visible = new ArrayDeque<>();

    public IRGenerator(CompUnit compUnit, SymbolManager symbolManager, IRBuilder irBuilder) {
        this.compUnit = compUnit;
        this.symbolManager = symbolManager;
        this.irBuilder = irBuilder;
        this.currentScope = symbolManager.getGlobalScope();
        visible.push(new HashMap<>());
    }

    // =================== 入口 ===================
    public void generate(){
        genCompUnit(compUnit);
    }

    // ================== 工具函数 ==================
    private void enterScope(){
        List<SymbolTable> sons = currentScope.getSon();
        int index = childIndex.getOrDefault(currentScope,0);
        if (index>=sons.size()){
            throw new IllegalStateException("enterScope: no child scope left for scope " + currentScope.getId());
        }
        childIndex.put(currentScope,index+1);
        currentScope = sons.get(index);
        visible.push(new HashMap<>());
    }

    private void exitScope(){
        SymbolTable father = currentScope.getFather();
        if (father==null){
            throw new IllegalStateException("exitScope: already at global scope");
        }
        visible.pop();
        currentScope = father;
    }

    private Symbol lookupSymbol(String name){
        SymbolTable symbolTable = currentScope;
        while (symbolTable!=null){
            Symbol symbol = symbolTable.lookupLocal(name);
            if (symbol!=null){
                return symbol;
            }
            symbolTable = symbolTable.getFather();
        }
        return null;
    }

    private Symbol lookupSymbolVisible(String name){
        for (var map : visible){
            Symbol symbol = map.get(name);
            if (symbol !=null) return symbol;
        }
        return null;
    }

    private void activateVisible(String name){
        Symbol symbol = currentScope.lookupLocal(name);
        if (symbol!=null){
            if (visible.peek() != null) {
                visible.peek().put(name,symbol);
            }
        }
    }

    private Symbol lookupFunc(String name){
        SymbolTable global = symbolManager.getGlobalScope();
        return global.lookupLocal(name);
    }

    private boolean isGlobal(Symbol symbol){
        if (symbol==null) return false;
        int globalId = symbolManager.getGlobalScope().getId();
        return symbol.getScopeId()==globalId || symbol.isStatic();
    }

    private String irNameOf(Symbol symbol){
        String base = symbol.getName();
        int scopeId= symbol.getScopeId();
        int globalId = symbolManager.getGlobalScope().getId();

        //全局变量
        if(scopeId==globalId){
            return "g_" + base;
        }

        //形参
        if(symbol.isParam()){
            return "p"+scopeId+"_"+base;
        }

        //static局部
        if(symbol.isStatic()){
            return "s" + scopeId + "_"+base;
        }

        //普通局部
        return "v"+scopeId+"_"+base;
    }

    //编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    private void genCompUnit(CompUnit compUnit){
        for (Decl decl:compUnit.getDecls()){
            genDecl(decl);
        }
        for (FuncDef funcDef:compUnit.getFuncDefs()){
            enterScope();
            genFuncDef(funcDef);
            exitScope();
        }

        MainFuncDef mainFuncDef=compUnit.getMainFuncDef();
        if (mainFuncDef!=null){
            enterScope();
            genMainFuncDef(mainFuncDef);
            exitScope();
        }
    }

    //声明 Decl → ConstDecl | VarDecl
    private void genDecl(Decl decl) {
        if(decl instanceof ConstDecl constDecl){
            genConstDecl(constDecl);
        } else if(decl instanceof VarDecl varDecl){
            genVarDecl(varDecl);
        }
    }

    //常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private void genConstDecl(ConstDecl constDecl) {
        if(constDecl==null) return;
        for(ConstDef constDef: constDecl.getConstDefs()){
            genConstDef(constDef);
        }

    }

    //常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    private  void genConstDef(ConstDef constDef){
        if(constDef==null) return;

        String name = constDef.getIdent().getLexeme();
        Symbol symbol = lookupSymbol(name);
        String irName;
        if(symbol==null){
            irName = name;
        } else {
            irName=irNameOf(symbol);
        }

        int len=0;
        boolean isArray = constDef.isArray();
        boolean isGlobal = isGlobal(symbol);

        if(!isArray){
            if(isGlobal){
                irBuilder.emitGlobal("gdecl",irName,"1",null);
            } else {
                irBuilder.emit("decl",irName,"1",null);
            }

        } else {
            len = evalConstExp(constDef.getArray());
            if (isGlobal){
                irBuilder.emitGlobal("gdecl",irName,Integer.toString(len),null);
            } else {

                irBuilder.emit("decl",irName,Integer.toString(len),null);
            }

        }


        //常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
        ConstInitVal constInitVal = constDef.getConstInitVal();
        if (constInitVal==null) return;

        List<ConstExp> constExps = constInitVal.getConstExps();
        if (constExps ==null || constExps.isEmpty()) return;

        if(!isArray){
            int value = evalConstExp(constExps.get(0));
            constScalar.put(irName, value);
            if (isGlobal) {
                irBuilder.emitGlobal("ginit", Integer.toString(value), null, irName);
            } else {
                irBuilder.emit("move", Integer.toString(value), null, irName);
            }

        } else {
            int[] arr = new int[len];
            for(int i = 0; i< constExps.size(); i++){
                String index = Integer.toString(i);
                int value = evalConstExp(constExps.get(i));
                arr[i] = value;
                if(isGlobal){
                    irBuilder.emitGlobal("ginitarr", Integer.toString(value), index, irName);
                } else {
                    irBuilder.emit("storearr", Integer.toString(value), index, irName);
                }
            }
            constArray.put(irName,arr);
        }

        activateVisible(name);

    }

    //变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
    private  void genVarDecl(VarDecl varDecl){
        if (varDecl==null) return;
        for (VarDef varDef : varDecl.getVarDefs()){
            genVarDef(varDef);
        }
    }

    //变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal //
    private void genVarDef(VarDef varDef){
        Token ident = varDef.getIdent();
        String name = ident.getLexeme();
        Symbol symbol = lookupSymbol(name);

        String irName;
        if(symbol==null){
            irName = name;
        } else {
            irName=irNameOf(symbol);
        }

        boolean isArray = varDef.isArray();
        boolean isGlobal = isGlobal(symbol);
        if(!isArray){
            if (isGlobal){
                irBuilder.emitGlobal("gdecl", irName,"1",null);
            } else {
                irBuilder.emit("decl", irName,"1",null);
            }

        } else {
            int len = evalConstExp(varDef.getArray());
            if (isGlobal){
                irBuilder.emitGlobal("gdecl",irName,Integer.toString(len),null);
            } else {
                irBuilder.emit("decl",irName,Integer.toString(len),null);
            }

        }
        InitVal initVal = varDef.getInitVal();
        if (initVal==null) {
            activateVisible(name);
            return;
        }


        //变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
        List<Exp> exps = initVal.getExps();
        if (exps==null || exps.isEmpty()) {
            activateVisible(name);
            return;
        }

        if(!isArray){
            Exp exp =exps.get(0);
            if (isGlobal){
                int value = evalAddExp((AddExp)exp);
                irBuilder.emitGlobal("ginit",Integer.toString(value),null,irName);
            } else {
                String value = genExp(exp);
                irBuilder.emit("move", value, null, irName);
            }
        } else {
            for(int i=0; i<exps.size(); i++){
                Exp exp = exps.get(i);
                if(exp==null) continue;
                String index = Integer.toString(i);
                if (isGlobal){
                    int value = evalAddExp((AddExp) exps.get(i));
                    irBuilder.emitGlobal("ginitarr",Integer.toString(value),index,irName);
                } else {
                    String value = genExp(exp);
                    irBuilder.emit("storearr", value, index, irName);
                }
            }
        }
        activateVisible(name);
    }

    //函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private void genFuncDef(FuncDef funcDef) {
        if(funcDef==null) return;

        String name=funcDef.getIdent().getLexeme();
        irBuilder.emit("func",name,null,null);

        FuncFParams funcFParams = funcDef.getParams();
        if (funcFParams!=null){
            genFuncFParams(funcFParams);
        }

        Block block = funcDef.getBody();
        genBlock(block, false);

        Quad last = irBuilder.lastQuad();
        if (last==null || !last.op.equals("ret")){
            irBuilder.emit("ret", "0", null, null);
        }

        irBuilder.emit("endfunc",name,null,null);
    }

    //主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    private void genMainFuncDef(MainFuncDef mainFuncDef){
        if (mainFuncDef==null) return;

        String name = "main";
        irBuilder.emit("func",name,null,null);

        Block block=mainFuncDef.getBody();
        genBlock(block,false);

        Quad last = irBuilder.lastQuad();
        if (last==null || !last.op.equals("ret")){
            irBuilder.emit("ret", "0", null, null);
        }

        irBuilder.emit("endfunc",name,null,null);
    }

    //函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    private void genFuncFParams(FuncFParams funcFParams){
        if(funcFParams == null) return;
        for (FuncFParam funcFParam : funcFParams.getFuncFParams()){
            genFuncFParam(funcFParam);
        }
    }

    // 函数形参 FuncFParam → BType Ident ['[' ']']
    private void genFuncFParam(FuncFParam funcFParam) {
        if (funcFParam == null) return;
        String name = funcFParam.getIdent().getLexeme();
        Symbol symbol = lookupSymbol(name);
        if (symbol==null) return;

        String irName=irNameOf(symbol);

        if (symbol.isArray()){
            irBuilder.emit("fparam_arr", irName, null, null);
        } else {
            irBuilder.emit("fparam", irName, null, null);
        }

        activateVisible(name);
    }

    //语句块 Block → '{' { BlockItem } '}'
    private void genBlock(Block block, boolean newScope){
        if (block==null) return;
        if(newScope){
            enterScope();
        }
        for(BlockItem blockItem:block.getBlockItems()){
            genBlockItem(blockItem);
        }
        if (newScope){
            exitScope();
        }
    }

    //语句块项 BlockItem → Decl | Stmt
    private  void genBlockItem(BlockItem blockItem){
        if (blockItem==null) return;
        Decl decl = blockItem.getDecl();
        Stmt stmt = blockItem.getStmt();
        if (decl != null){
            genDecl(decl);
        } else if(stmt!=null){
            genStmt(stmt);
        }
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
    private void genStmt(Stmt stmt){
        if (stmt==null) return;
        if(stmt instanceof AssignStmt assignStmt){
            genAssignStmt(assignStmt);
        } else if(stmt instanceof  ExpStmt expStmt){
            genExpStmt(expStmt);
        } else if(stmt instanceof BlockStmt blockStmt) {
            genBlockStmt(blockStmt);
        } else if(stmt instanceof IfStmt ifStmt) {
            genIfStmt(ifStmt);
        } else if(stmt instanceof ForStmt forStmt){
            genForStmt(forStmt);
        } else if(stmt instanceof BreakStmt breakStmt){
            genBreakStmt(breakStmt);
        } else if(stmt instanceof ContinueStmt continueStmt){
            genContinueStmt(continueStmt);
        } else if(stmt instanceof ReturnStmt returnStmt){
            genReturnStmt(returnStmt);
        } else if(stmt instanceof  PrintfStmt printfStmt) {
            genPrintfStmt(printfStmt);
        }
    }

    // 语句 Stmt → LVal '=' Exp ';'
    private void genAssignStmt(AssignStmt assignStmt){
        if (assignStmt==null) return;
        LVal lVal = assignStmt.getLVal();
        Exp exp = assignStmt.getExp();

        String value = genExp(exp);
        String name = lVal.getIdent().getLexeme();
        Symbol symbol = lookupSymbolVisible(name);
        String irName;
        if (symbol==null){
            irName = name;
        }else {
            irName = irNameOf(symbol);
        }

        if (lVal.getIndex()==null){
            irBuilder.emit("move",value,null,irName);
        } else {
            String place = genExp(lVal.getIndex());
            irBuilder.emit("storearr", value, place,irName);
        }
    }

    // 语句 Stmt → [Exp] ';'
    private void genExpStmt(ExpStmt expStmt){
        if (expStmt==null) return;
        Exp exp = expStmt.getExp();
        if(exp!=null){
            genExp(exp);
        }
    }

    // 语句 Stmt → Block
    private void genBlockStmt(BlockStmt blockStmt){
        if (blockStmt==null) return;
        Block block= blockStmt.getBlock();
        genBlock(block,true);
    }

    // 语句 Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void genIfStmt(IfStmt ifStmt){
        if(ifStmt == null) return;
        Stmt thenStmt =ifStmt.getThenStmt();
        Stmt elseStmt = ifStmt.getElseStmt();

        if(elseStmt==null){
            String endLabel =irBuilder.newLabel("if_end");
            genCond(ifStmt.getCond(), endLabel);
            genStmt(thenStmt);
            irBuilder.emit("label",null,null, endLabel);
        } else {
            String elseLabel = irBuilder.newLabel("if_else");
            String endLabel =irBuilder.newLabel("if_end");
            genCond(ifStmt.getCond(),elseLabel);
            genStmt(thenStmt);
            irBuilder.emit("j",null,null,endLabel);

            irBuilder.emit("label",null,null, elseLabel);
            genStmt(elseStmt);

            irBuilder.emit("label",null,null, endLabel);

        }
    }

    // 语句 Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    private void genForStmt(ForStmt forStmt){
        if(forStmt==null) return;

        //init
        List<ForStmtItem> init = forStmt.getInit();
        if (init !=null) {
            for(ForStmtItem item: forStmt.getInit()){
                genForStmtItem(item);
            }
        }

        Cond cond = forStmt.getCond();
        Stmt body = forStmt.getBody();
        List<ForStmtItem> updates = forStmt.getUpdate();

        String condLabel = irBuilder.newLabel("for_cond");
        String stepLabel = irBuilder.newLabel("for_step");
        String endLabel = irBuilder.newLabel("for_end");
        String continueTarget = (updates!=null && !updates.isEmpty()) ? stepLabel:condLabel;
        breakLabelStack.add(endLabel);
        continueLabelStack.add(continueTarget);
        irBuilder.emit("label", null, null, condLabel);

        if (cond!=null) genCond(cond,endLabel);
        genStmt(body);
        irBuilder.emit("label",null,null, stepLabel);

        if (updates!=null) {
            for (ForStmtItem item : updates){
                genForStmtItem(item);
            }
        }
        irBuilder.emit("j", null, null, condLabel);
        irBuilder.emit("label",null,null, endLabel);
        breakLabelStack.remove(breakLabelStack.size()-1);
        continueLabelStack.remove(continueLabelStack.size()-1);
    }

    // // 语句 Stmt → 'break' ';'
    private void genBreakStmt(BreakStmt breakStmt){
        if (breakStmt==null) return;
        if (breakLabelStack.isEmpty()) return;
        String target = breakLabelStack.get(breakLabelStack.size()-1);
        irBuilder.emit("j", null, null, target);
    }

    // 语句 Stmt → 'continue' ';'
    private void genContinueStmt(ContinueStmt continueStmt){
        if (continueStmt==null) return;
        if (continueLabelStack.isEmpty()) return;
        String target = continueLabelStack.get(continueLabelStack.size()-1);
        irBuilder.emit("j", null, null, target);
    }

    // 语句 Stmt → 'return' [Exp] ';'
    private void genReturnStmt(ReturnStmt returnStmt){
        if(returnStmt==null) return;
        Exp exp = returnStmt.getExp();
        if(exp!=null){
            String place = genExp(exp);
            irBuilder.emit("ret",place,null,null);
        } else {
            irBuilder.emit("ret",null,null,null);
        }
    }

    // 语句 Stmt → 'printf''('StringConst {','Exp}')'';'
    private void genPrintfStmt(PrintfStmt printfStmt){
        if (printfStmt==null) return;
        String content = printfStmt.getString().getLexeme();
        //去掉引号
        content = content.substring(1, content.length()-1);

        List<Exp> args = printfStmt.getExps();
        if (args==null){
            args=List.of();
        }

        List<String> argsVal = new ArrayList<>();
        for (Exp arg: args){
            String val = genExp(arg);
            String temp = irBuilder.newTemp();
            irBuilder.emit("move", val, null, temp);
            argsVal.add(temp);
        }

        int index = 0;
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<content.length(); i++){
            char c = content.charAt(i);
            if (c=='%' && i+1<content.length() && content.charAt(i+1)=='d') {
                //处理普通字符串
                if(sb.length()>0){
                    irBuilder.emit("print_str", sb.toString(), null,null);
                    sb.setLength(0);
                }
                //处理%d
                String value = argsVal.get(index++);
                irBuilder.emit("print_int", value, null, null);
                i++;
            } else if (c=='\\' && i+1<content.length()){
                char n = content.charAt(i+1);
                if (n=='n'){
                    sb.append("\n");
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }

        if(sb.length()>0){
            irBuilder.emit("print_str", sb.toString(),null,null);
        }
    }


    //语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    private  void genForStmtItem(ForStmtItem forStmtItem){
        if (forStmtItem==null) return;
        LVal lVal = forStmtItem.getLVal();
        Exp exp = forStmtItem.getExp();
        if (lVal==null || exp==null) return;

        String value = genExp(exp);
        String name = lVal.getIdent().getLexeme();
        Symbol symbol = lookupSymbolVisible(name);
        String irName;
        if(symbol==null){
            irName=name;
        } else {
            irName=irNameOf(symbol);
        }

        if(lVal.getIndex()==null){
            irBuilder.emit("move", value, null, irName);
        } else {
            String index = genExp(lVal.getIndex());
            irBuilder.emit("storearr", value, index, irName);
        }
    }

    //表达式 Exp → AddExp
    private String genExp(Exp exp){
        if(exp==null) return null;
        if(exp instanceof AddExp addExp){
            return genAddExp(addExp);
        }
        throw new IllegalStateException("Bad Exp: " + exp.getClass());
    }

    //条件表达式 Cond → LOrExp
    private void genCond(Cond cond, String falseLabel){
        if(cond == null) return;
        LOrExp lOrExp = cond.getlOrExp();
        genLOrExp(lOrExp, falseLabel);
    }

    //左值表达式 LVal → Ident ['[' Exp ']']
    private String genLVal(LVal lVal){
        String name = lVal.getIdent().getLexeme();
        Exp index=lVal.getIndex();
        Symbol symbol = lookupSymbolVisible(name);

        String irName;
        if(symbol==null){
            irName = name;
        } else {
            irName = irNameOf(symbol);
        }

        if (index==null){
            String temp = irBuilder.newTemp();
            irBuilder.emit("load",irName,null,temp);
            return temp;
        } else {
            if (symbol==null || !symbol.isArray()){
                throw new IllegalStateException("indexing non-array: " + name);
            }
            String place = genExp(index);
            String temp= irBuilder.newTemp();
            irBuilder.emit("loadarr",irName,place,temp);
            return temp;
        }
    }

    //基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    private String genPrimaryExp(PrimaryExp primaryExp){
        Exp exp=primaryExp.getExp();
        LVal lVal=primaryExp.getLVal();
        Number number=primaryExp.getNumber();

        if (exp!=null){
            return genExp(exp);
        } else if (lVal!=null){
            return genLVal(lVal);
        } else if (number!=null){
            return genNumber(number);
        }

        return null;
    }

    private int evalPrimaryExp(PrimaryExp primaryExp){
        if (primaryExp.getNumber()!=null) return primaryExp.getNumber().getValue();
        if (primaryExp.getLVal()!=null) return evalConstLVal(primaryExp.getLVal());
        if (primaryExp.getExp()!=null) return evalAddExp((AddExp) primaryExp.getExp());
        throw new IllegalStateException();
    }

    private int evalConstLVal(LVal lVal){
        String name = lVal.getIdent().getLexeme();
        Symbol sym = lookupSymbolVisible(name);
        if (sym == null) throw new IllegalStateException("const eval undefined: " + name);
        String ir = irNameOf(sym);

        if (lVal.getIndex() == null) {
            Integer v = constScalar.get(ir);
            if (v == null) throw new IllegalStateException("not a const scalar: " + name);
            return v;
        } else {
            int idx = evalAddExp((AddExp) lVal.getIndex());  // 你的 Exp 实际就是 AddExp 体系
            int[] arr = constArray.get(ir);
            if (arr == null) throw new IllegalStateException("not a const array: " + name);
            if (idx < 0 || idx >= arr.length) throw new IllegalStateException("const index out of range");
            return arr[idx];
        }
    }

    //数值 Number → IntConst
    private String genNumber(Number number){
        int intConst = number.getValue();
        return Integer.toString(intConst);
    }

    //一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private String genUnaryExp(UnaryExp unaryExp){
        if (unaryExp==null) return null;

        //PrimaryExp
        PrimaryExp primaryExp = unaryExp.getPrimaryExp();
        if (primaryExp!=null){
            return genPrimaryExp(primaryExp);
        }

        //Ident '(' [FuncRParams] ')'
        Token ident = unaryExp.getIdent();
        if (ident!=null){
            String funcName = ident.getLexeme();
            FuncRParams funcRParams = unaryExp.getFuncRParams();

            //getint()
            if (funcName.equals("getint") && (funcRParams==null || funcRParams.getExps() == null || funcRParams.getExps().isEmpty())) {
                String ret = irBuilder.newTemp();
                irBuilder.emit("getint", null, null,ret);
                return ret;
            }

            return genFuncRParams(funcRParams, funcName);
        }

        //UnaryOp UnaryExp
        UnaryExp unary=unaryExp.getUnaryExp();
        if (unary!=null){
            String src = genUnaryExp(unary);
            String op= unaryExp.getUnaryOp().getLexeme();

            String temp= irBuilder.newTemp();
            switch (op){
                case "+":
                    return src;
                case "-":
                    irBuilder.emit("neg",src,null,temp);
                    return temp;
                case "!":
                    irBuilder.emit("not",src,null,temp);
                    return temp;
                default:
                    return null;
            }
        }

        return null;
    }

    private int evalUnaryExp(UnaryExp unaryExp){
        if (unaryExp.getPrimaryExp()!=null) {
            return evalPrimaryExp(unaryExp.getPrimaryExp());
        }
        if (unaryExp.getUnaryExp()!=null){
            int value = evalUnaryExp(unaryExp.getUnaryExp());
            String op = unaryExp.getUnaryOp().getLexeme();
            return
            switch (op){
                case "+" -> value;
                case "-" -> -value;
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };
        }
        throw new IllegalStateException();
    }


    private static class ArgPack{
        final boolean isAddr;
        final String operand;

        public ArgPack(boolean isAddr, String operand) {
            this.isAddr = isAddr;
            this.operand = operand;
        }
    }

    //函数实参表达式 FuncRParams → Exp { ',' Exp }
    private String genFuncRParams(FuncRParams funcRParams, String funcName){
        List<Exp> exps;
        if (funcRParams==null || funcRParams.getExps()==null){
            exps = List.of();
        } else {
            exps=funcRParams.getExps();
        }

        //收集实参结果
        List<ArgPack> argPacks = new ArrayList<>();

        for (Exp exp : exps){
            LVal wholeArray = extractPlainLVal(exp);
            if (wholeArray!=null && wholeArray.getIndex()==null){
                String name = wholeArray.getIdent().getLexeme();
                Symbol symbol = lookupSymbolVisible(name);
                if (symbol!=null && symbol.isArray()){
                    String irName = irNameOf(symbol);
                    argPacks.add(new ArgPack(true, irName));
                    continue;
                }
            }

            String value = genExp(exp);
            argPacks.add(new ArgPack(false,value));
        }

        for(ArgPack argPack: argPacks){
            if (argPack.isAddr){
                irBuilder.emit("param_addr", argPack.operand, null,null);
            } else {
                irBuilder.emit("param_val", argPack.operand,null,null);
            }
        }

        Symbol funcSym = lookupFunc(funcName);
        String size = Integer.toString(argPacks.size());
        if(funcSym!=null && funcSym.isVoidFunc()){
            irBuilder.emit("call",funcName,size,null);
            return null;
        } else {
            String ret =irBuilder.newTemp();
            irBuilder.emit("call",funcName,size,ret);
            return ret;
        }
    }

    private LVal extractPlainLVal(Exp exp) {
        if (exp == null) return null;
        if (!(exp instanceof AddExp addExp)) return null;
        if (!addExp.getOperators().isEmpty() || addExp.getMulExps().size()!=1) return null;

        Exp e1 = addExp.getMulExps().get(0);
        if (!(e1 instanceof MulExp mulExp)) return null;

        if (!mulExp.getOperators().isEmpty() || mulExp.getUnaryExps().size()!=1) return null;
        Exp e2 = mulExp.getUnaryExps().get(0);
        if (!(e2 instanceof UnaryExp unaryExp)) return null;

        if (unaryExp.getPrimaryExp()==null || unaryExp.getIdent()!=null || unaryExp.getUnaryExp()!=null) return null;
        PrimaryExp p = unaryExp.getPrimaryExp();

        // PrimaryExp -> LVal
        if (p.getLVal() != null && p.getExp() == null && p.getNumber() == null) {
            return p.getLVal();
        }

        // PrimaryExp -> '(' Exp ')'
        if (p.getExp() != null && p.getLVal() == null && p.getNumber() == null) {
            return extractPlainLVal(p.getExp());
        }
        return null;
    }

    //乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private String genMulExp(MulExp mulExp){
        if(mulExp==null) return null;
        List<Exp> factors = mulExp.getUnaryExps();
        List<Token> ops = mulExp.getOperators();

        String left = null;
        if(!factors.isEmpty() && factors.get(0)instanceof UnaryExp uExp){
            left = genUnaryExp(uExp);
        }

        for(int i=1; i<factors.size(); i++){
            UnaryExp unaryExp = (UnaryExp) factors.get(i);
            String right = genUnaryExp(unaryExp);
            String temp = irBuilder.newTemp();
            String op = ops.get(i-1).getLexeme();
            switch (op){
                case "*":
                    irBuilder.emit("mul", left, right, temp);
                    break;
                case "/":
                    irBuilder.emit("div", left, right, temp);
                    break;
                case "%":
                    irBuilder.emit("mod", left, right, temp);
                    break;
                default:
                    return null;
            }
            left = temp;
        }
        return left;
    }

    private int evalMulExp(MulExp mulExp){
        int val = evalUnaryExp((UnaryExp)mulExp.getUnaryExps().get(0));
        for (int i=1; i<mulExp.getUnaryExps().size();i++){
            int right = evalUnaryExp((UnaryExp)mulExp.getUnaryExps().get(i));
            String op = mulExp.getOperators().get(i-1).getLexeme();
            switch (op){
                case "*" -> val *= right;
                case "/" -> val /= right;
                case "%" -> val %= right;
                default -> {}
            }
        }
        return val;
    }

    //加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    private String genAddExp(AddExp addExp){
        if(addExp==null)  return null;
        List<Exp> terms = addExp.getMulExps();
        List<Token> ops = addExp.getOperators();

        String left = null;
        if(!terms.isEmpty() && terms.get(0)instanceof MulExp mExp){
            left = genMulExp(mExp);
        }

        for(int i = 1; i< terms.size(); i++){
            MulExp mulExp = (MulExp) terms.get(i);
            String right = genMulExp(mulExp);
            String temp = irBuilder.newTemp();
            String op = ops.get(i-1).getLexeme();
            switch (op){
                case "+":
                    irBuilder.emit("add", left, right, temp);
                    break;
                case "-":
                    irBuilder.emit("sub", left, right, temp);
                    break;
                default:
                    return null;
            }
            left = temp;
        }
        return left;
    }

    private int evalAddExp(AddExp addExp){
        int val = evalMulExp((MulExp)addExp.getMulExps().get(0));
        for (int i=1; i<addExp.getMulExps().size();i++){
            int right = evalMulExp((MulExp)addExp.getMulExps().get(i));
            String op = addExp.getOperators().get(i-1).getLexeme();
            val = op.equals("+") ? (val+right) : (val-right);
        }
        return val;
    }

    //关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private String genRelExp(RelExp relExp){
        if(relExp==null) return null;
        List<Exp> addExps = relExp.getAddExps();
        List<Token> ops =relExp.getOperators();
        if (addExps.isEmpty()) return null;

        String left = null;
        if(addExps.get(0) instanceof AddExp addE){
            left = genAddExp(addE);
        }
        if (ops.isEmpty()) return left;

        for(int i=1; i<addExps.size(); i++){
            AddExp addExp = (AddExp) addExps.get(i);
            String right = genAddExp(addExp);
            String temp = irBuilder.newTemp();
            String op = ops.get(i-1).getLexeme();
            String irOp = switch (op) {
                case "<" -> "lt";
                case "<=" -> "le";
                case ">" -> "gt";
                case ">=" -> "ge";
                default -> throw new IllegalStateException("Bad RelOp");
            };

            irBuilder.emit(irOp, left, right, temp);
            left=temp;
        }
        return left;
    }

    //相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private String genEqExp(EqExp eqExp){
        if(eqExp==null) return null;
        List<Exp> relExps = eqExp.getRelExps();
        List<Token> ops =eqExp.getOperators();
        if (relExps.isEmpty()) return null;

        String left = null;
        if(relExps.get(0) instanceof RelExp relE){
            left = genRelExp(relE);
        }
        if (ops.isEmpty()) return left;

        for(int i=1; i<relExps.size(); i++){
            RelExp relExp = (RelExp) relExps.get(i);
            String right = genRelExp(relExp);
            String temp = irBuilder.newTemp();
            String op = ops.get(i-1).getLexeme();
            String irOp = switch (op) {
                case "==" -> "eq";
                case "!=" -> "ne";
                default -> throw new IllegalStateException("Bad EqOp");
            };

            irBuilder.emit(irOp, left, right, temp);
            left=temp;
        }
        return left;
    }

    //逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
    private void genLAndExp(LAndExp lAndExp, String falseLabel){
        if (lAndExp==null) return;
        List<Exp> eqExps0 = lAndExp.getEqExps();
        if(eqExps0==null || eqExps0.isEmpty()) return;

        for(Exp exp : eqExps0){
            EqExp eqExp = (EqExp) exp;
            String val = genEqExp(eqExp);
            // val=0 → false
            irBuilder.emit("bez", val, null,falseLabel);
        }
    }

    //逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    private  void genLOrExp(LOrExp lOrExp, String falseLabel){
        if (lOrExp==null) return;
        List<Exp> ands0 = lOrExp.getLAndExps();
        if(ands0 ==null || ands0.isEmpty() )  return;

        List<LAndExp> ands=new ArrayList<>();
        for (Exp exp:ands0){
            ands.add((LAndExp) exp);
        }

        int n=ands.size();
        if (n==1){
            genLAndExp(ands.get(0),falseLabel);
            return;
        }
        String passLabel = irBuilder.newLabel("pass");

        for(int i=0; i<n; i++){
            LAndExp lAndExp = ands.get(i);
            if(i!=n-1){
                //false→下一个or
                String nextLabel = irBuilder.newLabel("next");
                genLAndExp(lAndExp,nextLabel);
                irBuilder.emit("j", null, null, passLabel);
                irBuilder.emit("label", null, null, nextLabel);

            } else {
                //最后一个or
                genLAndExp(lAndExp,falseLabel);
            }
        }
        irBuilder.emit("label", null, null, passLabel);
    }

    //常量表达式 ConstExp → AddExp
    private int evalConstExp(ConstExp constExp){
        return evalAddExp(constExp.getAddExp());
    }

}
