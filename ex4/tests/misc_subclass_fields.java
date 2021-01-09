class Main {
	public static void main(String[] args) {
		System.out.println((new Tester()).test(new B()));
	}
}

class Tester {
	public int test(B b) {
		System.out.println((b).setA());
		System.out.println((b).setB());
		System.out.println((b).printA());
		System.out.println((b).printB());
		return 0;
	}

}

class A {
	int a;

	public int setA() {
		a = 5;
		return 0;
	}

	public int printA() {
		System.out.println(a);
		return 0;
	}

}

class B extends A {
	int b;

	public int setB() {
		b = 9;
		return 0;
	}

	public int printB() {
		System.out.println(b);
		return 0;
	}

}

