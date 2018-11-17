using System;
using Expload.Pravda;

[Program]
public class LogicOperations
{
   public Mapping<int, string> Log;

   public void TestLogicOperations()
   {
        Log.put(1, Convert.ToString(false || true));
        Log.put(2, Convert.ToString(false && true));
        Log.put(3, Convert.ToString(true ^ true));

        Log.put(4, Convert.ToString(3 | 5));
        Log.put(5, Convert.ToString(3 & 5));
        Log.put(6, Convert.ToString(3 ^ 5));
   }

   public static void Main() {}
}