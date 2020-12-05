package LLVM;

import ast.AstNode;
import ast.SymbolType;

import java.nio.file.attribute.UserDefinedFileAttributeView;

public interface ILLVMRegisterAllocator {

    /* convert int x=7 to %x = alloca i 32
     * Responsible only for retrieving the register which the value resides in memory
     * Call examples - declaring a variable and assigning/using a variable
     * The node is needed for context. In order to infer the scope and context of the name
     * -> allocateNewRegister("x", (AssignmentExpr) assign) */
    String allocateAddressRegister(String name, SymbolType type, AstNode node);

    /* used in complex expressions such as x = y + z
    * Call examples - saving temporary results of expressions
    * for example %5 = add nsw i 32 3 , 4 */
    String allocateNewTempRegister();


    String allocateVTableRegister(String classID);

}
