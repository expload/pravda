using Expload.Pravda;

[Program]
public class ProgramLoops {

   public void loops()
   {
       int a = 0;
       for (int i = 0; i < 10; i++) {
           a += 2;
       }

       while (a < 10000) {
          a *= 2;
       }
   }

   public static void Main() {}
}