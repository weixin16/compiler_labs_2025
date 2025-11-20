package frontend.symbol;

import java.util.List;

public class Symbol {
    private final String name;
    private final SymbolType type;
    private final int scopeId;
    private final int lineNum;
    private final boolean isParam;
    private final Integer array;
    private final List<SymbolType> paramTypes;

    private Symbol(String name, SymbolType type, int scopeId, int lineNum, boolean isParam, Integer arrayLen, List<SymbolType> paramTypes) {
        this.name = name;
        this.type = type;
        this.scopeId = scopeId;
        this.lineNum = lineNum;
        this.isParam = isParam;
        this.array = arrayLen;
        if(paramTypes == null) {
            this.paramTypes = List.of();
        } else {
            this.paramTypes=List.copyOf(paramTypes);
        }
    }

    // 变量
    public Symbol(String name, SymbolType type, int scopeId, int lineNum) {
        this(name, type, scopeId, lineNum, false, null,List.of());
    }

    // 形参
    public Symbol(String name, SymbolType type, int scopeId, int lineNum, boolean isParam) {
        this(name, type, scopeId, lineNum, isParam, null,List.of());
    }

    // 函数
    public Symbol(String name, SymbolType type, int scopeId, int lineNum, List<SymbolType> paramTypes) {
        this(name, type, scopeId, lineNum, false, null,paramTypes);
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getLineNum() {
        return lineNum;
    }

    public boolean isParam() {
        return isParam;
    }

    public Integer getArray() {
        return array;
    }

    public List<SymbolType> getParamTypes() {
        return paramTypes;
    }

    public boolean isArray(){
        return type==SymbolType.IntArray || type==SymbolType.ConstIntArray || type==SymbolType.StaticIntArray;
    }

    public boolean isConstArray(){
        return type==SymbolType.ConstIntArray;
    }

    public boolean isConst(){
        return type==SymbolType.ConstIntArray || type==SymbolType.ConstInt;
    }

    public boolean isFunc(){
        return type==SymbolType.VoidFunc || type==SymbolType.IntFunc;
    }

}
