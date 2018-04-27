
public class Program {
   public static void Main() {
       int a = 0;
       for (int i = 0; i < 10; i++) {
           a += 2;
       }

       while (a < 10000) {
          a *= 2;
       }
   }
}