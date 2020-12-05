package ast;

public class SymbolItemKey {

    private final String name;
    private final SymbolType type;

    public SymbolItemKey(String name, SymbolType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return this.name;}
    public SymbolType getType() { return this.type;}

    @Override
    public boolean equals(Object o) {
        if(this == o)  return true;
        if(o == null || getClass() != o.getClass()) return false;
        SymbolItemKey item = (SymbolItemKey) o;
        if(!this.name.equals(item.name) || this.type != item.type) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.type.hashCode();
    }
}
