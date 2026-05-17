package com.aichen.datapackconfigmod.test;

import com.aichen.datapackconfigmod.config.model.DatapackSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试 DatapackSettings 的序列化和反序列化
 */
public class DatapackSettingsTest {
    
    public static void main(String[] args) {
        testSerialization();
    }
    
    public static void testSerialization() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        // 创建一个测试对象
        DatapackSettings settings = new DatapackSettings("test:datapack", "测试数据包");
        settings.setEnabled(false);
        Map<String, String> customValues = new HashMap<>();
        customValues.put("spawnRate", "100");
        customValues.put("enableDrops", "true");
        settings.setCustomValues(customValues);
        
        // 序列化
        String json = gson.toJson(settings);
        System.out.println("序列化结果:");
        System.out.println(json);
        System.out.println();
        
        // 反序列化
        DatapackSettings deserialized = gson.fromJson(json, DatapackSettings.class);
        System.out.println("反序列化结果:");
        System.out.println("datapackId: " + deserialized.getDatapackId());
        System.out.println("displayName: " + deserialized.getDisplayName());
        System.out.println("enabled: " + deserialized.isEnabled());
        System.out.println("customValues: " + deserialized.getCustomValues());
    }
}
