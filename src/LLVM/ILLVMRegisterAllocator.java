package LLVM;

import ast.AstNode;

import java.nio.file.attribute.UserDefinedFileAttributeView;

public interface ILLVMRegisterAllocator {

    /* convert int x=7 to %x = alloca i 32
     * Responsible only for retrieving the register which the value resides in memory
     * Call examples - declaring a variable and assigning/using a variable
     * -> allocateNewRegister("x") */
    String allocateAddressRegister(String name);

    /* used in complex expressions such as x = y + z
    * Call examples - saving temporary results of expressions
    * for example %5 = add nsw i 32 3 , 4 */
    String allocateNewTempRegister();

}
