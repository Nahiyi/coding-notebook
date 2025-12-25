# Runtime.exec 原理详解

## 一图看懂原理

### 命令行执行 vs Runtime.exec 执行

```
┌─────────────────────────────────────────────────────────────────┐
│                        命令行执行流程                              │
└─────────────────────────────────────────────────────────────────┘

用户输入: python demo.py 张三 25
    ↓
【Shell解析】
    ↓
Shell找到python.exe
Shell创建python.exe进程
Shell把demo.py、张三、25传给python
    ↓
Python进程执行

┌─────────────────────────────────────────────────────────────────┐
│                     Runtime.exec执行流程                          │
└─────────────────────────────────────────────────────────────────┘

Java代码: runtime.exec(cmd数组)
    ↓
【直接创建进程】（不经过Shell！）
    ↓
操作系统读取cmd[0] = python.exe
操作系统创建python.exe进程
操作系统把cmd[1+]传给python
    ↓
Python进程执行
```

## cmd数组的本质

### 这不是"约定俗成"，而是操作系统API的设计！

```java
String[] cmd = {
    "D:\\python.exe",    // cmd[0]: 可执行文件路径
    "demo.py",           // cmd[1]: 第1个参数
    "张三",              // cmd[2]: 第2个参数
    "25"                 // cmd[3]: 第3个参数
};
runtime.exec(cmd);
```

**等价于操作系统级别的调用：**

**Windows API (CreateProcess):**
```c
CreateProcess(
    "D:\\python.exe",           // lpApplicationName (可执行文件)
    "demo.py 张三 25",          // lpCommandLine (命令行参数)
    ...                         // 其他参数
);
```

**Linux API (execve):**
```c
char *argv[] = {
    "python",                   // argv[0] (惯例，程序名)
    "demo.py",                  // argv[1]
    "张三",                     // argv[2]
    "25",                       // argv[3]
    NULL                        // argv[4] 必须以NULL结尾
};
execve("D:\\python.exe", argv, envp);
```

## 为什么感觉是"约定俗成"？

因为**所有程序**都遵循这个规则：

| 程序 | cmd[0] | cmd[1] | cmd[2] | cmd[3] | cmd[4+] |
|------|--------|--------|--------|--------|---------|
| Python | python.exe | script.py | arg1 | arg2 | ... |
| Node | node.exe | script.js | arg1 | arg2 | ... |
| Java | java.exe | -cp | classpath | MainClass | args... |
| Ruby | ruby.exe | script.rb | arg1 | arg2 | ... |
| Chrome | chrome.exe | url1 | url2 | ... | ... |

这不是Java规定的，而是**操作系统进程创建机制**决定的！

## 你原来的疑问：cd 命令去哪了？

### ❌ 错误理解

```bash
# 命令行写法
cd D:\develop\Python3.13
python demo.py

# 以为Java也要这么写
String[] cmd = {"cd", "D:\\develop\\Python3.13", "python", "demo.py"}; // 错误！
```

**问题：**
- `cd` 是**shell的内建命令**，不是可执行文件
- 你无法找到 `cd.exe` 这个文件
- Runtime.exec 不经过shell，所以不认识 `cd`

### ✅ 正确理解

```bash
# 命令行写法（简化版）
python demo.py

# Java写法：直接指定完整路径
String[] cmd = {
    "D:\\develop\\Python3.13\\python.exe",  // 完整路径，不需要cd
    "demo.py"
};
runtime.exec(cmd);
```

### 如果一定要用cd、&&等shell命令

需要显式调用shell：

```java
// Windows: 通过cmd.exe执行
String[] cmd = {
    "cmd",
    "/c",
    "cd D:\\develop && dir && python demo.py"
};
runtime.exec(cmd);

// Linux: 通过sh执行
String[] cmd = {
    "/bin/sh",
    "-c",
    "cd /path && ls && python demo.py"
};
runtime.exec(cmd);
```

## 各类调用示例

### 1. Python脚本

```java
// 命令行: python demo.py 张三 25
String[] cmd = {
    "D:\\Python3.13\\python.exe",  // 可执行文件
    "demo.py",                     // 参数1
    "张三",                        // 参数2
    "25"                           // 参数3
};

// Python接收: sys.argv = ['demo.py', '张三', '25']
```

### 2. Node.js脚本

```java
// 命令行: node server.js 8080
String[] cmd = {
    "C:\\Program Files\\nodejs\\node.exe",  // 可执行文件
    "server.js",                            // 参数1
    "8080"                                  // 参数2
};

// Node.js接收: process.argv = ['node路径', 'server.js', '8080']
```

