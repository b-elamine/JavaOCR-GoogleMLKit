package com.example.textrecognitionapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostProcessor {

    private static final String[] FUEL_TYPES = {"SP95E10", "SP98", "SP95", "E85", "E10", "GAZOLE", "GPL", "Splomb98", "Splomb95"};

    public static List<Map<String, Object>> combineSplitWords(List<Map<String, Object>> elements) {
        List<Map<String, Object>> combinedData = new ArrayList<>();
        int n = elements.size();

        for (int i = 0; i < n; ) {
            StringBuilder combinedText = new StringBuilder((String) elements.get(i).get("text"));
            int left = (int) elements.get(i).get("left");
            int top = (int) elements.get(i).get("top");
            int right = (int) elements.get(i).get("right");
            int bottom = (int) elements.get(i).get("bottom");
            int width = (int) elements.get(i).get("width");
            int height = (int) elements.get(i).get("height");
            float conf = (float) elements.get(i).get("conf");

            int j = i + 1;
            while (j < n && (int) elements.get(j).get("top") == top) {
                combinedText.append(elements.get(j).get("text"));
                right = (int) elements.get(j).get("left") + (int) elements.get(j).get("width");
                bottom = Math.max(bottom, (int) elements.get(j).get("top") + (int) elements.get(j).get("height"));
                conf = (float) ((float) elements.get(j).get("conf") + (float) elements.get(i).get("conf"))/2;
                j++;
            }

            Map<String, Object> combinedElement = new HashMap<>();
            combinedElement.put("text", combinedText.toString().toUpperCase());
            combinedElement.put("left", left);
            combinedElement.put("right", right);
            combinedElement.put("top", top);
            combinedElement.put("bottom", bottom);
            combinedElement.put("centerX", left + width / 2.0f);
            combinedElement.put("centerY", top + height / 2.0f);
            combinedElement.put("width", right - left);
            combinedElement.put("height", bottom - top);
            combinedElement.put("conf", conf);
            combinedData.add(combinedElement);

            i = j;
        }

        return combinedData;
    }

    public static List<Map<String, Object>> filterAndExtractLabels(List<Map<String, Object>> combinedData) {
        List<Map<String, Object>> extractedLabels = new ArrayList<>();
        for (Map<String, Object> item : combinedData) {
            String text = ((String) item.get("text")).replace(" ", "").toUpperCase();
            for (String fuelType : FUEL_TYPES) {
                if (text.contains(fuelType)) {
                    Map<String, Object> labelData = new HashMap<>();
                    labelData.put("text", fuelType);
                    labelData.put("bounding_box", item);
                    extractedLabels.add(labelData);
                }
            }
        }
        return extractedLabels;
    }

    public static List<Map<String, Object>> processElements(List<Map<String, Object>> elements) {
        List<Map<String, Object>> combinedData = combineSplitWords(elements);
        return filterAndExtractLabels(combinedData);
    }
}
