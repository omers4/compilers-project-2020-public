class Main {
	public static void main(String[] a) {
		System.out.println(3);
	}
}

class A {
	public int fun(int[] b) {
		return (b).fun();
	}

}

class B {
	public int fun() {
		return 3;
	}

}

