
public class Program {
   public static int answer() {
       return 42;
   }

   private static int secretAnswer() {
       return 40 + 2;
   }

   public static int sum(int a, int b) {
       return a + b;
   }

   public int personalAnswer() {
       return 21 * 2;
   }

   private int personalSecretAnswer() {
       return 126 / 3;
   }

   public static void Main() {
       int a = Program.answer();
       int b = Program.secretAnswer();
       int c = Program.sum(a, b);

       Program p = new Program();
       int d = p.personalAnswer();
       int e = p.personalSecretAnswer();
   }
}