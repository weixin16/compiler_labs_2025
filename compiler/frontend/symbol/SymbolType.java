package frontend.symbol;

public enum SymbolType {
    ConstInt,
    ConstIntArray,
    StaticInt,
    Int,
    IntArray,
    StaticIntArray,
    VoidFunc,
    IntFunc;

    public static SymbolType VarType(boolean isConst, boolean isArray, boolean isStatic) {
        if (isConst) {
            if(isArray){
                return ConstIntArray;
            } else {
                return ConstInt;
            }
        }

        if(isStatic){
            if (isArray){
                return StaticIntArray;
            } else {
                return StaticInt;
            }
        }

        if(isArray){
            return IntArray;
        } else {
            return Int;
        }
    }

    public static SymbolType FuncType(boolean returnInt){
        if (returnInt){
            return IntFunc;
        }else {
            return VoidFunc;
        }
    }

}
