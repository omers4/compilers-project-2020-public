class Main {
	public static void main(String[] args) {
		System.out.println((new Example()).run());
	}
}

class Example {
	int x;

	public Example classRet(Example m, int k) {
		int x;
		x = 1;
		return m;
	}

	public int run() {
		x = 0;
		return (x) + (x);
	}

	public int other(int y) {
		int x;
		x = 1;
		return (x) - (1);
	}

}

class B extends Example {
	public Example classRet(C m, int k) {
		return m;
	}

	public int run() {
		x = 2;
		return (x) + (x);
	}

	public int test() {
		x = 4;
		return (x) + (x);
	}

}

class C extends B {
	public int run() {
		x = 2;
		return (x) + (x);
	}

	public int C_Test() {
		x = 7;
		return (x) + (x);
	}

}

class D extends C {
	public int D_Test() {
		x = 23;
		return (x) + (x);
	}

}

class E {
	int x;

	public int other() {
		x = 7;
		return (x) + (x);
	}

}

