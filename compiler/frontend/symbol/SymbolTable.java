package frontend.symbol;

import java.util.*;

public class SymbolTable {
    private final int id;
    private final SymbolTable father;
    private final List<SymbolTable> son =new ArrayList<>();
    private final Map<String,Symbol> symbols = new LinkedHashMap<>();

    public SymbolTable(int id, SymbolTable father) {
        this.id = id;
        this.father = father;
    }

    public int getId() {
        return id;
    }

    public SymbolTable getFather() {
        return father;
    }

    public List<SymbolTable> getSon() {
        return son;
    }

    public Collection<Symbol> getSymbols() {
        return symbols.values();
    }

    public boolean define(Symbol symbol) {
        String name = symbol.getName();
        if(symbols.containsKey(name)){
            return false;
        }
        symbols.put(name,symbol);
        return true;
    }

    public Symbol lookupLocal(String name){
        return symbols.get(name);
    }
}
