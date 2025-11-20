package frontend.symbol;

import java.util.ArrayList;
import java.util.List;

public class SymbolManager {
    private final List<SymbolTable> allSymbolTables = new ArrayList<>();
    private SymbolTable global;
    private SymbolTable current;
    private int nextId = 1;

    public SymbolManager(){
        reset();
    }

    public void reset(){
        allSymbolTables.clear();
        nextId=1;
        global = new SymbolTable(nextId++,null);
        current = global;
        allSymbolTables.add(global);
    }

    public SymbolTable getGlobalScope() {
        return global;
    }

    public SymbolTable getCurrentScope() {
        return current;
    }

    public void pushScope(){
        SymbolTable child = new SymbolTable(nextId++, current);
        allSymbolTables.add(child);
        current.getSon().add(child);
        current = child;
    }

    public void popScope(){
        if (current.getFather() != null){
            current = current.getFather();
        }
    }

    public boolean define(Symbol symbol){
        return  current.define(symbol);
    }

    public Symbol lookup(String name){
        SymbolTable symbolTable = current;
        while (symbolTable!=null){
            Symbol symbol = symbolTable.lookupLocal(name);
            if (symbol!=null){
                return symbol;
            }
            symbolTable = symbolTable.getFather();
        }
        return null;
    }

    public List<Symbol> getAllSymbols() {
        List<Symbol> result = new ArrayList<>();
        traverse(global,result);
        return result;
    }

    private void traverse(SymbolTable global, List<Symbol> result){
        result.addAll(global.getSymbols());
        for(SymbolTable son: global.getSon()){
            traverse(son,result);
        }
    }

}
