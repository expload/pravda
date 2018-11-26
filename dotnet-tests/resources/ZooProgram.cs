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
        ZooToOwner.put(ZooCnt, Info.Sender());
        ZooCnt += 1;
        return ZooCnt - 1;
    }

    public void TransferZoo(Bytes to, int zoo)
    {
        if (ZooToOwner.getDefault(zoo, Bytes.EMPTY) == Info.Sender()) {
            ZooToOwner.put(zoo, to);
        }
    }

    public String NewPet(int zoo)
    {
        if (ZooToOwner.getDefault(zoo, Bytes.EMPTY) == Info.Sender()) {
            String pet = "pet" + System.Convert.ToString(PetId);
            PetToOwner.put(pet, Info.Sender());
            PetSignature.put(pet, GenerateSignature(pet));
            PetId += 1;
            return pet;
        }
        return "";
    }

    public void TransferPet(Bytes to, int zoo, String pet)
    {
        if (PetToOwner.getDefault(pet, Bytes.EMPTY) == Info.Sender() && ZooToOwner.getDefault(zoo, Bytes.EMPTY) == to) {
           PetToOwner.put(pet, to);
           PetToZoo.put(pet, zoo);
        }
    }

    public String BreedPets(String pet1, String pet2)
    {
        if (PetToOwner.getDefault(pet1, Bytes.EMPTY) == Info.Sender() &&
                PetToOwner.getDefault(pet2, Bytes.EMPTY) == Info.Sender() &&
                PetToZoo.getDefault(pet1, -1) == PetToZoo.getDefault(pet2, -1)) {

            String newPet = pet1 + pet2;
            PetToOwner.put(newPet, Info.Sender());
            PetSignature.put(newPet, PetSignature.get(pet1).Concat(PetSignature.get(pet2)));
            return newPet;
        } else {
            return "";
        }
    }

    public static void Main() {}
}
