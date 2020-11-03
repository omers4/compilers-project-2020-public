package ast;

import java.util.ArrayList;

public class ClassHierarchyForest implements IClassHierarchyForest {
    private Program program;
    private ArrayList<ClassTree> trees;

    public ClassHierarchyForest(Program program) {
        this.program = program;
        trees = new ArrayList<ClassTree>();
        initForest();
    }

    private void initForest() {

    }

    @Override
    public ClassTree getHighestClassTreeByMethod(int lineNumber) {
        return null;
    }

    @Override
    public boolean isSubclassOf(String childClassName, ClassDecl parentClassName) {
        return false;
    }
}
