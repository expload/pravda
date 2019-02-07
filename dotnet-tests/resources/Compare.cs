using Expload.Pravda;

[Program]
public class Compare
{
   public bool TestCompare()
   {
       int a = 1;
       int b = 2;
       short c = 3;
       short d = 4;
       long e = 5L;
       long f = 6L;

       bool tmp = true;
       tmp &= a == b;
       tmp &= a == c;
       tmp &= c == d;
       tmp &= c == e;
       tmp &= e == f;

       if (a == b) { tmp = true; }
       if (a == c) { tmp = true; }
       if (c == d) { tmp = true; }
       if (c == e) { tmp = true; }
       if (e == f) { tmp = true; }

       tmp &= a >= b;
       tmp &= a >= c;
       tmp &= c >= d;
       tmp &= c >= e;
       tmp &= e >= f;

       if (a >= b) { tmp = true; }
       if (a >= c) { tmp = true; }
       if (c >= d) { tmp = true; }
       if (c >= e) { tmp = true; }
       if (e >= f) { tmp = true; }

       tmp &= a <= b;
       tmp &= a <= c;
       tmp &= c <= d;
       tmp &= c <= e;
       tmp &= e <= f;

       if (a <= b) { tmp = true; }
       if (a <= c) { tmp = true; }
       if (c <= d) { tmp = true; }
       if (c <= e) { tmp = true; }
       if (e <= f) { tmp = true; }

       tmp &= a != b;
       tmp &= a != c;
       tmp &= c != d;
       tmp &= c != e;
       tmp &= e != f;

       if (a != b) { tmp = true; }
       if (a != c) { tmp = true; }
       if (c != d) { tmp = true; }
       if (c != e) { tmp = true; }
       if (e != f) { tmp = true; }

       tmp &= a > b;
       tmp &= a > c;
       tmp &= c > d;
       tmp &= c > e;
       tmp &= e > f;

       if (a > b) { tmp = true; }
       if (a > c) { tmp = true; }
       if (c > d) { tmp = true; }
       if (c > e) { tmp = true; }
       if (e > f) { tmp = true; }

       tmp &= a < b;
       tmp &= a < c;
       tmp &= c < d;
       tmp &= c < e;
       tmp &= e < f;

       if (a < b) { tmp = true; }
       if (a < c) { tmp = true; }
       if (c < d) { tmp = true; }
       if (c < e) { tmp = true; }
       if (e < f) { tmp = true; }

       return tmp;
   }

   public static void Main() {}
}