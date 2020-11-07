package ast;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    private ClassTree getClassTreeByNameFromForest(List<ClassTree> forest, String name) {
        ClassDecl methodOwnerClass = astUtils.getClassFromName(name);
        ClassTree methodOwnerClassTree = null;
        for(ClassTree tree : forest) {
            methodOwnerClassTree = tree.getClassTree(methodOwnerClass);
            if (methodOwnerClassTree != null) {
                break;
            }
        }
        return methodOwnerClassTree;
    }

    private void initForest() {
        var tempForest = new LinkedList<ClassTree>();

        for (ClassDecl classDecl : program.classDecls()) {
            tempForest.add(new ClassTree(classDecl));
        }

        ClassTree tree;
        while (!tempForest.isEmpty()) {
            tree = tempForest.pop();
            String superName = tree.getClassDecl().superName();
            if (superName != null) {
                ClassTree parentTree = getClassTreeByNameFromForest(tempForest, superName);
                if (parentTree != null) {
                    // Set the tree as parent to the current tree
                    parentTree.addChild(tree);
                }
            } else {
                // When the tree doesn't have a place to be added, add it to our forest.
                trees.add(tree);
            }
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

        // Get the most upper parent we can find with this method in, until we reach to the root of the tree
        if (methodDecl != null && methodOwnerClassTree != null) {
            while (methodOwnerClassTree.getParent() != null) {
                var parent = methodOwnerClassTree.getParent();
                if (parent.getClassDecl().hasMethod(methodDecl.name())) {
                    methodOwnerClassTree = parent;
                }
            }
        }


        return methodOwnerClassTree;
    }

    @Override
    public boolean isSubclassOf(String childClassName, ClassDecl parentClassName) {
        return false;
    }

    public ClassTree findClassTree(ClassDecl classDecl){
        for (ClassTree tree : trees){
            ClassTree find = tree.getClassTree(classDecl);
            if(find != null)
                return find;
        }
        return null;
    }
}
