package ast;

import java.util.ArrayList;

public class ClassHierarchyForest implements IClassHierarchyForest {
    private Program program;
    private ASTUtils astUtils;
    private ArrayList<ClassTree> trees;

    public ClassHierarchyForest(Program program) {
        this.program = program;
        this.astUtils = new ASTUtils(program);
        trees = new ArrayList<ClassTree>();
        initForest();
    }

    private void initForest() {
        for (ClassDecl classDecl : program.classDecls()) {
            // TODO parse the inheritance relationships. for now put it all in a forest
            trees.add(new ClassTree(classDecl));
        }
    }

    @Override
    public ClassTree getHighestClassTreeByMethod(int lineNumber) {
        MethodDecl methodDecl = astUtils.getMethodFromLineNumber(lineNumber);
        ClassDecl methodOwnerClass = astUtils.getClassFromLineNumber(lineNumber);
        ClassTree methodOwnerClassTree = null;

        // Get the relevant tree object
        for(ClassTree tree : trees) {
            methodOwnerClassTree = tree.getClassTree(methodOwnerClass);
            if (methodOwnerClassTree != null) {
                break;
            }
        }

        // Get the most upper parent we can find with this method in it
        if (methodDecl != null && methodOwnerClassTree != null) {
            while (methodOwnerClassTree.getParent() != null) {
                var parent = methodOwnerClassTree.getParent();
                if (parent.getClassDecl().hasMethod(methodDecl.name())) {
                    methodOwnerClassTree = parent;
                }
            }
            return methodOwnerClassTree;
        }


        return null;
    }

    @Override
    public boolean isSubclassOf(String childClassName, ClassDecl parentClassName) {
        return false;
    }
}
