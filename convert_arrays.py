#!/usr/bin/env python
"""将数据包JSON中的 points 和 placement 数组转换为英文数字对象格式"""
import json
import os
import re

EXTRACTED = r"D:\Users\qq274\Desktop\开发文件\模组开发\数据包配置mod\epicterrain_extracted"

# 英文数字映射
NUM_WORDS = [
    "zero", "one", "two", "three", "four", "five",
    "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen",
    "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
]

def array_to_object(arr):
    """将JSON数组转换为英文数字为key的对象"""
    result = {}
    for i, item in enumerate(arr):
        key = NUM_WORDS[i] if i < len(NUM_WORDS) else str(i)
        result[key] = recursive_convert(item)
    return result

def recursive_convert(data):
    """递归转换JSON中的数组"""
    if isinstance(data, dict):
        new_dict = {}
        for k, v in data.items():
            if k in ("points", "placement") and isinstance(v, list):
                new_dict[k] = array_to_object(v)
            else:
                new_dict[k] = recursive_convert(v)
        return new_dict
    elif isinstance(data, list):
        # 仅当列表中包含 dict 且有 "location" 或 "type" 特征时转换
        # 这里我们只转换顶层 points/placement 中的数组元素
        # 嵌套的普通列表不转换
        return [recursive_convert(item) for item in data]
    else:
        return data

def main():
    # 需要转换的特定键
    TARGET_KEYS = {"points", "placement"}
    
    for root, dirs, files in os.walk(EXTRACTED):
        for fname in files:
            if not fname.endswith(".json"):
                continue
            
            fpath = os.path.join(root, fname)
            with open(fpath, "r", encoding="utf-8") as f:
                content = f.read()
            
            # 快速检查是否包含目标键
            if not any(k in content for k in TARGET_KEYS):
                continue
            
            try:
                data = json.loads(content)
            except json.JSONDecodeError:
                print(f"  [跳过] JSON解析失败: {fpath}")
                continue
            
            # 递归转换
            new_data = recursive_convert(data)
            
            # 写回
            with open(fpath, "w", encoding="utf-8") as f:
                json.dump(new_data, f, indent=2)
            
            rel_path = os.path.relpath(fpath, EXTRACTED)
            print(f"  [已转换] {rel_path}")

if __name__ == "__main__":
    print("开始转换数据包 JSON 数组格式...")
    main()
    print("完成！")
