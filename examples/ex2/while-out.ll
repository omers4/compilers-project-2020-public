0: 
%_0 = icmp slt i1 1, 2 
br i1 %_0, label 1, label 2 
1: 
%_1 = sub i32 100, 50 
br label %0 
2: 
