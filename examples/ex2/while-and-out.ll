declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
    %_str = bitcast [4 x i8]* @_cint to i8*
    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
    ret void
}

define void @throw_oob() {
    %_str = bitcast [15 x i8]* @_cOOB to i8*
    call i32 (i8*, ...) @printf(i8* %_str)
    call void @exit(i32 1)
    ret void
}

0: 
%_0 = and i1 1, 1 
3: 
br i1 %_0, label 4, label 6 
4: 
%_1 = and i1 0, 0 
br label %5 
5: 
br label %6 
6: 
%_2 = phi i32 [0, %3], [%_1, %5] 
br i1 %_2, label 1, label 2 
1: 
%_3 = sub i32 100, 50 
br label %0 
2: 
