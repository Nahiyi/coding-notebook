package cn.clazs.jdk.dynamicproxy.jdkdynamic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * InvocationHandler实现类
 * 这是JDK动态代理的核心角色之一，负责方法拦截
 */
public class MyInvocationHandler implements InvocationHandler {

    // 持有被代理对象的引用
    private Object target;

    public MyInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     * 核心方法：代理对象的方法调用都会到这里
     *
     * @param proxy  代理对象本身
     * @param method 被调用的方法
     * @param args   方法参数
     * @return 方法返回值
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println(">>> [代理前置] 方法名: " + method.getName());

        // 调用真实对象的方法
        Object result = method.invoke(target, args);

        System.out.println(">>> [代理后置] 方法执行完毕\n");

        return result;
    }
}
