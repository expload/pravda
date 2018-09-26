# Dotnet classes translation

### Program class

Each C# file that is going to be translated should follow the listed rules:
  * Class with Program methods must be marked with `[Program]` attribute
  * Each compiled .exe file must contain exactly one class with `[Program]` attribute.
  * Program class can inherit interfaces _[future plans]_
  * Program class contains only public fields and public or private methods.
  * Static fields and methods are not allowed.
  * Public methods are translated to Program methods.
  * Private methods are translated to inner functions, only difference between them and Program methods is that inner functions are not accessible from outside world and can only be called from Program method.
  * Fields (that can be only public) are translated to storage items with `utf8("p_<field_name>")` keys.
  * Only one constructor of Program class is allowed. This constructor mustn't have any arguments.

### User defined classes

User can freely define classes without `[Program]` attribute.
Objects of these classes are translated to [data `struct`s](../vm/data.md) and behave very similar to objects in C#.

Formally this translation follow the listed rules:
  * Class doesn't have `[Program]` attribute, `[Program]` attribute is used for only one class, which is translated to Program methods.
  * Interfaces are not translated, they serve only as compile-time entities.
  * **All** (`private`, `protected`, `internal` and `public`) fields are translated to `utf8("<field_name>") -> <field_value>` pairs in `struct`.
  * **All** methods are translated to `utf8("<method_name>_<args_types>")` -> `ref(#<inner_function_offset>)`.
    The `<args_types>` prefix is needed to support overloading and
    `<inner_function_offset>` means offset of function that should be called for the `<method_name>` method of that class.
    Overridden methods will point to different `<inner_function_offset>`.
    This technique is similar to [Virtual method table](https://en.wikipedia.org/wiki/Virtual_method_table).
  * `static` fields are translated to storage items with `utf8("s_<class_name>_<field_name>")` keys.
  * `static` methods are translated to inner functions with `<class_name>_<method_name>_<args_types>` names.
  * Constructors are translated to inner functions with `<class_name>_ctor_<args_types>` names.

##### Example:

Suppose we have the following classes definitions in C#:
```c#
interface Vehicle
{
  void ComeIn(String someone);
}

class Bicycle : Vehicle
{
  public String Owner = "no one";

  void ComeIn(String someone) {
    Owner = someone;
  }
}

class Car : Vehicle
{
  static private bool isBearBurnedDown = false;
  static public bool IsBearBurnedDown()
  {
    return Car.isBearBurnedDown;
  }

  public int NumberOfTires;

  public Car(int tires)
  {
    NumberOfTires = tires.
  }

  void ComeIn(String someone)
  {
    if (someone == "bear") {
      Car.isBearBurnedDown = true;
    }
  }
}
```

- `interface Vehicle` won't be translated to anything
- `Bicycle()` constructor will be translated to `Bicycle_ctor` function that creates `struct(utf8("Owner") -> utf8("no one"), utf8("ComeIn_string") -> ref(#<function1>)`
- `Bicycle.ComeIn` will be translated to some function (let's call it `function1`) that changes `utf8("Owner")` field in the given `struct`.
- `Car(int)` constructor will be translated to `Car_ctor_int32` function that creates `struct(utf8("NumberOfTires") -> int32(<given_int>), utf8("ComeIn_string") -> ref(#<function2>))`
- `isBearBurnedDown` static field will be translated to `utf8("s_Car_isBearBurnedDown") -> bool` storage item
- `IsBearBurnedDown` static method will be translated to `Car_IsBearBurnedDown` function that reads `utf8("s_Car_isBearBurnedDown")` storage key.
- `Car.ComeIn` will be translated to some function (let's call it `function2`) that changes `utf8("s_Car_isBearBurnedDown") -> bool` storage item according to the given `String` from the stack.