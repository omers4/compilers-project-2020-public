class Main {
	public static void main(String[] a) {
		System.out.println(3);
	}
}

class A {
	public int fun() {
		int a;
		int b;
		b = (new B()).getInt(a);
		return 0;
	}

}

class B {
	public int getInt(int x) {
		return 8;
	}

}

