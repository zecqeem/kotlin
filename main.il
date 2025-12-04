.assembly extern mscorlib {}
.assembly main {}
.module main.exe

.class private auto ansi beforefieldinit Program extends [mscorlib]System.Object {
  .field public static float32 PI
  .field public static string GREETING
  .field public static int32 x
  .field public static float32 y
  .field public static bool z
  .field public static string name


  .method public hidebysig static int32 sum(int32 a, int32 b) cil managed {
    ldarg a
    ldarg b
    add
    ret
  }

  .method public hidebysig static bool is_positive1(float32 num) cil managed {
    ldarg num
    conv.r4
    ldc.r4 0.0
    cgt
    brfalse L1
    ldc.i4.1
    ret
    br L2
L1:
    ldc.i4.0
    ret
L2:
  }

  .method public hidebysig static float32 calculate_area(float32 radius) cil managed {
    ldsfld float32 Program::PI
    ldarg radius
    conv.r8
    ldc.i4 2
    conv.r8
    call float64 [mscorlib]System.Math::Pow(float64, float64)
    conv.r4
    conv.r4
    mul
    ret
  }
  .method private hidebysig static void Main(string[] args) cil managed {
    .entrypoint
    ldc.r4 3.14159
    stsfld float32 Program::PI
    ldstr "Hello, world!"
    stsfld string Program::GREETING
    ldc.i4 10
    stsfld int32 Program::x
    ldc.r4 5.5
    stsfld float32 Program::y
    ldc.i4.0
    stsfld bool Program::z
    ldstr "gorb user"
    stsfld string Program::name
    ldstr "All tokens and constructions example"
    call void [mscorlib]System.Console::WriteLine(string)
    ldsfld int32 Program::x
    ldc.i4 5
    call int32 Program::sum(int32, int32)
    stsfld int32 Program::x
    ldc.i4 1
    ldsfld int32 Program::x
    mul
    call void [mscorlib]System.Console::WriteLine(int32)
    ldsfld float32 Program::y
    call bool Program::is_positive1(float32)
    brfalse L3
    ldstr "y is positive"
    call void [mscorlib]System.Console::WriteLine(string)
    br L4
L3:
    ldstr "y is not positive"
    call void [mscorlib]System.Console::WriteLine(string)
L4:
L5:
    ldsfld int32 Program::x
    ldc.i4 10
    cgt
    brfalse L6
    ldsfld int32 Program::x
    ldc.i4 1
    sub
    stsfld int32 Program::x
    ldstr "x is now "
    ldsfld int32 Program::x
    box [mscorlib]System.Int32
    call string [mscorlib]System.String::Concat(object, object)
    call void [mscorlib]System.Console::WriteLine(string)
    br L5
L6:
    ldc.r4 2.0
    call float32 Program::calculate_area(float32)
    stsfld float32 Program::y
    ldsfld float32 Program::y
    call void [mscorlib]System.Console::WriteLine(float32)
    ldc.i4 5
    ldc.i4 3
    cgt
    ldc.i4.1
    ceq
    stsfld bool Program::z
    ldsfld bool Program::z
    call void [mscorlib]System.Console::WriteLine(bool)
    ldstr "Enter a number: "
    call void [mscorlib]System.Console::WriteLine(string)
    call string [mscorlib]System.Console::ReadLine()
    call float32 [mscorlib]System.Convert::ToSingle(string)
    stsfld float32 Program::y
    ldstr "You entered: "
    ldsfld float32 Program::y
    box [mscorlib]System.Single
    call string [mscorlib]System.String::Concat(object, object)
    call void [mscorlib]System.Console::WriteLine(string)
    ldstr ""
    call void [mscorlib]System.Console::WriteLine(string)
    ldsfld string Program::GREETING
    ldstr ", "
    call string [mscorlib]System.String::Concat(object, object)
    ldsfld string Program::name
    call string [mscorlib]System.String::Concat(object, object)
    call void [mscorlib]System.Console::WriteLine(string)
    ret
  }
}
