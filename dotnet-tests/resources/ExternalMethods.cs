using System;
using Expload.Pravda;

namespace ExternalNamespace {

    public class ExternalMethods {
        public int A;
        public int B;

        public static int Add(int a, int b) {
            return a + b;
        }

        public ExternalMethods(int a, int b) {
            this.A = a;
            this.B = b;
        }

        public int Add() {
            return A + B;
        }

        public int Add(int c) {
            return A + B + c;
        }
    }

    [Program]
    public class ExternalProgramMethods
    {
        public static ExternalProgramMethods GetInstance()
        {
            return ProgramHelper.Program<ExternalProgramMethods>(new Bytes(
                "123456789012345678901234567890123456789012345678901234567890ABCD"
            ));
        }

        public int Add(int a, int b)
        {
            return a + b;
        }

        public static void Main() {}
    }
}