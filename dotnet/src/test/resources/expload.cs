using System;

// WARNING
// Purpose of this dll is only to marks specific methods for translator.
// Translator detects them and generates necessary bytecode.
namespace Com.Expload {
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
    }

    // Immutable array of bytes
    public class Bytes {
       // Get the empty byte array
       public static Bytes EMPTY = null;

       public Bytes(params byte[] bytes) {}

       // Get the i-th byte
       public byte this[int i] { get { return 0; } set { return; } }
       // Get the sub-array
       public Bytes Slice(int start, int length) { return null; }
       // Concatenate two Bytes
       public Bytes Concat(Bytes other) { return null; }
    }

    public class StdLib {
        public static Bytes Ripemd160(Bytes bytes) { return null; }
        public static Bytes Ripemd160(String str) { return null; }

        public static bool ValidateEd25519Signature(Bytes pubKey, Bytes message, Bytes signature) { return false; }
        public static bool ValidateEd25519Signature(Bytes pubKey, String message, Bytes signature) { return false; }
    }
}
