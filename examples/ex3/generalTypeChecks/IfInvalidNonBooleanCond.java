class Factorial {
    public static void main(String[] a) {
        System.out.println(new Fac().ComputeFac(10));
    }
}

class Fac {

    public int ComputeFac(int num) {
      int num_aux ;
      if (num + num) { // This should throw exception because non boolean cond, but an int
        num_aux = 1 ;
      } else {
        num_aux = num * (this.ComputeFac(num-1)) ;
      }
      return num_aux ;
  }

}
