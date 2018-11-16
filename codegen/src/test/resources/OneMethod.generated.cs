using System;
using System.Collections;
using Expload.Unity.Codegen;

namespace Expload.Pravda.OneMethod
{
    public class BalanceOfRequest: ProgramRequest<uint>
    {
        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override uint ParseResult(string elem)
        {
            return ExploadTypeConverters.ParseUInt32(elem);
        }

        public IEnumerator Test(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, true);
        }

        public IEnumerator Call(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, false);
        }

        // Same as Call
        // Deprecated
        public IEnumerator BalanceOf(byte[] arg0)
        {
            yield return SendRequest("BalanceOf", new string[] { ExploadTypeConverters.PrintBytes(arg0) }, false);
        }
    }
}