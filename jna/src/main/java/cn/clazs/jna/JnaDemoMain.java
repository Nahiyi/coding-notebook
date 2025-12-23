package cn.clazs.jna;

import com.sun.jna.Platform;
import com.sun.jna.Native;
import com.sun.jna.Library;

/**
 * JNA演示程序主入口
 * 统一运行所有JNA演示示例
 */
public class JnaDemoMain {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("       JNA (Java Native Access) 演示程序");
        System.out.println("==========================================");
        System.out.println("当前平台: " + Platform.getOSType());
        System.out.println("Java版本: " + System.getProperty("java.version"));
        System.out.println("JNA版本: " + Native.class.getPackage().getImplementationVersion());
        System.out.println("==========================================");

        try {
            // 1. Hello World 基础演示
            System.out.println("\n🚀 启动基础演示...");
            HelloWorld.main(new String[]{});

            // 2. Windows API 演示（仅Windows）
            if (Platform.isWindows()) {
                System.out.println("\n🪟 启动Windows API演示...");
                WindowsApiDemo.main(new String[]{});
            } else {
                System.out.println("\n⚠️  跳过Windows API演示（非Windows平台）");
            }

            // 3. 结构体演示
            System.out.println("\n🏗️  启动结构体演示...");
            StructureDemo.main(new String[]{});

            // 4. 高级功能演示
            System.out.println("\n🔧 启动高级功能演示...");
            AdvancedDemo.main(new String[]{});

            System.out.println("\n==========================================");
            System.out.println("✅ 所有JNA演示运行完成！");
            System.out.println("==========================================");
            System.out.println("\n📚 学习要点总结:");
            System.out.println("1. JNA通过Library接口映射本地库函数");
            System.out.println("2. 使用Native.load动态加载本地库");
            System.out.println("3. 结构体通过继承Structure类定义");
            System.out.println("4. 回调函数通过实现Callback接口实现");
            System.out.println("5. 内存管理使用Memory和Pointer类");
            System.out.println("6. ByReference类用于模拟指针参数");
            System.out.println("7. 跨平台兼容性通过Platform类实现");

        } catch (Exception e) {
            System.err.println("❌ 演示运行出错: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n🎯 实习应用建议:");
        System.out.println("- 理解JNA与JNI的区别和应用场景");
        System.out.println("- 掌握基础数据类型映射规则");
        System.out.println("- 熟悉结构体和回调函数的使用");
        System.out.println("- 注意内存管理和性能优化");
        System.out.println("- 学习处理平台差异性");
    }

    /**
     * 验证JNA环境是否正确配置
     */
    public static boolean validateJnaEnvironment() {
        try {
            // 测试JNA基本功能
            Library testLib = Native.load("msvcrt", Library.class);
            return true;
        } catch (Exception e) {
            System.err.println("JNA环境验证失败: " + e.getMessage());
            return false;
        }
    }
}