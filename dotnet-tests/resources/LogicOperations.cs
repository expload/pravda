using System;
using Expload.Pravda;

[Program]
public class LogicOperations
{
   private Mapping<int, string> Log;

   public void TestLogicOperations()
   {
        Log[1] = Convert.ToString(false || true);
        Log[2] = Convert.ToString(false && true);
        Log[3] = Convert.ToString(true ^ true);

        Log[4] = Convert.ToString(3 | 5);
        Log[5] = Convert.ToString(3 & 5);
        Log[6] = Convert.ToString(3 ^ 5);
   }

   public static void Main() {}
}