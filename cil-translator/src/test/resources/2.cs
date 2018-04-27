using System;

public class Program {
   public static int val = 42;

   public static int B() {
       return Program.val;
   }

   public static void Main() {
       int a = 2, b = Program.B();
       int c = a + b;
       int d = a * b;
       int e = a / b;
   }
}