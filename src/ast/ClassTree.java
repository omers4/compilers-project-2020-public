package ast;

import java.util.ArrayList;
import java.util.List;

public class ClassTree implements IClassTree {

    private ClassDecl data = null;
    private List<ClassTree> children = new ArrayList<>();
    private ClassTree parent = null;

    public ClassTree(ClassDecl data) {
        this.data = data;
    }

    public void addChild(ClassTree child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(ClassDecl data) {
        ClassTree newChild = new ClassTree(data);
        this.addChild(newChild);
    }

    public void addChildren(List<ClassTree> children) {
        for(ClassTree t : children) {
            t.setParent(this);
        }
        this.children.addAll(children);
    }

    public List<ClassTree> getChildren() {
        return children;
    }

    public ClassDecl getClassDecl() {
        return data;
    }

    public void setClassDecl(ClassDecl data) {
        this.data = data;
    }

    private void setParent(ClassTree parent) {
        this.parent = parent;
    }

    public ClassTree getParent() {
        return parent;
    }

    public ClassDecl getData(){return data;}

    public ClassTree getClassTree(ClassDecl classDecl) {
        if (data == classDecl) {
            return this;
        }
        for (ClassTree child: children) {
            ClassTree childSearch = child.getClassTree(classDecl);
            if (childSearch != null) {
                return childSearch;
            }
        }
        return null;
    }


    public void getFamilyList(List<ClassTree> familyList) {
        List<ClassTree> children = this.getChildren();
        if (children != null) {
            familyList.addAll(children);
            for (ClassTree child : children) {
                child.getFamilyList(familyList);
            }
        }
    }

    public boolean isNameInFamily(List<ClassTree> familyList, String name) {
        for (ClassTree tree : familyList) {
            if (tree.data.name().equals(name)) {
                return true;
            }
        }
        return false;
    }


}