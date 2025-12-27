package cn.clazs.jdk.dynamicproxy.jdkdynamic;

import java.lang.reflect.Proxy;

/**
 * JDK动态代理演示类
 *
 * 核心角色：
 * 1. Proxy：用于创建代理对象
 * 2. InvocationHandler：用于处理代理对象的方法调用
 */
public class JdkDynamicProxyDemo {
    public static void main(String[] args) {
        // 1. 创建真实对象
        UserService realObject = new UserServiceImpl();

        // 2. 创建InvocationHandler（传入真实对象）
        MyInvocationHandler handler = new MyInvocationHandler(realObject);

        // 3. 创建代理对象（这是JDK动态代理的核心）
        //    Proxy.newProxyInstance(类加载器, 接口数组, InvocationHandler)
        UserService proxy = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),  // 类加载器
                new Class[]{UserService.class},      // 被代理的接口
                handler                              // InvocationHandler
        );

        System.out.println("##### 直接调用真实对象 #####");
        realObject.addUser("张三");

        System.out.println("\n##### 通过代理对象调用 #####");
        proxy.addUser("李四");
        proxy.deleteUser("王五");
    }
}
