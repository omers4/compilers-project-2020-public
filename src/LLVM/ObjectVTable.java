package LLVM;

import ast.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class ObjectVTable {

    private String Id;
    private List<String> fields;

    // TODO: Better to make it more abstract and not connect it directly to LLVM
    private LinkedHashMap<String, MethodSignature> methods;

    public ObjectVTable(String id) {

        this.Id = id;

        // Order is important to know later where each field resides in memory;
        fields = new ArrayList<>();
        methods = new LinkedHashMap<>();
    }

    public void addField(String id) {
        fields.add(id);
    }

    public void addOrUpdateMethod(String id, String className, MethodDecl methodDecl) {

        MethodSignature methodSignature = new MethodSignature("@"+className+"."+id, methodDecl.returnType(), methodDecl.formals());

        // TODO: Check when overiding that order preserves
        this.methods.put(id,methodSignature);
    }

    public List<MethodSignature> getMethods() {
        return new ArrayList<>(this.methods.values());
    }

    public List<String> getFields() {
        return this.fields;
    }
}
