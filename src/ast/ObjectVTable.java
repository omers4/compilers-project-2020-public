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

    public int getMethodIndex(String method) {
        MethodSignature methodSignature = methods.get(method);
return 1;
    }

    public void addField(String id, AstType type) {
        fields.put(id,type);
    }

    public void addOrUpdateMethod(String id, String className, MethodDecl methodDecl) {
        MethodSignature methodSignature = new MethodSignature("@"+className+"."+id, methodDecl.returnType(), methodDecl.formals());
        this.methods.put(id,methodSignature);
    }

    public void addOrUpdateMethod(String id, MethodSignature methodSignature) {
        this.methods.put(id,methodSignature);
    }

    public LinkedHashMap<String, MethodSignature> getMethods() {
        return this.methods;
    }

    public LinkedHashMap<String, AstType> getFields() {
        return this.fields;
    }
}
