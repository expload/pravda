using System;

// WARNING
// Purpose of this dll is only to marks specific methods for translator.
// Translator detects them and generates necessary bytecode.
namespace Expload.Pravda {

    // Special attribute to mark classes to be translated to Pravda program
    public class Program : Attribute {}

    // Access to the storage
    public class Mapping<K, V> {
       public V get(K key) { return default(V); }
       public bool exists(K key) { return false; }
       public void put(K key, V value) { return; }
       public V getDefault(K key, V def) { return default(V); }
    }

    public class Info {
        // Get address of the sender user
        public static Bytes Sender() { return null; }

        // Get balance of given address
        public static long Balance(Bytes address) { return 0L; }

        // Get program address
        public static Bytes ProgramAddress() { return null; }
    }

    public class Log {
        public static void Event<T>(string name, T data) { return; }
    }

    public class Error {
        public static void Throw(String message) { return; }
    }

    // Immutable array of bytes
    public class Bytes {
       // Get the empty byte array
       public static Bytes EMPTY = null;
       // Get the void address (32 zero bytes)
       public static Bytes VOID_ADDRESS = null;

       public Bytes(params byte[] bytes) {}

       // Get the i-th byte
       public byte this[int i] { get { return 0; } set { return; } }
       // Get the sub-array
       public Bytes Slice(int start, int length) { return null; }
       // Concatenate two Bytes
       public Bytes Concat(Bytes other) { return null; }
       // Length of byte array
       public int Length() { return 0; }
    }

    public class StdLib {
        public static Bytes Ripemd160(Bytes bytes) { return null; }
        public static Bytes Ripemd160(String str) { return null; }

        public static bool ValidateEd25519Signature(Bytes pubKey, Bytes message, Bytes signature) { return false; }
        public static bool ValidateEd25519Signature(Bytes pubKey, String message, Bytes signature) { return false; }
    }

    public class ProgramHelper {
        public static T Program<T>(Bytes address) { return default(T); }
    }
}
