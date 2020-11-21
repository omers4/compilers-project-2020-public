package LLVM;

import java.util.List;

public interface LLVMCommandFormatter {

    /* declare i32 @printf(i8*, ...)
     * -> formatExternalMethodDeclaration(LLVMType.Int, "printf", "i8*, ...") */
    String formatExternalMethodDeclaration(LLVMType retType, String methodName, String params);

    /* define i32 @BBS.Start(i8* %this, i32 %.sz)
     * -> formatMethodDefinition(LLVMType.Int, "rv")*/
    String formatMethodDefinition();

    /* ret i32 %rv
     * -> formatReturn(LLVMType.Int, "rv") */
    String formatReturn(LLVMType retType, String register);

    /* %ptr = alloca i32
     * -> formatAlloca("ptr", LLVMType.Int) */
    String formatAlloca(String register, LLVMType type);

    /* store i32 %.sz, i32* %sz
     * -> formatStore(LLVMType.Int, ".sz", LLVMType.Int, "sz") */
    String formatStore(LLVMType sourceType, String sourceRegister, LLVMType destType, String destRegister);

    /* %_31 = load i32, i32* %_30
     * -> formatLoad("_31", LLVMType.Int, "_30") */
    String formatLoad(String register, LLVMType valueType, String sourcePointer);

    /* %result = call i8* @calloc(i32 1, i32 %val)
     * -> formatCall("result", LLVMType.IntPointer, "calloc",
     * new List(){new LLVMMethodParam(LLVMType.Int, "1"), new LLVMMethodParam(LLVMType.Int, "%val")}) */
    String formatCall(String register, LLVMType retType, List<LLVMMethodParam> params);

    /* %_%sum = add i32 %a, %b
     * -> formatAdd("_%sum", LLVMType.Int, "a", "b")
     %_%sum = add i32 4, %var
     * -> formatAdd("_%sum", LLVMType.Int, "4", "var")
     * Same for and, sub, mul, xor
     * */
    String formatAdd(String register, LLVMType resultType, String first, String second);
    String formatAnd(String register, LLVMType resultType, String first, String second);
    String formatSub(String register, LLVMType resultType, String first, String second);
    String formatMul(String register, LLVMType resultType, String first, String second);
    String formatXOR(String register, LLVMType resultType, String first, String second);

    /* %case = icmp slt i32 %a, %b
     * -> formatCompare("case", ComparisonType.LessOrEquals, LLVMType.Int, "a", "b") */
    String formatCompare(String register, ComparisonType compareType, LLVMType variableType,
                         String register1, String register2);

    /* br i1 %case, label %if, label %else
    * -> formatConditionalBreak("case", "if", "else")
    * */
    String formatConditionalBreak(String booleanRegister, String ifLabel, String elseLabel);

    /* br label %goto
     * -> formatBreak("goto") */
    String formatBreak(String label);

    /* label123:
     * -> formatLabelName("label123") */
    String formatLabelName(String labelName);

    /* %ptr = bitcast i32* %ptr2 to i8**
     * -> formatLabelName("ptr", LLVMType.IntPointer, "ptr2", LLVMType.IntPointer2) */
    String formatBitcast(String register, LLVMType fromType, String fromRegister, LLVMType toType);

    /* %ptr_idx = getelementptr i8, i8* %ptr, i32 %idx
     * -> formatGetElementPtr("ptr_idx", LLVMType.Byte, "idx") */
    String formatGetElementPtr(String register, LLVMType type, String pointerRegister, int index);

    /* @.str = constant [12 x i8] c"Hello world\00"
    * -> formatConstant(".str", "Hello world\00") */
    String formatConstant(String register, String constantValue);

    /* TODO when we learn on vtables
    @.vtable = global [2 x i8*] [i8* bitcast (i32 ()* @func1 to i8*), i8* bitcast (i8* (i32, i32*)* @func2 to i8*)]
     * -> formatConstant("i8* bitcast (i32 ()* @func1 to i8*)", "i8* bitcast (i8* (i32, i32*)* @func2 to i8*)")*/
    String formatGlobalVTable(List<String> table);

    /* %c = phi i32 [%a, %lb1], [%b, %lb2]
     * -> formatPhi("c", "%a", "lb1", "%b", "lb2") */
    String formatPhi(String register,
                     String valueIfLabel1, String label1,
                     String valueIfLabel2, String label2);
}