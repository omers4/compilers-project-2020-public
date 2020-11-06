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

    // create a list with this all children (and their children etc.). without the first this(ClassTree which called).
    public List<ClassTree> getFamilyList(List<ClassTree> familyList);

    // return true if class name is in this.getFamilyList
    public boolean isNameInFamily(List<ClassTree> familyList, String name);
}
