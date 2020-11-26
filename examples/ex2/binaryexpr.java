class Main {
	public static void main(String[] args) {
		System.out.println(new Example().run());
	}
}

class Example{
	public int run() {

		System.out.println(33 * 44);

		System.out.println((100 - 50) + 11);

		System.out.println(100 < 50);

		System.out.println(!(100 < 50));

		return 1;
	}
}
