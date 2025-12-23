package cn.clazs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 测试本地DLL加载
 * 验证从resources加载msvcrt.dll的可行性
 */
public class LocalDllTest {

    /**
     * C标准库接口
     */
    public interface CLibrary extends Library {
        void printf(String format, Object... args);

        int strlen(String str);
    }

    /**
     * 从系统路径加载
     */
    public static CLibrary loadSystemLibrary() {
        return Native.load("msvcrt", CLibrary.class);
    }

    /**
     * 从resources加载
     */
    public static CLibrary loadLocalLibrary() {
        try {
            // 从resources复制到临时文件
            InputStream is = LocalDllTest.class.getClassLoader()
                    .getResourceAsStream("libs/windows/x64/msvcrt.dll");

            if (is == null) {
                System.out.println("❌ 找不到resources中的msvcrt.dll");
                return null;
            }

            // 创建临时文件
            File tempFile = File.createTempFile("msvcrt_local", ".dll");
            tempFile.deleteOnExit();

            // 复制文件
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ 成功复制DLL到: " + tempFile.getAbsolutePath());

            // 加载本地DLL
            return (CLibrary) Native.load(tempFile.getAbsolutePath(), CLibrary.class);

        } catch (Exception e) {
            System.out.println("❌ 加载本地DLL失败: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 本地DLL加载测试 ===");
        System.out.println("当前平台: " + (Platform.isWindows() ? "Windows" : "Other"));
        System.out.println();

        // 1. 测试系统DLL加载
        System.out.println("1. 测试系统msvcrt.dll加载:");
        try {
            CLibrary systemLib = loadSystemLibrary();
            systemLib.printf("✅ 系统DLL加载成功!\n");

            String testStr = "Hello from system DLL";
            int length = systemLib.strlen(testStr);
            System.out.printf("系统DLL计算字符串长度: '%s' = %d\n", testStr, length);
        } catch (Exception e) {
            System.out.println("❌ 系统DLL加载失败: " + e.getMessage());
        }

        System.out.println();

        // 2. 测试本地DLL加载
        System.out.println("2. 测试本地msvcrt.dll加载:");
        try {
            CLibrary localLib = loadLocalLibrary();
            if (localLib != null) {
                localLib.printf("✅ 本地DLL加载成功!\n");

                String testStr = "Hello from local DLL";
                int length = localLib.strlen(testStr);
                System.out.printf("本地DLL计算字符串长度: '%s' = %d\n", testStr, length);
            }
        } catch (Exception e) {
            System.out.println("❌ 本地DLL测试失败: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 结论 ===");
        System.out.println("✅ 系统DLL: 可以直接通过库名加载");
        System.out.println("✅ 本地DLL: 可以通过文件路径加载");
        System.out.println("💡 实习建议: 自定义库文件放在resources/libs/目录下，使用绝对路径加载");
    }
}