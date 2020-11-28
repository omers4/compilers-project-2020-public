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

define i32 @main() 
	%_1 = call i32 @null() 
	call void @print_int(i32 %_1) 
	ret i32 0 
}

define i32 @Simple.bar(i8* %this) 
	%_x2 = alloca i32* 
	%_3 = icmp slt i32 2, 0 
	br i1 %_3, label 0, label 1 
0: 
	call void @throw_oob() 
	br label %1 
1: 
	%_4 = add i32 2, 1 
	%_5 = call i8* @calloc(i32 4, i32 %_4) 
	%_6 = bitcast i8* %_5 to i32* 
	store i32 2, i32* %_6 
	store i32* %_6, i32** %_x2 
	%_7 = load i32*, i32** %_x2 
	%_8 = icmp slt i32 0, 0 
	br i1 %_8, label 2, label 3 
2: 
	call void @throw_oob() 
	br label %3 
3: 
	%_9 = getelementptr i32, i32* %_7, i32 0
	%_10 = load i32, i32* %_9 
	%_11 = icmp sle i32 %_10, 0 
	br i1 %_11, label 4, label 5 
4: 
	call void @throw_oob() 
	br label %5 
5: 
	%_12 = add i32 0, 1 
	%_13 = getelementptr i32, i32* %_7, i32 %_12
	store i32 1, i32* %_13 
	%_14 = load i32*, i32** %_x2 
	%_15 = icmp slt i32 1, 0 
	br i1 %_15, label 6, label 7 
6: 
	call void @throw_oob() 
	br label %7 
7: 
	%_16 = getelementptr i32, i32* %_14, i32 0
	%_17 = load i32, i32* %_16 
	%_18 = icmp sle i32 %_17, 1 
	br i1 %_18, label 8, label 9 
8: 
	call void @throw_oob() 
	br label %9 
9: 
	%_19 = add i32 1, 1 
	%_20 = getelementptr i32, i32* %_14, i32 %_19
	store i32 2, i32* %_20 
	%_21 = load i32*, i32** %_x2 
	%_22 = load i32*, i32** %_21 
	%_23 = icmp slt i32 0, 0 
	br i1 %_23, label 10, label 11 
10: 
	call void @throw_oob() 
	br label %11 
11: 
	%_24 = getelementptr i32, i32* %_22, i32 0
	%_25 = load i32, i32* %_24 
	%_26 = icmp sle i32 %_25, 0 
	br i1 %_26, label 12, label 13 
12: 
	call void @throw_oob() 
	br label %13 
13: 
	%_27 = add i32 0, 1 
	%_28 = getelementptr i32, i32* %_22, i32 %_27
	%_29 = load i32, i32* %_28 
	%_30 = load i32*, i32** %_x2 
	%_31 = load i32*, i32** %_30 
	%_32 = icmp slt i32 1, 0 
	br i1 %_32, label 14, label 15 
14: 
	call void @throw_oob() 
	br label %15 
15: 
	%_33 = getelementptr i32, i32* %_31, i32 0
	%_34 = load i32, i32* %_33 
	%_35 = icmp sle i32 %_34, 1 
	br i1 %_35, label 16, label 17 
16: 
	call void @throw_oob() 
	br label %17 
17: 
	%_36 = add i32 1, 1 
	%_37 = getelementptr i32, i32* %_31, i32 %_36
	%_38 = load i32, i32* %_37 
	%_39 = add i32 %_29, %_38 
	call void @print_int(i32 %_39) 
	ret i32 0 
}

