// PA1 parse ref fail
// PA1 parse decl fail
class Main {
	public static void main(String[] args) {
		System.out.println((new Test()).test());
	}
}

class Test {

    public int p(int a) {
	that.this = 4;
    }
}