### 3. Java程序

```java
// 命令行: java -cp ./libs com.example.Main hello
String[] cmd = {
    "java",                    // 可执行文件
    "-cp",                     // 参数1: classpath选项
    "./libs",                  // 参数2: classpath值
    "com.example.Main",        // 参数3: 主类
    "hello"                    // 参数4: 传给main的参数
};

// Java的main接收: args = ["hello"]
```

### 4. 直接执行系统命令

```java
// Windows: 打开记事本
String[] cmd = {"notepad.exe", "test.txt"};
runtime.exec(cmd);

// Windows: 列出文件
String[] cmd = {"cmd", "/c", "dir"};
runtime.exec(cmd);

// Linux: 列出文件
String[] cmd = {"/bin/ls", "-l"};
runtime.exec(cmd);
```

## Runtime.exec vs ProcessBuilder

### Runtime.exec（老式API）

```java
String[] cmd = {"python", "demo.py"};
Process process = Runtime.getRuntime().exec(cmd);
```

**优点：**
- 简单快捷
- 适合简单场景

**缺点：**
- 不能设置工作目录
- 不能修改环境变量
- 不能重定向流

### ProcessBuilder（推荐）

```java
ProcessBuilder pb = new ProcessBuilder("python", "demo.py");

// 设置工作目录
pb.directory(new File("/path/to/workdir"));

// 设置环境变量
pb.environment().put("PYTHONPATH", "/custom/path");

// 合并错误流
pb.redirectErrorStream(true);

// 启动进程
Process process = pb.start();
```

**优点：**
- 功能更强大
- API更清晰
- 推荐使用

## 常见错误与解决

### 错误1: 路径中有空格

```java
// ❌ 错误
String[] cmd = {"C:\\Program Files\\python.exe", "demo.py"};

// ✅ 正确：加引号
String[] cmd = {"\"C:\\Program Files\\python.exe\"", "demo.py"};

// ✅ 或者：ProcessBuilder会自动处理
ProcessBuilder pb = new ProcessBuilder("C:\\Program Files\\python.exe", "demo.py");
```

### 错误2: 找不到可执行文件

```java
// ❌ 错误：相对路径可能找不到
String[] cmd = {"python.exe", "demo.py"};

// ✅ 正确：使用绝对路径或完整PATH
String[] cmd = {"D:\\Python3.13\\python.exe", "demo.py"};

// ✅ 或者：依赖系统PATH
String[] cmd = {"python", "demo.py"}; // 需要python在PATH中
```

### 错误3: 中文乱码

```java
// ❌ 错误：不指定编码
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream())
);

// ✅ 正确：指定编码
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream(), "GBK") // Windows用GBK
);
```

### 错误4: 死锁（输出量大时）

```java
// ❌ 错误：只读stdout，如果stderr满了会死锁
BufferedReader stdout = ...;
while ((line = stdout.readLine()) != null) {
    // 只读stdout
}

// ✅ 正确：分别读取stdout和stderr
new Thread(() -> readStdout()).start();
new Thread(() -> readStderr()).start();

// ✅ 或者：合并流
processBuilder.redirectErrorStream(true);
```

## 参数传递验证

### Python端接收参数

```python
# demo.py
import sys

print(f"程序名: {sys.argv[0]}")  # demo.py
print(f"参数1: {sys.argv[1]}")   # 第一个参数
print(f"参数2: {sys.argv[2]}")   # 第二个参数
print(f"参数总数: {len(sys.argv)}")
```

### Java端传递参数

```java
String[] cmd = {
    "python.exe",
    "demo.py",      // sys.argv[0]
    "hello",        // sys.argv[1]
    "world"         // sys.argv[2]
};
```

### 执行结果

```
程序名: demo.py
参数1: hello
参数2: world
参数总数: 3
```

## 总结

1. **Runtime.exec 不是执行命令行字符串**
   → 而是直接创建操作系统进程

2. **cmd数组不是"约定俗成"**
   → 而是操作系统API的标准参数传递方式

3. **cmd[0]必须是可执行文件**
   → 必须能找到对应的.exe或可执行文件

4. **不需要cd、&&等shell命令**
   → 直接指定可执行文件的完整路径

5. **不同解释器遵循相同规则**
   → Python、Node、Java等都一样

6. **推荐使用ProcessBuilder**
   → 更强大、更清晰、更安全

**Runtime.exec直接和操作系统打交道，不经过Shell！**
