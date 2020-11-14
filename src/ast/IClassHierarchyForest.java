package ast;

/*
 * This class represents the hierarchy of the classes of the program.
 * the hierarchy is actualy a forest of classes.
 * */
public interface IClassHierarchyForest {
    ClassTree getHighestClassTreeByMethod(int lineNumber);
    ClassTree findClassTree(ClassDecl classDecl);
}
