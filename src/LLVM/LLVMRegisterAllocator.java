package LLVM;

import ast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class LLVMRegisterAllocator implements ILLVMRegisterAllocator {

    private IAstToSymbolTable _astToSymbolTable;
    private int _counter;
    private Map<String,String> _classIDToVTable;

    public LLVMRegisterAllocator(IAstToSymbolTable symbolTable) {
        _astToSymbolTable = symbolTable;
        _classIDToVTable = new HashMap<>();
        _counter = 0;
    }

    @Override
    public String allocateAddressRegister(String name, SymbolType type, AstNode node) {
        SymbolTable symbolTable = _astToSymbolTable.getSymbolTable(node);
        try {
            SymbolTableItem item = symbolTable.get(new SymbolItemKey(name, type));
            if (null == item.getRegisterId()) {
                item.setRegisterId("%" + name);
            }

            return item.getRegisterId();

        }
        catch (NoSuchElementException e) {
            return "Error, variable does not exist";
        }
    }

    @Override
    public String allocateNewTempRegister() {
        String result = "%_" + String.valueOf(_counter);
        _counter++;
        return result;
    }

    @Override
    public String allocateVTableRegister(String classID) {
        String register = _classIDToVTable.get(classID);
        if (null != register)
            return register;

        String newRegisterID = "@." + classID + "_vtable";
        _classIDToVTable.put(classID, newRegisterID);
        return newRegisterID;
    }

    public void resetCounter() {
        _counter = 0;
    }
}
