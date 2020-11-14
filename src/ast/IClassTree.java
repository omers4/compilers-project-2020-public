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

    public ClassDecl getData();

    // The target of this function if to get the tree node that contains the class declaration
    public ClassTree getClassTree(ClassDecl classDecl);

    // add to familyList all this children (and their children etc.). without this (ClassTree which first called).
    public void getFamilyList(List<ClassTree> familyList);

    // return true if class name is in familyList
    public boolean isNameInFamily(List<ClassTree> familyList, String name);
}
