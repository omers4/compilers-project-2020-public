package ast;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Map.entry;

public class ClassInfo {

    private Map<String, ObjectVTable> classesToInfo = new HashMap<>();

    Map<Class, Integer> astTypeToSize = Map.ofEntries(
            entry(BoolAstType.class, 1),
            entry(IntArrayAstType.class, 8),
            entry(IntAstType.class, 4),
            entry(RefType.class, 8)
    );

    public void addClassInfo(String className, ObjectVTable info) {
        classesToInfo.put(className, info);
    }

    public ObjectVTable getClassVTable(String classId) {
        if (!this.classesToInfo.containsKey(classId))
            return null;
        return this.classesToInfo.get(classId);
    }

    public int getClassPhysicalSize(String classId) {
        ObjectVTable classInfo = classesToInfo.get(classId);
        int size = 0;
        for (var fieldType : classInfo.getFields().values()) {
            size += astTypeToSize.get(fieldType.getClass());
        }

        return size + 8;
    }
}
