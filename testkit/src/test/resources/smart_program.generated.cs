using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

namespace Com.Expload.Program {
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
        public byte[] ProgramAddress { get; protected set; }

        public T Result { get; protected set; }
        public string Error { get; protected set; }
        public bool IsError { get; protected set; }

        protected ProgramRequest(byte[] programAddress)
        {
            ProgramAddress = programAddress;
            IsError = false;
            Error = "";
        }

        protected abstract T ParseResult(string json);

        protected IEnumerator SendJson(string json)
        {
            UnityWebRequest www = UnityWebRequest.Put("localhost:8087/api/program/method", json);
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

        public BalanceOfRequest(byte[] programAddress) : base(programAddress) { }

        protected override int ParseResult(string json)
        {
            return IntResult.FromJson(json).value;
        }

        public IEnumerator BalanceOf(byte[] arg0)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"balanceOf\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bytes\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + BitConverter.ToString(arg0).Replace("-","") + "\"" );
            yield return SendJson(json);
        }
    }
    class TransferRequest: ProgramRequest<object> {

        public TransferRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator Transfer(byte[] arg0, int arg1)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"transfer\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bytes\" }}, {{ \"value\": {2}, \"tpe\": \"int32\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + BitConverter.ToString(arg0).Replace("-","") + "\"" , arg1);
            yield return SendJson(json);
        }
    }
}