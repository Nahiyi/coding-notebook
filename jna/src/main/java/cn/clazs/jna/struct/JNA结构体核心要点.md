# JNA结构体映射核心要点

## 1. 结构体类定义

### 基本结构
```java
public class MyStruct extends Structure {
    public int field1;
    public double field2;

    // 方式1: 使用注解（推荐）
    @FieldOrder({"field1", "field2"})

    // 方式2: 重写方法（二选一即可）
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("field1", "field2");
    }
}
```

### 关键点
- ✅ **必须**继承`Structure`
- ✅ **必须**指定字段顺序（注解或方法二选一）
- ✅ 字段必须是`public`
- ✅ 字段类型使用JNA基本类型（`int`, `double`, `NativeLong`等）

## 2. 字段顺序的重要性

```java
// C语言
struct Point {
    int x;  // 偏移量0
    int y;  // 偏移量4
};
```

```
内存布局：
[0-3字节]  x
[4-7字节]  y
```

如果不指定顺序或顺序错误：
```java
// ❌ 错误：JNA可能把y映射到偏移量0
public int y;
public int x;

// ✅ 正确：显式指定顺序
@FieldOrder({"x", "y"})
public int x;
public int y;
```

## 3. ByValue vs ByReference

### 3.1 ByValue（按值传递）

**定义：**
```java
public static class ByValue extends MyStruct implements Structure.ByValue {}
```

**使用场景：**
- C函数**返回**结构体值（不是指针）
- C函数**按值**接收结构体参数

**C语言对应：**
```c
// 返回结构体值
MyStruct func();

// 按值传递参数
void process(MyStruct s);  // 拷贝整个结构体
```

**Java映射：**
```java
// 接口定义
MyStruct.ByValue func();
void process(MyStruct.ByValue s);

// 使用
MyStruct.ByValue result = lib.func();
```

### 3.2 ByReference（按引用传递）

**定义：**
```java
public static class ByReference extends MyStruct implements Structure.ByReference {}
```

**使用场景：**
- 显式强调按引用传递（可选）
- 与ByValue区分使用

**C语言对应：**
```c
// 传递指针
void process(MyStruct* s);
```

**Java映射：**
```java
// 接口定义（两种等价）
void process(MyStruct s);
void process(MyStruct.ByReference s);

// 使用
MyStruct s = new MyStruct();
lib.process(s);  // 默认就是按引用
```

### 3.3 对比总结

| Java类型 | C语言对应 | 内存方式 | 性能 | 使用频率 |
|----------|-----------|---------|------|---------|
| `MyStruct` | `MyStruct*` | 指针 | 快 | ⭐⭐⭐ 最常用 |
| `MyStruct.ByValue` | `MyStruct` | 值拷贝 | 慢 | ⭐⭐ 返回值 |
| `MyStruct.ByReference` | `MyStruct*` | 指针 | 快 | ⭐ 可选 |

## 4. 实际例子对照

### 例子1：返回结构体值（div函数）

```c
// C语言
typedef struct {
    int quot;
    int rem;
} div_t;

div_t div(int x, int y);  // 返回结构体值
```

```java
// Java映射
@FieldOrder({"quot", "rem"})
public class DivT extends Structure {
    public int quot;
    public int rem;

    public static class ByValue extends DivT implements Structure.ByValue {}
}

// 接口
DivT.ByValue div(int x, int y);

// 使用
DivT.ByValue result = lib.div(17, 5);
```

### 例子2：传递结构体指针（mktime函数）

```c
// C语言
struct tm {
    int tm_sec;
    int tm_min;
    // ...
};

time_t mktime(struct tm* timer);  // 传递指针
```

```java
// Java映射
public class Tm extends Structure {
    public int tm_sec;
    public int tm_min;
    // ...

    // ByValue是可选的，这里不需要
}

// 接口（不需要ByReference，默认就是按引用）
NativeLong mktime(Tm tm);

// 使用
Tm tm = new Tm();
tm.tm_sec = 30;
tm.tm_min = 15;
tm.write();  // ⭐ 关键：写入native内存
NativeLong timestamp = lib.mktime(tm);
```

