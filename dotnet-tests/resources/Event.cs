using System;
using Expload.Pravda;

[Program]
class Event
{
    public void TestEvent() {
        Log.Event("my_event", 1234);
        Log.Event("my_event", "my_string");
        Log.Event("my_event", 2.0);
        Log.Event("my_event", new Bytes(0x01, 0x02, 0x03, 0x04));
    }

    public static void Main() {}
}