using Com.Expload;

[Program]
public class ProgramCompare {
   public void compare()
   {
       int a = 1;
       int b = 2;
       uint c = 3;
       uint d = 4;
       long e = 5L;
       long f = 6L;

       bool tmp = false;
       tmp = a == b;
       tmp = a == c;
       tmp = c == d;
       tmp = c == e;
       tmp = e == f;

       if (a == b) {}
       if (a == c) {}
       if (c == d) {}
       if (c == e) {}
       if (e == f) {}

       tmp = a >= b;
       tmp = a >= c;
       tmp = c >= d;
       tmp = c >= e;
       tmp = e >= f;

       if (a >= b) {}
       if (a >= c) {}
       if (c >= d) {}
       if (c >= e) {}
       if (e >= f) {}

       tmp = a <= b;
       tmp = a <= c;
       tmp = c <= d;
       tmp = c <= e;
       tmp = e <= f;

       if (a <= b) {}
       if (a <= c) {}
       if (c <= d) {}
       if (c <= e) {}
       if (e <= f) {}

       tmp = a != b;
       tmp = a != c;
       tmp = c != d;
       tmp = c != e;
       tmp = e != f;

       if (a != b) {}
       if (a != c) {}
       if (c != d) {}
       if (c != e) {}
       if (e != f) {}

       tmp = a > b;
       tmp = a > c;
       tmp = c > d;
       tmp = c > e;
       tmp = e > f;

       if (a > b) {}
       if (a > c) {}
       if (c > d) {}
       if (c > e) {}
       if (e > f) {}

       tmp = a < b;
       tmp = a < c;
       tmp = c < d;
       tmp = c < e;
       tmp = e < f;

       if (a < b) {}
       if (a < c) {}
       if (c < d) {}
       if (c < e) {}
       if (e < f) {}
   }

   public static void Main() {}
}