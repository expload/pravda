using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

namespace Expload.Pravda.Program {
    [System.Serializable]
    class StringResult {
       public string value;
       public string tpe;

       public static StringResult FromJson(string json) {
           return JsonUtility.FromJson<StringResult>(json);
       }
    }
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

    class BreedPetsRequest: ProgramRequest<string> {

        public BreedPetsRequest(byte[] programAddress) : base(programAddress) { }

        protected override string ParseResult(string json)
        {
            return StringResult.FromJson(json).value;
        }

        public IEnumerator BreedPets(string arg0, string arg1)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"BreedPets\", \"args\": [{{ \"value\": {1}, \"tpe\": \"utf8\" }}, {{ \"value\": {2}, \"tpe\": \"utf8\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + arg0 + "\"" ,  "\"" + arg1 + "\"" );
            yield return SendJson(json);
        }
    }
    class NewPetRequest: ProgramRequest<string> {

        public NewPetRequest(byte[] programAddress) : base(programAddress) { }

        protected override string ParseResult(string json)
        {
            return StringResult.FromJson(json).value;
        }

        public IEnumerator NewPet(int arg0)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"NewPet\", \"args\": [{{ \"value\": {1}, \"tpe\": \"int32\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" , arg0);
            yield return SendJson(json);
        }
    }
    class NewZooRequest: ProgramRequest<int> {

        public NewZooRequest(byte[] programAddress) : base(programAddress) { }

        protected override int ParseResult(string json)
        {
            return IntResult.FromJson(json).value;
        }

        public IEnumerator NewZoo()
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"NewZoo\", \"args\": [] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" );
            yield return SendJson(json);
        }
    }
    class TransferPetRequest: ProgramRequest<object> {

        public TransferPetRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator TransferPet(byte[] arg0, int arg1, string arg2)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"TransferPet\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bytes\" }}, {{ \"value\": {2}, \"tpe\": \"int32\" }}, {{ \"value\": {3}, \"tpe\": \"utf8\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + BitConverter.ToString(arg0).Replace("-","") + "\"" , arg1,  "\"" + arg2 + "\"" );
            yield return SendJson(json);
        }
    }
    class TransferZooRequest: ProgramRequest<object> {

        public TransferZooRequest(byte[] programAddress) : base(programAddress) { }

        protected override object ParseResult(string json)
        {
            return null;
        }

        public IEnumerator TransferZoo(byte[] arg0, int arg1)
        {
            String json = String.Format("{{ \"address\": {0}, \"method\": \"TransferZoo\", \"args\": [{{ \"value\": {1}, \"tpe\": \"bytes\" }}, {{ \"value\": {2}, \"tpe\": \"int32\" }}] }}",  "\"" + BitConverter.ToString(ProgramAddress).Replace("-","") + "\"" ,  "\"" + BitConverter.ToString(arg0).Replace("-","") + "\"" , arg1);
            yield return SendJson(json);
        }
    }
}