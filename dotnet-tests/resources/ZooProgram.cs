using System;
using Expload.Pravda;

[Program]
public class ZooProgram
{
    private Mapping<String, int> PetToZoo = new Mapping<String, int>();
    private Mapping<String, Bytes> PetSignature = new Mapping<String, Bytes>();
    private Mapping<String, Bytes> PetToOwner = new Mapping<String, Bytes>();
    private Mapping<int, Bytes> ZooToOwner = new Mapping<int, Bytes>();
    public int ZooCnt = 1;
    public int PetId = 1;

    private Bytes GenerateSignature(String pet)
    {
        byte[] sign = new byte[10];
        for (int i = 0; i < 10; i++) {
            sign[i] = Convert.ToByte(pet[i % pet.Length] / 2);
        }
        return new Bytes(sign);
    }

    public int NewZoo()
    {
        ZooToOwner[ZooCnt] = Info.Sender();
        ZooCnt += 1;
        return ZooCnt - 1;
    }

    public void TransferZoo(Bytes to, int zoo)
    {
        if (ZooToOwner.GetOrDefault(zoo, Bytes.EMPTY) == Info.Sender()) {
            ZooToOwner[zoo] = to;
        }
    }

    public String NewPet(int zoo)
    {
        if (ZooToOwner.GetOrDefault(zoo, Bytes.EMPTY) == Info.Sender()) {
            String pet = "pet" + System.Convert.ToString(PetId);
            PetToOwner[pet] = Info.Sender();
            PetSignature[pet] = GenerateSignature(pet);
            PetId += 1;
            return pet;
        }
        return "";
    }

    public void TransferPet(Bytes to, int zoo, String pet)
    {
        if (PetToOwner.GetOrDefault(pet, Bytes.EMPTY) == Info.Sender() && ZooToOwner.GetOrDefault(zoo, Bytes.EMPTY) == to) {
           PetToOwner[pet] = to;
           PetToZoo[pet] = zoo;
        }
    }

    public String BreedPets(String pet1, String pet2)
    {
        if (PetToOwner.GetOrDefault(pet1, Bytes.EMPTY) == Info.Sender() &&
                PetToOwner.GetOrDefault(pet2, Bytes.EMPTY) == Info.Sender() &&
                PetToZoo.GetOrDefault(pet1, -1) == PetToZoo.GetOrDefault(pet2, -1)) {

            String newPet = pet1 + pet2;
            PetToOwner[newPet] = Info.Sender();
            PetSignature[newPet] = PetSignature[pet1].Concat(PetSignature[pet2]);
            return newPet;
        } else {
            return "";
        }
    }

    public static void Main() {}
}
