using Xunit;
using System;
using System.Collections.Generic;
using Expload.Pravda;

namespace Expload.Pravda.Tests
{
    public class PravdaLocalImplementation : IDisposable
    {
        public void Dispose()
        {
            Sandbox.SandboxValues.ClearValues();
        }

        [Fact]
        public void SenderIsVoid()
        {
            Assert.True(Info.Sender() == Bytes.VOID_ADDRESS);
        }

        [Fact]
        public void MappingSetGet()
        {
            var mapping = new Mapping<string, int>();
            mapping["key1"] = 1;
            mapping["key2"] = 2;
            Assert.Equal(1, mapping["key1"]);
            Assert.Equal(2, mapping["key2"]);

            mapping["key2"] = -2;
            Assert.Equal(-2, mapping["key2"]);
        }

        [Fact]
        public void MappingSetGetBytes()
        {
            var mapping = new Mapping<Bytes, int>();
            mapping[new Bytes(42, 1)] = 1;
            mapping[new Bytes(42, 2)] = 2;
            Assert.Equal(1, mapping[new Bytes(42, 1)]);
            Assert.Equal(2, mapping[new Bytes(42, 2)]);

            mapping[new Bytes(42, 2)] = -2;
            Assert.Equal(-2, mapping[new Bytes(42, 2)]);
        }

        [Fact]
        public void MappingContainsDefault()
        {
            var mapping = new Mapping<string, int>();
            mapping["key1"] = 1;
            Assert.True(mapping.ContainsKey("key1"));
            Assert.False(mapping.ContainsKey("key2"));

            Assert.Equal(1, mapping.GetOrDefault("key1", 0));
            Assert.Equal(0, mapping.GetOrDefault("key2", 0));
        }

        [Fact]
        public void EventAdd()
        {
            Log.Event("event1", 1);
            Log.Event("event2", 2.0);
            Log.Event("event3", "3");
            Log.Event("event4", new Bytes(4));

            var expected = new List<Tuple<string, object>> {
                Tuple.Create<string, object>("event1", 1),
                Tuple.Create<string, object>("event2", 2.0),
                Tuple.Create<string, object>("event3", "3"),
                Tuple.Create<string, object>("event4", new Bytes(4))
            };

            Assert.Equal(expected, Sandbox.SandboxValues.Events);
        }

        [Fact]
        public void ThrowError()
        {
            Assert.Throws<PravdaException>(() => Error.Throw("Noooo!"));
        }

        [Fact]
        public void BytesMethods()
        {
            Bytes bytes = new Bytes(1, 2, 3, 4, 5, 6, 7, 8);

            Assert.Equal(new Bytes(1, 2, 3, 4, 5, 6, 7, 8), bytes);
            Assert.Equal(new Bytes(4, 5), bytes.Slice(3, 2));
            Assert.Equal(new Bytes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), bytes.Concat(new Bytes(9, 10)));
            Assert.Equal(8, bytes.Length());
            Assert.Equal(1, bytes[0]);
            Assert.Equal(8, bytes[7]);
        }

        [Fact]
        public void TransferFromSender()
        {
            var sender = new Bytes("0102030405060708090A");
            var to = new Bytes("0B0C0D0E0F1011121314");
            Sandbox.SandboxValues.Sender = sender;

            Assert.Equal(sender, Info.Sender());

            Sandbox.SandboxValues.Balances[sender] = 100L;
            Actions.Transfer(to, 40L);

            Assert.Equal(60L, Info.Balance(sender));
            Assert.Equal(40L, Info.Balance(to));
        }

        [Fact]
        public void TransferFromProgram()
        {
            var program = new Bytes("0102030405060708090A");
            var to = new Bytes("0B0C0D0E0F1011121314");
            Sandbox.SandboxValues.ProgramAddress = program;

            Assert.Equal(program, Info.ProgramAddress());

            Sandbox.SandboxValues.Balances[program] = 100L;
            Actions.TransferFromProgram(to, 40L);

            Assert.Equal(60L, Info.Balance(program));
            Assert.Equal(40L, Info.Balance(to));
        }

        [Fact]
        public void HexToBytes()
        {
            Assert.Equal(new Bytes(1, 10, 31, -1), StdLib.HexToBytes("010A1FFF"));
        }

        [Fact]
        public void BytesToHex()
        {
            Assert.Equal("010A1FFF", StdLib.BytesToHex(new Bytes(1, 10, 31, -1)));
        }
    }
}
