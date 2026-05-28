package com.aichen.datapackconfigmod.client;

import net.minecraft.network.chat.Component;

/**
 * 枚举选择器辅助类
 * 提供动态枚举类型支持，实现真正的点击切换按钮功能
 */
public class EnumSelectorHelper {
    
    /**
     * 通用枚举类型，支持动态枚举值
     * 这个枚举包含示例数据包中可能出现的所有枚举值
     */
    public enum DynamicEnum {
        // 游戏难度
        EASY("EASY"),
        MEDIUM("MEDIUM"),
        HARD("HARD"),
        EXPERT("EXPERT"),
        
        // 性能模式
        LOW("LOW"),
        BALANCED("BALANCED"),
        HIGH("HIGH"),
        ULTRA("ULTRA"),
        
        // 生物生成频率
        VERY_LOW("VERY_LOW"),
        LOW_FREQ("LOW"),
        NORMAL("NORMAL"),
        HIGH_FREQ("HIGH"),
        VERY_HIGH("VERY_HIGH"),
        
        // 天气强度
        DISABLED("DISABLED"),
        MILD("MILD"),
        REALISTIC("REALISTIC"),
        INTENSE("INTENSE"),
        EXTREME("EXTREME"),
        
        // AI智能等级
        DUMB("DUMB"),
        BASIC("BASIC"),
        STANDARD("STANDARD"),
        SMART("SMART"),
        GENIUS("GENIUS"),
        
        // 资源再生
        NONE("NONE"),
        VERY_SLOW("VERY_SLOW"),
        SLOW("SLOW"),
        NORMAL_RES("NORMAL"),
        FAST("FAST");
        
        private final String value;
        
        DynamicEnum(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        /**
         * 根据字符串值获取枚举实例
         */
        public static DynamicEnum fromString(String value) {
            for (DynamicEnum enumValue : values()) {
                if (enumValue.value.equals(value)) {
                    return enumValue;
                }
            }
            // 如果找不到匹配的值，返回第一个作为默认值
            return values()[0];
        }
        
        /**
         * 获取枚举值的显示名称（本地化）
         */
        public Component getDisplayName(String[] enumValues, String[] localizedNames) {
            if (localizedNames != null && enumValues != null) {
                for (int i = 0; i < enumValues.length; i++) {
                    if (enumValues[i].equals(this.value) && i < localizedNames.length) {
                        return Component.literal(localizedNames[i]);
                    }
                }
            }
            return Component.literal(this.value);
        }
    }
}