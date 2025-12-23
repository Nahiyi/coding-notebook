package cn.clazs.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 自定义库加载演示
 * 模拟实习中调用resources下的DLL/SO文件
 */
public class CustomLibraryDemo {

    /**
     * 自定义硬件SDK接口
     */
    public interface HardwareSDK extends Library {
        // 初始化设备
        int initialize();

        // 读取设备数据
        int readData(byte[] buffer, int bufferSize);

        // 关闭设备
        int close();
    }

    /**
     * 自定义图像处理库接口
     */
    public interface ImageProcessLib extends Library {
        // 图像增强
        int enhanceImage(byte[] inputImage, int width, int height, byte[] outputImage);

        // 图像压缩
        int compressImage(byte[] inputImage, int inputSize, byte[] outputImage, int[] outputSize);
    }

    /**
     * 从resources加载库文件
     */
    public static Library loadFromResources(String libraryName, String libraryPath, Class<? extends Library> interfaceClass) {
        try {
            // 1. 构建资源路径
            String resourcePath = "libs/" + libraryPath;

            // 2. 从resources复制到临时文件
            InputStream is = CustomLibraryDemo.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new RuntimeException("找不到资源文件: " + resourcePath);
            }

            // 3. 创建临时文件
            File tempFile = File.createTempFile(libraryName, getLibraryExtension());
            tempFile.deleteOnExit();

            // 4. 复制库文件
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 5. 加载库
            return Native.load(tempFile.getAbsolutePath(), interfaceClass);

        } catch (Exception e) {
            throw new RuntimeException("加载库失败: " + libraryName, e);
        }
    }

    /**
     * 获取系统对应的库文件扩展名
     */
    private static String getLibraryExtension() {
        if (Platform.isWindows()) {
            return ".dll";
        } else if (Platform.isMac()) {
            return ".dylib";
        } else {
            return ".so";
        }
    }

    /**
     * 平台特定的库加载
     */
    public static Library loadPlatformLibrary(String libraryBaseName, Class<? extends Library> interfaceClass) {
        String libraryPath;

        if (Platform.isWindows()) {
            libraryPath = "windows/" + libraryBaseName + ".dll";
        } else if (Platform.isMac()) {
            libraryPath = "mac/" + libraryBaseName + ".dylib";
        } else {
            libraryPath = "linux/" + "lib" + libraryBaseName + ".so";
        }

        return loadFromResources(libraryBaseName, libraryPath, interfaceClass);
    }

    public static void main(String[] args) {
        System.out.println("=== 自定义库加载演示 ===");

        try {
            // 模拟加载硬件SDK
            System.out.println("1. 加载硬件SDK...");
            HardwareSDK hardwareSDK = (HardwareSDK) loadPlatformLibrary("hardware_sdk", HardwareSDK.class);
            System.out.println("硬件SDK加载成功: " + hardwareSDK);

            // 模拟调用硬件SDK
            // int result = hardwareSDK.initialize();
            // System.out.println("设备初始化结果: " + result);

            // 模拟加载图像处理库
            System.out.println("\n2. 加载图像处理库...");
            ImageProcessLib imageLib = (ImageProcessLib) loadPlatformLibrary("image_process", ImageProcessLib.class);
            System.out.println("图像处理库加载成功: " + imageLib);

            // 模拟调用图像处理
            // byte[] input = getImageData();
            // byte[] output = new byte[input.length];
            // int result = imageLib.enhanceImage(input, width, height, output);
            // System.out.println("图像增强结果: " + result);

        } catch (Exception e) {
            System.out.println("库加载失败（这是正常的，因为resources目录下没有实际的库文件）");
            System.out.println("错误信息: " + e.getMessage());
        }

        System.out.println("\n=== 实习应用建议 ===");
        System.out.println("1. 将DLL/SO文件放在resources/libs/对应平台目录下");
        System.out.println("2. 使用loadFromResources()方法从resources加载");
        System.out.println("3. 注意32位/64位库与JDK版本的匹配");
        System.out.println("4. 处理库依赖关系（如vc_redist等运行时库）");
        System.out.println("5. 考虑库文件的版本兼容性");

        // 演示实际的目录结构
        System.out.println("\n=== 建议的项目结构 ===");
        System.out.println("src/main/resources/");
        System.out.println("├── libs/");
        System.out.println("│   ├── windows/");
        System.out.println("│   │   ├── x64/");
        System.out.println("│   │   │   └── hardware_sdk.dll");
        System.out.println("│   │   └── x86/");
        System.out.println("│   │       └── hardware_sdk.dll");
        System.out.println("│   └── linux/");
        System.out.println("│       ├── x64/");
        System.out.println("│       │   └── libhardware_sdk.so");
        System.out.println("│       └── x86/");
        System.out.println("│           └── libhardware_sdk.so");
    }
}