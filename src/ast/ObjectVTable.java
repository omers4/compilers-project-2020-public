package ast;

import ast.*;

import java.sql.Ref;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class ObjectVTable {

    Map<Class, Integer> astTypeToSize = Map.ofEntries(
            entry(BoolAstType.class, 1),
            entry(IntArrayAstType.class, 8),
            entry(IntAstType.class, 4),
            entry(RefType.class, 8)
    );

    private String Id;
    private LinkedHashMap<String, AstType> fields;
    private LinkedHashMap<String, MethodSignature> methods;
    private String superName;

    public ObjectVTable(String id, String superName) {

        this.Id = id;

        // Order is important to know later where each field resides in memory;
        fields = new LinkedHashMap<>();
        methods = new LinkedHashMap<>();
        this.superName = superName;
    }

    public int getFieldIndex(String field) {
        int size = 0;
        for (var entry : fields.entrySet()) {
            if (entry.getKey().equals(field))
                return size + 8;

            size += astTypeToSize.get(entry.getValue().getClass());
        }

        return -1;
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

    public String superName() {return this.superName;}
}
