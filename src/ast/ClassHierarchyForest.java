package ast;
import Semantics.InvalidSemanticsException;

import java.util.*;

public class ClassHierarchyForest implements IClassHierarchyForest {
    private Program program;
    private ASTUtils astUtils;
    private ArrayList<ClassTree> trees;

    public ClassHierarchyForest(Program program) throws InvalidSemanticsException {
        this.program = program;
        this.astUtils = new ASTUtils(program);
        trees = new ArrayList<ClassTree>();
        initForest();
    }

    /*
    * This method receives a forest and a name of a class,
    * and returns the subtree in this forest that matches the class.
    * */
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

    public ClassDecl getClassDeclByName(String name) {
        return this.getClassTreeByNameFromForest(this.trees, name).getClassDecl();
    }

    /*
     * The target of this method is to initialize the forest by going over all
     * classes and link them according to the inheritance.
     * */
    private void initForest() throws InvalidSemanticsException {
        var tempForest = new LinkedList<ClassTree>();
        List<String> classNames = new ArrayList<>();

        String mainClassName = program.mainClass().name();

        for (ClassDecl classDecl : program.classDecls()) {
            if (classDecl.name().equals(mainClassName) || classNames.contains(classDecl.name())) {
                throw new InvalidSemanticsException(); // Refrain from duplicated class names
            }
            tempForest.add(new ClassTree(classDecl));
            classNames.add(classDecl.name());
        }

        ClassTree tree;
        while (!tempForest.isEmpty()) {
            tree = tempForest.pop();
            String superName = tree.getClassDecl().superName();
            if (superName != null) {
                if (superName.equals(mainClassName) || !classNames.contains(superName) || superName.equals(tree.getClassDecl().name()) ||
                        classNames.indexOf(tree.getClassDecl().name()) < classNames.indexOf(superName)) {
                    throw new InvalidSemanticsException(); // Cyclic, bad or self inheritance
                }

                ClassTree parentTree = getClassTreeByNameFromForest(tempForest, superName);
                if (parentTree != null) {
                    // Set the tree as parent to the current tree
                    parentTree.addChild(tree);
                } else {
                    ClassTree parentTreeFromResolved = getClassTreeByNameFromForest(trees, superName);
                    if (parentTreeFromResolved != null) {
                        // Set the tree as parent to the current tree
                        parentTreeFromResolved.addChild(tree);
                    }
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
        ClassTree highestClassTreeWithMethod = methodOwnerClassTree;
        if (methodDecl != null && methodOwnerClassTree != null) {
            while (methodOwnerClassTree.getParent() != null) {
                var parent = methodOwnerClassTree.getParent();
                if (parent.getClassDecl().hasMethod(methodDecl.name())) {
                    highestClassTreeWithMethod = parent;
                }
                methodOwnerClassTree = parent;
            }
        }


        return highestClassTreeWithMethod;
    }

    public ClassTree findClassTree(ClassDecl classDecl){
        for (ClassTree tree : trees){
            ClassTree find = tree.getClassTree(classDecl);
            if(find != null)
                return find;
        }
        return null;
    }

    public boolean isParent(String parent, String child) {
        var cltree = getClassTreeByNameFromForest(this.trees, child);
        if (parent.equals(child)) {
            return true;
        }

        while (cltree.getParent() != null) {
            cltree = cltree.getParent();
            if (cltree.getClassDecl().name().equals(parent)) {
                return true;
            }
        }
        return false;
    }

    public void getTreesNames(List<String> names) {
        List<ClassTree> all_trees = new ArrayList<>();
        for (ClassTree tree : trees){
            all_trees.add(tree);
            tree.getFamilyList(all_trees);
        }

        for (ClassTree tree : all_trees){
            names.add(tree.getData().name());
        }
    }

}
