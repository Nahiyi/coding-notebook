#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Java Runtime.exec 调用的Python示例脚本
演示接收参数并返回结果
"""

import sys
import json
from datetime import datetime


def print_info():
    """打印基本信息"""
    print("*" * 50)
    print("Python脚本执行成功！")
    print(f"Python版本: {sys.version}")
    print(f"当前时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"脚本名称: {sys.argv[0]}")
    print("*" * 50)


def greet_with_args():
    """根据参数进行个性化问候"""
    if len(sys.argv) > 1:
        name = sys.argv[1] if len(sys.argv) > 1 else "Guest"
        age = sys.argv[2] if len(sys.argv) > 2 else "Unknown"
        job = sys.argv[3] if len(sys.argv) > 3 else "Unknown"

        print(f"\n你好, {name}!")
        print(f"年龄: {age}")
        print(f"职业: {job}")

        # 计算出生年份
        try:
            current_year = datetime.now().year
            birth_year = current_year - int(age)
            print(f"出生年份: 约 {birth_year} 年")
        except ValueError:
            print("出生年份: 无法计算（年龄不是有效数字）")

    else:
        print("\n没有提供参数，使用默认问候。")
        print("Usage: python demo.py <name> <age> <job>")

def generate_json_response():
    """生成JSON格式的响应"""
    if len(sys.argv) > 1:
        response = {
            "status": "success",
            "timestamp": datetime.now().isoformat(),
            "user_info": {
                "name": sys.argv[1] if len(sys.argv) > 1 else "Guest",
                "age": sys.argv[2] if len(sys.argv) > 2 else None,
                "job": sys.argv[3] if len(sys.argv) > 3 else None
            },
            "message": "数据来自Python脚本"
        }
        print(f"\n【JSON响应】")
        print(json.dumps(response, ensure_ascii=False, indent=2))


def main():
    """主函数"""
    print_info()

    if len(sys.argv) > 1:
        greet_with_args()
        generate_json_response()
    else:
        print("\n【无参数模式】")
        print("请提供参数以查看更多功能")

    print("\n脚本执行完成！")
    return 0


if __name__ == "__main__":
    sys.exit(main())
