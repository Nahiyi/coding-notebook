package cn.clazs.jdk.dynamicproxy.jdkdynamic;

/**
 * 被代理的接口
 */
public interface UserService {
    void addUser(String username);

    void deleteUser(String username);
}
