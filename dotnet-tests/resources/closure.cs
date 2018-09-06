using System;

public class Program {
    public static void Main() {
         int e = 1;
         Func<int, int> d = x => x + e;
         int f = d(3);
    }
}