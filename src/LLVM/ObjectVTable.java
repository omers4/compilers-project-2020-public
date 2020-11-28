package LLVM;

import ast.AstType;
import ast.IntAstType;
import ast.RefType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class ObjectVTable {

    private String Id;
    private Map<String,String> fields;
    private List<String> methods;


    public ObjectVTable() {

        // Order is important to know later where each field resides in memory;
        fields = new LinkedHashMap<>();
    }

    public void addField(AstType type, String name) {
        fields.put(type,name);
    }

}
