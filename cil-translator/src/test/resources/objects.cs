
public class A {
   private int a;

   public A(int _a) {
       this.a = _a;
   }

   public int answerA() {
       return a + 42;
   }
}

public class B {
    private int b;

    public B(int _b) {
        this.b = _b;
    }

    public int answerB() {
        return b + 42;
    }
}

public class Program {
   public static void Main() {
       A a = new A(-42);
       B b = new B(0);
       int c = a.answerA() + b.answerB();
   }
}