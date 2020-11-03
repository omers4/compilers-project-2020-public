package ast;

public interface IClassHierarchyForest {
    public ClassTree getHighestClassTreeByMethod(int lineNumber);
    public boolean isSubclassOf(String childClassName, ClassDecl parentClassName);
}
