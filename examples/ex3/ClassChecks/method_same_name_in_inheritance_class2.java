class Main {
    public static void main(String[] args) {
        System.out.println(new Example().run());
    }
}

class Example {
    public int run() {
        int x;
        x = 0;
        return x + x;
    }

    public int other() {
        int x;
        int xx;
        x = 1;
        xx = x;
        return x - 1;
    }
}

class A extends Example {
    public void run() {
        int x;
        x = 0;
    }
}