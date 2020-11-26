%_0 = and i1 1, 1 
0: 
br i1 %_0, label 1, label 3 
1: 
%_1 = and i1 0, 0 
br label %2 
2: 
br label %3 
3: 
%_2 = phi i32 [0, %0], [%_1, %2] 