### 例子3：返回结构体指针（localtime函数）

```c
// C语言
struct tm* localtime(const time_t* timer);  // 返回指针
```

```java
// Java映射
public class Tm extends Structure {
    // 需要Pointer构造函数
    public Tm(Pointer p) {
        super(p);
        read();
    }
}

// 接口
Pointer localtime(Pointer timer);

// 使用
Pointer tmPtr = lib.localtime(timePtr);
Tm tm = new Tm(tmPtr);  // 从指针构造
```

## 5. write() 和 read() 方法

### write() - 写入native内存

```java
Tm tm = new Tm();
tm.tm_year = 2025;
tm.tm_mon = 0;
tm.write();  // ⭐ 必须调用！将Java字段写入native内存
lib.mktime(tm);  // native函数才能看到数据
```

### read() - 从native内存读取

```java
Tm tm = new Tm();
lib.someFunction(tm);  // native函数修改了tm
tm.read();  // ⭐ 读取修改后的数据
System.out.println(tm.tm_year);  // 看到修改后的值
```

### 自动调用场景
- 从Pointer构造Structure时：**自动read()**
- 传递Structure给native函数前：**自动write()**（如果配置了）
- native函数返回后：**自动read()**（如果配置了）

## 6. 快速参考

### 如何选择ByValue或ByReference？

**问：C函数返回的是什么？**
- `MyStruct` → 用`MyStruct.ByValue`
- `MyStruct*` → 返回`Pointer`，用`new MyStruct(p)`

**问：C函数参数是什么类型？**
- `MyStruct s` → 用`MyStruct.ByValue`
- `MyStruct* s` → 用`MyStruct`（默认）

**问：需要定义内部类吗？**
- 只用于输入输出参数 → 不需要，默认够用
- 需要返回结构体值 → **必须**定义ByValue
- 需要按值传递参数 → **必须**定义ByValue

## 7. 常见错误

### ❌ 错误1：忘记指定字段顺序
```java
public class BadStruct extends Structure {
    public int x;
    public int y;
    // 缺少@FieldOrder或getFieldOrder()
}
```

### ❌ 错误2：返回值不用ByValue
```java
// C: div_t div(int, int);
DivT div(int x, int y);  // ❌ 错误！默认按指针处理
DivT.ByValue div(int x, int y);  // ✅ 正确
```

### ❌ 错误3：忘记调用write()
```java
Tm tm = new Tm();
tm.tm_year = 2024;
lib.mktime(tm);  // ❌ tm字段都是0！
tm.write();  // ✅ 应该先调用write()
lib.mktime(tm);
```

### ❌ 错误4：同时使用注解和重写
```java
@FieldOrder({"x", "y"})  // 重复了！
public class BadStruct extends Structure {
    public int x;
    public int y;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("x", "y");  // 二选一即可！
    }
}
```

## 8. 总结

**最小必需模板：**
```java
@FieldOrder({"field1", "field2"})
public class MyStruct extends Structure {
    public int field1;
    public int field2;
}
```

**如果需要返回结构体值：**
```java
@FieldOrder({"field1", "field2"})
public class MyStruct extends Structure {
    public int field1;
    public int field2;

    public static class ByValue extends MyStruct implements Structure.ByValue {}
}
```

**如果需要从Pointer构造：**
```java
@FieldOrder({"field1", "field2"})
public class MyStruct extends Structure {
    public int field1;
    public int field2;

    public MyStruct(Pointer p) {
        super(p);
        read();
    }
}
```

**完整版（推荐）：**
```java
@FieldOrder({"field1", "field2"})
public class MyStruct extends Structure {
    public int field1;
    public int field2;

    // 默认构造函数
    public MyStruct() {
        super();
    }

    // 从Pointer构造
    public MyStruct(Pointer p) {
        super(p);
        read();
    }

    // 按值传递（如果需要）
    public static class ByValue extends MyStruct implements Structure.ByValue {}

    // 按引用传递（可选）
    public static class ByReference extends MyStruct implements Structure.ByReference {}
}
```
