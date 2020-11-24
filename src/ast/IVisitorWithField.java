package ast;

public interface IVisitorWithField<T> extends Visitor{

    public T getField();
}
