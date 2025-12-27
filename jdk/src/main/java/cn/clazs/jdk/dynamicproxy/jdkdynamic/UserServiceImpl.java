package cn.clazs.jdk.dynamicproxy.jdkdynamic;

/**
 * 真实对象类（被代理的对象）
 */
public class UserServiceImpl implements UserService {
    @Override
    public void addUser(String username) {
        System.out.println("=== 真实对象执行: 添加用户 " + username + " ===");
    }

    @Override
    public void deleteUser(String username) {
        System.out.println("=== 真实对象执行: 删除用户 " + username + " ===");
    }
}
