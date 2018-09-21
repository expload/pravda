using Expload.Pravda;

[Program]
public class ProgramIfs {
   public void ifs()
   {
       int x = 10;

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

       if (x > 2 && x < 4) {
          x = 6;
       } else {
          x = 8;
       }

       if (x > 7 || x > 10) {
           x = 1;
       } else {
           x = 0;
       }

       if ((x > 1 && x < 3) || x > 20) {
           x = 2;
       } else {
           x = 3;
       }
   }

   public static void Main() {}
}