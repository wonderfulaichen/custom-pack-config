package com.aichen.datapackconfigmod.client;

import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

/**
 * 动态工具提示助手
 * 提供按Ctrl键显示详细信息的动态tooltip功能
 */
public class DynamicTooltipHelper {
    
    /**
     * 为配置项生成动态工具提示
     * @param settings 数据包设置
     * @param key 配置键
     * @param baseTooltip 基础工具提示
     * @return 动态工具提示组件列表
     */
    public static List<Component> createDynamicTooltip(DatapackSettings settings, String key, String baseTooltip) {
        MutableComponent tooltip = Component.literal(baseTooltip);
        
        // 获取配置位置列表
        List<DatapackSettings.ConfigLocation> locations = settings.getConfigLocations(key);
        
        // 如果有多个位置，添加批量修改信息
        if (locations.size() > 1) {
            // 检查是否按下了Ctrl键
            boolean isCtrlPressed = isCtrlKeyPressed();
            
            if (isCtrlPressed) {
                // 按Ctrl键时显示详细位置信息
                tooltip.append("\n\n");
                tooltip.append(Component.literal("批量修改位置 (" + locations.size() + "个):").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF00").result().orElse(null))));
                
                for (int i = 0; i < locations.size() && i < 5; i++) {
                    DatapackSettings.ConfigLocation loc = locations.get(i);
                    String locDesc = loc.getDescription() != null ? loc.getDescription() : "";
                    
                    tooltip.append("\n");
                    MutableComponent locationText = Component.literal("- " + loc.getFilePath());
                    if (locDesc != null && !locDesc.isEmpty()) {
                        locationText.append(" (" + locDesc + ")");
                    }
                    tooltip.append(locationText.withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").result().orElse(null))));
                }
                
                if (locations.size() > 5) {
                    tooltip.append("\n");
                    tooltip.append(Component.literal("... 还有 " + (locations.size() - 5) + " 个位置").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").result().orElse(null))));
                }
            } else {
                // 未按Ctrl键时显示提示信息
                tooltip.append("\n\n");
                tooltip.append(Component.literal("按住 Ctrl 键查看 " + locations.size() + " 个修改位置").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF00").result().orElse(null))));
            }
        }
        
        return List.of(tooltip);
    }
    
    /**
     * 检查Ctrl键是否按下
     * MC 1.21.10: Screen.hasControlDown() 已移除，改用 Minecraft.hasControlDown()
     */
    private static boolean isCtrlKeyPressed() {
        try {
            return Minecraft.getInstance().hasControlDown();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 为配置项生成通用动态工具提示（可扩展到其他用途）
     * @param baseTooltip 基础工具提示
     * @param detailedInfo 详细信息（按Ctrl键时显示）
     * @return 动态工具提示组件列表
     */
    public static List<Component> createGenericDynamicTooltip(String baseTooltip, String detailedInfo) {
        MutableComponent tooltip = Component.literal(baseTooltip);

        if (detailedInfo != null && !detailedInfo.isEmpty()) {
            boolean isCtrlPressed = isCtrlKeyPressed();

            if (isCtrlPressed) {
                // 按Ctrl键时显示详细信息
                tooltip.append("\n\n");
                tooltip.append(Component.literal(detailedInfo).withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").result().orElse(null))));
            } else {
                // 未按Ctrl键时显示提示信息
                tooltip.append("\n\n");
                tooltip.append(Component.literal("按住 Ctrl 键查看更多信息").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF00").result().orElse(null))));
            }
        }

        return List.of(tooltip);
    }
}