/*
 * C# Program to Find Magnitude of Integer
 */

using System;

public class Program {
    public static void Main() {
        int num, mag = 0;
        Console.WriteLine("Enter the Number : ");
        num = int.Parse(Console.ReadLine());
        Console.WriteLine("Number: " + num);
        while (num > 0) {       
            mag++;  
            num = num / 10;
        }
        Console.WriteLine("Magnitude: " + mag);
        Console.Read();
   }
}