package LLVM;

import ast.*;

import java.util.NoSuchElementException;

public class LLVMRegisterAllocator implements ILLVMRegisterAllocator {

    private IAstToSymbolTable _astToSymbolTable;
    private int _counter;

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
}
