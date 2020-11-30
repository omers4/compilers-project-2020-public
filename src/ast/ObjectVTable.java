package ast;

import ast.*;

import java.sql.Ref;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class ObjectVTable {

    private String Id;
    private LinkedHashMap<String, AstType> fields;
    private LinkedHashMap<String, MethodSignature> methods;

    public ObjectVTable(String id) {

        this.Id = id;

        // Order is important to know later where each field resides in memory;
        fields = new LinkedHashMap<>();
        methods = new LinkedHashMap<>();
    }

    public void addField(String id, AstType type) {
        fields.put(id,type);
    }

    public void addOrUpdateMethod(String id, String className, MethodDecl methodDecl) {

        MethodSignature methodSignature = new MethodSignature("@"+className+"."+id, methodDecl.returnType(), methodDecl.formals());

        // TODO: Check when overiding that order preserves
        this.methods.put(id,methodSignature);
    }

    public void addOrUpdateMethod(MethodSignature methodSignature) {

        // TODO: Check when overiding that order preserves
        this.methods.put(methodSignature.getName(),methodSignature);
    }

    public List<MethodSignature> getMethods() {
        return new ArrayList<>(this.methods.values());
    }

    public LinkedHashMap<String, AstType> getFields() {
        return this.fields;
    }

    public int getClassPhysicalSize() {
        return 2;
    }

}
