package cn.clazs.jdk.draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class D1 {
    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        int len = list.size();
        list.remove(len - 1);
        list.remove(len - 1 - 1);

        List<Integer> synchronizedList = Collections.synchronizedList(list);

        CopyOnWriteArrayList<Integer> copyOnWriteArrayList = new CopyOnWriteArrayList<>(list);

        Object[] o1 = {};
        Object[] o2 = {};
        System.out.println("o1==o2 = " + (o1 == o2));


        ArrayList<String> demoList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            demoList.add(i + " --- ");
            System.out.println("cur size = " + demoList.size());
        }
    }
}
