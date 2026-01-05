package cn.clazs.jdk.draft;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ArrayListCapacityInspector {

    public static void main(String[] args) throws Exception {
        // 实验1: 无参构造器
        System.out.println("============== 实验 1: new ArrayList<>() ==============");
        testGrowthMechanism(new ArrayList<>());

        System.out.println("\n\n");

        // 实验2: 指定容量为0的构造器
        System.out.println("============== 实验 2: new ArrayList<>(0) ==============");
        testGrowthMechanism(new ArrayList<>(0));
    }

    public static void testGrowthMechanism(ArrayList<String> list) throws Exception {
        // 通过反射获取 ArrayList 类中的 elementData 字段
        Class<?> clazz = list.getClass();
        Field elementDataField = clazz.getDeclaredField("elementData");
        
        // 设置访问权限（因为 elementData 是 transient Object[]，通常不是 public 的）
        elementDataField.setAccessible(true);

        // 打印初始状态
        printState("初始状态", list, elementDataField);

        // 模拟添加元素并观察扩容
        // 添加 7 个元素足以看清前期的扩容差异
        for (int i = 1; i <= 7; i++) {
            list.add("Element-" + i);
            printState("添加第 " + i + " 个元素后", list, elementDataField);
        }
    }

    private static void printState(String action, ArrayList<?> list, Field field) throws Exception {
        // 反射获取当前的数组对象
        Object[] elementData = (Object[]) field.get(list);
        int capacity = elementData.length; // 物理容量
        int size = list.size();            // 逻辑大小
        
        System.out.printf("%-15s | 逻辑size: %d | 物理capacity: %d%n", action, size, capacity);
    }
}