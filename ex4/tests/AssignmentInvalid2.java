class Main {
	public static void main(String[] a) {
		System.out.println((new A()).ret());
	}
}

class A {
	public A test(int a, boolean b) {
		return this;
	}

	public int ret() {
		C c;
		c = new A();
		return 4;
	}

}

class B extends A {
	public C test(int a, boolean b) {
		return new C();
	}

}

class C extends B {
}

