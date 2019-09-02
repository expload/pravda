# Internal Data Format

## String Representation

Human-readable representation of `vm.Data`, that us supported by the PravdaVM assembler.

### Primitive Types

```
int8, int16, int32, int64
bigint, number
ref,
boolean,
utf8, bytes
```

1. All numbers encode as `type(number)`. For example: `int16(500)` or `number(12.0)`. You can use a decimal or a hexadecimal way of writing for integers. Also, you can write only a number and the nearest type will be inferred automatically. For example: `4` will be `int8`, `-500` will be `int16`.
2. Booleans encode as `true` and `false`.
3. Refs encode as `#0x0000`.
4. UTF8 string encodes classically `"hello world"`.
5. Byte strings encode as `xAABBCCEE`.

### Arrays

Pravda arrays are homogeneous. This means that you cannot store `int8` and `int32` in the same array (of course, you can store references). Array encodes as `type[one, two, three]`. For example: `utf8["one", "two", three]`, or `uint8[1, 2, 3]`. Also, you can move the type to item declaration, where this is convenient: `[int8(1), int8(2)]`.

## Structs

Structs in Pravda are tables where the key and value are primitive. They encode as comma separated tuples or primitives. For example:

```
{
  0: "nothing",
  x11EE: "teh bytes",
  "nothing": 0
}
```

## Binary Representation

```
length := 0b00<6 bits of data>
       | 0b01<6 bits length>
       | 0b10<14 bits length>
       | 0b11<22 bits length>

bytes := length byte[&length]

null    := 0x00
int8    := 0x01
int16   := 0x02
int32   := 0x03
bigint  := 0x04
int64   := 0x05
decimal := 0x08
boolean := 0x09
ref     := 0x0A
utf8    := 0x0B
array   := 0x0C
struct  := 0x0D
bytestr := 0x0E

primitive_type := int8
               | int16
               | int32
               | int64
               | bigint
               | double
               | boolean
               | ref
               | null

type := primitive_type
      | struct
      | array
      | utf8
      | bytestr

primitive := int8 bytes~1
           | int16 bytes~2
           | int32 bytes~4
           | int64 bytes~8
           | bigint length bytes[&length]
           | double bytes~8 # strict IEEE-754 floating point number
           | ref byte[4] # ref is constant sized
           | boolean
           | utf8 bytes
           | bytestr bytes

data := primitive
      | array primitive_type length data(primitive_type)[&length]
      | struct length (primitive, primitive)[&length]
```

### How to Read This?

1. `smth[num]` means that we duplicate the `smth` structure num times. `byte[8]` means 8 bytes.
2. `bytes~num` means that we expect `num` of `bytes` (which length is dynamic).
3. `&length` refers to the given `length` field and means an integer representation of this field.
4. `(a, b)` means a pair type, e.g. two values of a and b are written consecutively.
5. `data(primitive_type)` means the corresponding structure for the `primitive`, except for the type byte.

## Json Representation

### Primitives

All primitives encode as JSON strings with a prefix. They are easy to parse. Most of the popular languages have `indexOf` and `substring` functions.  The type always precedes the first dot, and the value follows after the dot.

```json
"int8.-100"
"int16.-100"
"int32.-100"
"int64.-100"
"bigint.9999999999999"
"number.2.0"
"ref.1"
"bool.true"
"utf8.i am cow"
"bytes.01fca4e9"
"null"
```

### Arrays

Arrays correspond to JSON arrays. The first item contains the type of primitive.

```json
["int32", "100", "200", "300"]
```

### Structs

Structs correspond to JSON objects.

```json
{
  "utf8.user": "ref.9153",
  "int32.1432": "bytes.41f8cff6"
}
```
