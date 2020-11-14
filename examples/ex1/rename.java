class Main {
	public static void main(String[] args) {
		System.out.println((new Cat()).sleep());
	}
}

class Animal {
	int x;

	int sleep() {
		return (new Dog()).wakeup(x);
	}

}

class Dog extends Animal {
	int sleep() {
		int x;
		x = 1;
		return (x) + (x);
	}

	int wakeup(int x) {
		x = (x) * ((x) + (x));
		return (this).sleep();
	}

}

class CuteDog extends Dog {
	int[] y;

	int sleep() {
		y = new int[1];
		y[x] = x;
		return (y)[(this).sleep()];
	}

}

class Cat extends Animal {
	int wakeup(int y) {
		Monster m;
		x = (x) + (y);
		return (x) + ((m).wakeup(x));
	}

}

class Monster {
	int x;

	int run() {
		x = 0;
		return (x) + (x);
	}

	int wakeup(int x) {
		x = 1;
		return (x) - (1);
	}

}

