package cn.clazs.jna.runtimeexec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Runtime.exec 调用Python脚本示例
 * 演示如何使用Java的Runtime类执行本地Python脚本
 */
public class PythonRuntimeExecDemo {

    // Python解释器路径
    private static final String PYTHON_PATH = "D:\\develop\\Python3.13\\python.exe";

    // Python脚本路径
    private static final String SCRIPT_PATH = "jna/src/main/resources/demo.py";

    // 字符编码：Windows使用GBK，Linux/Mac使用UTF-8
    private static final String CHARSET_NAME = System.getProperty("os.name").toLowerCase().contains("win")
            ? "GBK" : "UTF-8";

    /**
     * 基础示例：调用Python脚本
     */
    public static void basicExample() {
        try {
            // 获取Runtime对象
            Runtime runtime = Runtime.getRuntime();

            // 构建命令：python.exe 脚本路径
            String[] cmd = {PYTHON_PATH, SCRIPT_PATH};

            // 执行命令
            Process process = runtime.exec(cmd);

            // 获取脚本输出（使用系统默认编码）
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET_NAME));

            String line;
            System.out.println("=== Python脚本输出 ===");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待进程结束并获取退出码
            int exitCode = process.waitFor();
            System.out.println("=======================");
            System.out.println("脚本执行完成，退出码: " + exitCode);

            reader.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("执行Python脚本时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 带参数的示例：向Python脚本传递参数
     */
    public static void withArgumentsExample() {
        try {
            Runtime runtime = Runtime.getRuntime();

            // 构建命令：python.exe 脚本路径 参数1 参数2
            String[] cmd = {
                PYTHON_PATH,
                SCRIPT_PATH,
                "张三",
                "18",
                "Java开发工程师"
            };

            Process process = runtime.exec(cmd);

            // 读取正常输出（使用系统默认编码）
            InputStream is = process.getInputStream();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(is, CHARSET_NAME));

            // 读取错误输出（使用系统默认编码）
            BufferedReader stdError = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), CHARSET_NAME));

            System.out.println("=== 带参数的Python脚本输出 ===");
            String line;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            // 如果有错误输出，也打印出来
            StringBuilder errorOutput = new StringBuilder();
            while ((line = stdError.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            System.out.println("=======================");
            System.out.println("退出码: " + exitCode);

            if (errorOutput.length() > 0) {
                System.err.println("错误输出:\n" + errorOutput);
            }

            stdInput.close();
            stdError.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 实用工具方法：执行Python命令并返回结果
     */
    public static String executePython(String scriptPath, String... args) {
        try {
            String[] cmd = new String[2 + args.length];
            cmd[0] = PYTHON_PATH;
            cmd[1] = scriptPath;
            System.arraycopy(args, 0, cmd, 2, args.length);

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);

            // 拿到runtime执行exec的输出作为输入流读取
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, CHARSET_NAME));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            reader.close();

            if (exitCode == 0) {
                return result.toString();
            } else {
                return "Error: Script exited with code " + exitCode;
            }

        } catch (IOException | InterruptedException e) {
            return "Error: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        // basicExample();

        withArgumentsExample();

        // String result = executePython(SCRIPT_PATH, "李四", "30", "架构师");
        // System.out.println(result);
    }
}
