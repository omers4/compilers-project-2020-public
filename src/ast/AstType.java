package ast;

public abstract class AstType extends AstNode {

    public AstType() {
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)  return true;
        if(obj == null) return false;
        return obj.getClass() == this.getClass();
    }
}
