# Pravda internal data format  

## String representation

Human-readable representation of `vm.Data`. Supported by assembler for PravdaVM.
 
### Primitive types

```
int8, int16, int32,
uint8, uint16, uint32
bigint, number
ref,
boolean,
utf8, bytes
```

1. All numbers encodes as `type(number)`. For example: `int16(500)` or `number(12.0)`. You can use decimal and hexadecimal way of writing for integers. Also you can write only a number and nearest type will be inferred automatically. For example: `4` will be `uint8`, `-500` will be `int16`.
2. Booleans encodes as `true` and `false`. 
3. Refs encodes as `#0x0000`.
4. UTF8 string encodes classically `"hello world"`.
5. Byte strings encodes as `xAABBCCEE`.

### Arrays

Pravda arrays is homogeneous. It means you can't store `int8` and `int32` in same array (or course you can store references). Array encodes as `type[one, two, three]`. For example: `utf8["one", "two", three]`, or `uint8[1, 2, 3]`. Also you can move type to item declaration if it's convenient: `[int8(1), int8(2)]`.

## Structs

Structs in pravda is tables where key and value are primitive. It's encodes as comma separated tuples or primitives. For example:

```
{
  0: "nothing",
  x11EE: "teh bytes",
  "nothing": 0 
}
```  

## Binary representation

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
uint8   := 0x05
uint16  := 0x06
uint32  := 0x07
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
               | bigint
               | uint8
               | uint16
               | uint256
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
           | bigint length bytes[&length]
           | uint8 bytes~1
           | uint16 bytes~2
           | uint32 bytes~4
           | double bytes~8 # strict IEEE-754 floating point number
           | ref byte[4] # ref is constant sized
           | boolean
           | utf8 bytes
           | bytestr bytes

data := primitive
      | array primitive_type length data(primitive_type)[&length]
      | struct length (primitive, primitive)[&length]
```

### How to read this?

1. `smth[num]` means that we duplicate `smth` structure num times. `byte[8]` means 8 bytes,
2. `bytes~num` means that we expect `num` of `bytes` (which length is dynamic).
3. `&length` refers to given `length` field and means an integer representation of that field.
4. `(a, b)` means pair type, e.g. two values of a and b are written consecutively.
5. `data(primitive_type)` means corresponding structure for `primitive` except type byte.

## Json representation

### Primitives

All primitives encodes as JSON strings with prefix. It's easy to parse. Most of popular languages have `indexOf` and `substring` functions.  Type always situated before first dot, value after.

```json
"int8.-100"
"int16.-100" 
"int32.-100"
"uint8.100"
"uint16.100"
"uint32.1000"
"bigint.9999999999999"
"number.2.0"
"ref.1"
"bool.true"
"utf8.i am cow"
"bytes.01fca4e9"
"null"
```

### Arrays

Arrays corresponds to JSON arrays. First item contains type of primitive.

```json
["int32", "100", "200", "300"]
```

### Structs

Structs corresponds to JSON objects.

```json
{
  "utf8.user": "ref.9153",
  "int32.1432": "bytes.41f8cff6"
}
```
