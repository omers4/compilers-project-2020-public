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

    @Override
    public void renameMethodNameInSubtree(String oldName, String newName) {

    }
}