class Main {
	public static void main(String[] a) {
		System.out.println(3);
	}
}

class A {
	public int fun() {
		return (this).fun2(0, 0);
	}

	public int fun2(int x) {
		return x;
	}

}

