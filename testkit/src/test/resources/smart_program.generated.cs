using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;
using Keiwando.BigInteger;

namespace Io.Mytc.Program {
    [System.Serializable]
    class IntResult {
       public int value;
       public string tpe;

       public static IntResult FromJson(string json) {
           return JsonUtility.FromJson<IntResult>(json);
       }
    }

    abstract class ProgramRequest<T>
    {
        public BigInteger ProgramAddress { get; protected set; }

        public T Result { get; protected set; }
        public string Error { get; protected set; }
        public bool IsError { get; protected set; }

        protected ProgramRequest(BigInteger programAddress)
        {
            ProgramAddress = programAddress;
            IsError = false;
            Error = "";
        }

        protected abstract T ParseResult(string json);

        protected IEnumerator SendJson(string json)
        {
            UnityWebRequest www = UnityWebRequest.Put("localhost:8080/program/method", json);
            www.method = "POST";
            www.SetRequestHeader("Content-Type", "application/json");

            yield return www.SendWebRequest();

            if (www.isNetworkError || www.isHttpError)
            {
                IsError = true;
                Error = www.error;
            }
            else
            {
                try
                {
                    Result = ParseResult(www.downloadHandler.text);
                }
                catch (ArgumentException e)
                {
                    IsError = true;
                    Error = "Invalid JSON: " + www.downloadHandler.text + "\n" + e.Message;
                }
            }
        }
    }

    class BalanceOfRequest: ProgramRequest<int> {

        public BalanceOfRequest(BigInteger programAddress) : base(programAddress) { }

        protected override int ParseResult(string json)
        {
            return IntResult.FromJson(json).value;
        }

        public IEnumerator BalanceOf(BigInteger arg0)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"balanceOf\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + arg0.ToHexString() + "\"" );
            yield return SendJson(json);
        }
    }
    class TransferRequest: ProgramRequest<object> {

        public TransferRequest(BigInteger programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Transfer(BigInteger arg0, int arg1)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"transfer\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bigint\" }}, {{ \"value\": {2}, \"tpe\": \"int32\" }}] }}",  "\"" + ProgramAddress.ToHexString() + "\"" ,  "\"" + arg0.ToHexString() + "\"" , arg1);
            yield return SendJson(json);
        }
    }
    class MainRequest: ProgramRequest<object> {

        public MainRequest(BigInteger programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Main()
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"Main\", \"args\": [] }}",  "\"" + ProgramAddress.ToHexString() + "\"" );
            yield return SendJson(json);
        }
    }
}