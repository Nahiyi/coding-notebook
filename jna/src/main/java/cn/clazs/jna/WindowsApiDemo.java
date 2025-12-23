package cn.clazs.jna;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows API JNA演示
 * 展示如何调用Windows系统的原生API
 *
 * 注意：此示例仅适用于Windows平台
 */
public class WindowsApiDemo {

    /**
     * Windows Kernel32 API接口
     * 包含系统核心功能
     */
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.UNICODE_OPTIONS);

        // 获取系统启动时间（毫秒）
        int GetTickCount();

        // 获取当前进程ID
        int GetCurrentProcessId();

        // 获取当前线程ID
        int GetCurrentThreadId();

        // 系统时间结构
        @Structure.FieldOrder({"wYear", "wMonth", "wDayOfWeek", "wDay", "wHour", "wMinute", "wSecond", "wMilliseconds"})
        class SYSTEMTIME extends Structure {
            public short wYear;
            public short wMonth;
            public short wDayOfWeek;
            public short wDay;
            public short wHour;
            public short wMinute;
            public short wSecond;
            public short wMilliseconds;

            @Override
            public String toString() {
                return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d",
                    wYear, wMonth, wDay, wHour, wMinute, wSecond, wMilliseconds);
            }
        }

        void GetSystemTime(SYSTEMTIME lpSystemTime);
        void GetLocalTime(SYSTEMTIME lpSystemTime);

        // 内存操作
        Pointer GlobalAlloc(int uFlags, int dwBytes);
        Pointer GlobalFree(Pointer hMem);
    }

    /**
     * Windows User32 API接口
     * 包含用户界面相关功能
     */
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.UNICODE_OPTIONS);

        // 获取屏幕尺寸
        int GetSystemMetrics(int nIndex);

        // 消息框
        int MessageBoxA(Pointer hWnd, String lpText, String lpCaption, int uType);

        // 获取窗口信息
        Pointer GetForegroundWindow();
        int GetWindowTextW(Pointer hWnd, char[] lpString, int nMaxCount);

        // 系统常量
        int SM_CXSCREEN = 0;  // 屏幕宽度
        int SM_CYSCREEN = 1;  // 屏幕高度
        int MB_OK = 0;
        int MB_ICONINFORMATION = 0x00000040;
    }

    public static void main(String[] args) {
        if (!Platform.isWindows()) {
            System.out.println("此示例仅适用于Windows平台");
            return;
        }

        System.out.println("=== Windows API JNA Demo ===");

        // 1. 系统信息演示
        demonstrateSystemInfo();

        // 2. 时间信息演示
        demonstrateTimeInfo();

        // 3. 屏幕信息演示
        demonstrateScreenInfo();

        // 4. 内存操作演示
        demonstrateMemoryOperations();

        System.out.println("\n=== Windows API演示完成 ===");
    }

    private static void demonstrateSystemInfo() {
        System.out.println("\n1. 系统信息:");

        Kernel32 kernel32 = Kernel32.INSTANCE;
        int tickCount = kernel32.GetTickCount();
        int processId = kernel32.GetCurrentProcessId();
        int threadId = kernel32.GetCurrentThreadId();

        System.out.println("系统启动时间（毫秒）: " + tickCount);
        System.out.println("当前进程ID: " + processId);
        System.out.println("当前线程ID: " + threadId);
    }

    private static void demonstrateTimeInfo() {
        System.out.println("\n2. 系统时间:");

        Kernel32 kernel32 = Kernel32.INSTANCE;
        Kernel32.SYSTEMTIME systemTime = new Kernel32.SYSTEMTIME();
        Kernel32.SYSTEMTIME localTime = new Kernel32.SYSTEMTIME();

        kernel32.GetSystemTime(systemTime);
        kernel32.GetLocalTime(localTime);

        System.out.println("UTC时间: " + systemTime);
        System.out.println("本地时间: " + localTime);
    }

    private static void demonstrateScreenInfo() {
        System.out.println("\n3. 屏幕信息:");

        User32 user32 = User32.INSTANCE;
        int screenWidth = user32.GetSystemMetrics(User32.SM_CXSCREEN);
        int screenHeight = user32.GetSystemMetrics(User32.SM_CYSCREEN);

        System.out.println("屏幕分辨率: " + screenWidth + " x " + screenHeight);

        // 显示消息框（注释掉以避免弹窗干扰）
        // user32.MessageBoxA(null, "JNA调用Windows API成功！", "JNA Demo", User32.MB_OK | User32.MB_ICONINFORMATION);
        System.out.println("消息框功能已注释（避免弹窗干扰）");
    }

    private static void demonstrateMemoryOperations() {
        System.out.println("\n4. 内存操作:");

        Kernel32 kernel32 = Kernel32.INSTANCE;

        // 分配内存
        Pointer memPtr = kernel32.GlobalAlloc(0x0040, 1024); // GMEM_MOVEABLE
        System.out.println("分配的内存地址: " + memPtr);

        if (memPtr != null) {
            // 释放内存
            kernel32.GlobalFree(memPtr);
            System.out.println("内存已释放");
        } else {
            System.out.println("内存分配失败");
        }
    }
}