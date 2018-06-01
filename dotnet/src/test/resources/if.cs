
public class Program {
   public static int x = 1;

   public static void Main() {
       if (x < 1) {
           x = 4;
       }
       if (x > 5) {
          if (x > 6) {
              x = 7;
          }
       }
       if (x > 0) {
          x = 4;
       } else {
          x = 5;
       }
   }
}