package LLVM;

import ast.*;

import java.util.Map;
import java.util.NoSuchElementException;

public class LLVMRegisterAllocator implements ILLVMRegisterAllocator {

    private IAstToSymbolTable _astToSymbolTable;
    private int _counter;
    private Map<String,String> _classIDToVTable;

    public LLVMRegisterAllocator(IAstToSymbolTable symbolTable) {
        _astToSymbolTable = symbolTable;
        _counter = 0;
    }

    @Override
    public String allocateAddressRegister(String name, AstNode node) {
        SymbolTable symbolTable = _astToSymbolTable.getSymbolTable(node);
        try {
            SymbolTableItem item = symbolTable.get(name);
            if (null == item.getRegisterId()) {
                ++_counter;
                item.setRegisterId("%_" + name + String.valueOf(_counter));
            }

            return item.getRegisterId();

        }
        catch (NoSuchElementException e) {
            return "Error, variable does not exist";
        }
    }

    @Override
    public String allocateNewTempRegister() {
        ++_counter;
        return "%_" + String.valueOf(_counter);
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
}
