package cn.clazs.jdk.draft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RemoveEmptyLines {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. 用户输入文件路径
        System.out.print("请输入文件路径（例如 D:/test.md 或 D:/test.txt）：");
        String filePath = scanner.nextLine().trim();

        File file = new File(filePath);

        // 2. 检查文件是否存在
        if (!file.exists()) {
            System.out.println("文件不存在！");
            return;
        }

        // 3. 读取文件内容并统计
        List<String> lines = new ArrayList<>();
        int originalLines = 0; // 原文件总行数
        int emptyLinesRemoved = 0; // 删除的空行数

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                originalLines++;
                // 如果 trim() 后不是空行，则保留
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                } else {
                    emptyLinesRemoved++;
                }
            }
        } catch (IOException e) {
            System.out.println("读取文件失败：" + e.getMessage());
            return;
        }

        // 4. 将处理后的内容覆盖写回原文件
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine(); // 保留原有的换行
            }
        } catch (IOException e) {
            System.out.println("写入文件失败：" + e.getMessage());
            return;
        }

        // 5. 输出处理结果报告
        System.out.println("\n=== 处理结果 ===");
        System.out.println("源文件总行数: " + originalLines);
        System.out.println("删除的空行数: " + emptyLinesRemoved);
        System.out.println("处理后的行数: " + lines.size());
        System.out.println("空行已删除，并覆盖原文件。");
    }
}