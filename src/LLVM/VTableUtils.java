package LLVM;

import java.util.Map;

import static java.util.Map.entry;

public class VTableUtils {

    private Map<String, Integer> typeToMemorySize = Map.ofEntries(
            entry("int", 8),
            entry("void", 4)
    );

    // Based only in the number of Fields
    public int getClassPhysicalSize(String classID){
        int size = 0;
        for (String fieldType : fields.keySet()) {
            size += typeToMemorySize.get(fieldType);
        }

        return size + 4;
    }
}
