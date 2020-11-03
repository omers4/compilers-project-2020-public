package ast;

import java.util.List;

public interface IClassTree {
    public void addChild(ClassTree child);
    public void addChild(ClassDecl data);
    public void addChildren(List<ClassTree> children);
    public List<ClassTree> getChildren();
    public ClassDecl getClassDecl();
    public void setClassDecl(ClassDecl data);
    public ClassTree getParent();

    // The target of this function if to get the tree node that contains the class declaration
    public ClassTree getClassTree(ClassDecl classDecl);

    // Giving a tree of classes, we would like to rename all signatures in inheriting classes
    void renameMethodNameInSubtree(String oldName, String newName);
}
