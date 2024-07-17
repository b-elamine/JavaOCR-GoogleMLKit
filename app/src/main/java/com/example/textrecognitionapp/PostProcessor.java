package com.example.textrecognitionapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostProcessor {

    private static final String[] FUEL_TYPES = {
            "SP95E10",
            "SP95-E10",
            "SP98",
            "SP95",
            "E85",
            "E10",
            "GAZOLE",
            "GASOIL",
            "GPL",
            "GPLC",
            "Splomb98",
            "Splomb95",
            "SUPERETHANOL",
            "SUPER",
            "ETHANOL",
            "DIESEL"
    };



    public static List<Map<String, Object>> combineSplitWords(List<Map<String, Object>> elements) {
        List<Map<String, Object>> combinedData = new ArrayList<>();
        int n = elements.size();

        // Test and Change tolerance values or dynamic for ex :
        // HorizontalTolerance = heightCurrentElement/2
        // TopTolerance = widthCurrentElement
        final int TOP_TOLERANCE = 5;
        final int HORIZONTAL_TOLERANCE = 20;

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

            while (j < n && Math.abs((int) elements.get(j).get("top") - top) <= TOP_TOLERANCE) {
                int currentLeft = (int) elements.get(j).get("left");
                if (currentLeft - right <= HORIZONTAL_TOLERANCE) {
                    combinedText.append(elements.get(j).get("text"));
                    right = currentLeft + (int) elements.get(j).get("width");
                    bottom = Math.max(bottom, (int) elements.get(j).get("top") + (int) elements.get(j).get("height"));
                    conf = (float) ((float) elements.get(j).get("conf") + (float) elements.get(i).get("conf")) / 2;
                    j++;
                } else {
                    break;
                }
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


    // TODO :
    //  *** Conditions for more robust labels detection ***
    //  if [SP95], then check under for [E10]
    //  if [E10], then check above or right side for [SP95]
    //  if [SUPER], then check under for [Ethanol]
    //  if [SUPER Ethanol], check under for [E85]
    public static List<Map<String, Object>> filterAndExtractLabels(List<Map<String, Object>> combinedData) {
        List<Map<String, Object>> extractedLabels = new ArrayList<>();
        for (Map<String, Object> item : combinedData) {
            String text = ((String) item.get("text")).replace(" ", "").toUpperCase();
            for (String fuelType : FUEL_TYPES) {
                if (text.equals(fuelType) || text.contains(fuelType)) {
                    Map<String, Object> labelData = new HashMap<>();
                    if (text.equals(fuelType)) {
                        labelData.put("text", fuelType);
                        labelData.put("bounding_box", item);
                        extractedLabels.add(labelData);
                    } else {
                        labelData.put("text", fuelType);
                        labelData.put("pPrice0", text.substring(0, (text.indexOf(fuelType))));
                        labelData.put("pPrice1", text.substring((text.indexOf(fuelType)) + (fuelType.length()), text.length()));
                        System.out.println("Potential Prices : " + labelData.get("pPrice0") + " | " + labelData.get("pPrice1") + " " + "For " + fuelType);
                    }
                }
            }
        }
        return extractedLabels;
    }

    public static List<Map<String, Object>> processElements(List<Map<String, Object>> elements) {
        List<Map<String, Object>> combinedData = combineSplitWords(elements);
        return filterAndExtractLabels(combinedData);
    }

    public static List<Map<String, Object>> processPrice(List<Map<String, Object>> elements) {
        // Define a regex pattern to match common OCR mistakes
        String[][] replacements = {
                {"I", "1"},
                {"S", "5"},
                {"O", "0"},
                {"B", "8"},
                {"G", "6"},
                {"Z", "2"},
                {"Q", "0"},
                {"D", "0"},
                {"L", "1"},
                {"C", "0"},
                {"N", "7"}
        };

        List<Map<String, Object>> prices = new ArrayList<>();

        for (Map<String, Object> element : elements) {
            String text = (String) element.get("text");
            if (isLikelyPrice(text, 0.5)) {
                System.out.println("Results : True");
                String corrected = text;
                Map<String, Object> pricesData = new HashMap<>();
                // Loop through the replacements and apply them
                for (String[] replacement : replacements) {
                    corrected = corrected.replaceAll(replacement[0], replacement[1]);
                }

                corrected = corrected.replaceAll("\\D", "");
                if (corrected.length() == 4) {
                    corrected = corrected.replaceAll("(\\d)(\\d{3})", "$1.$2");
                } else {
                    corrected = String.format("%04d", Integer.parseInt(corrected));
                    //corrected = corrected.replaceAll("(\\d)(\\d{3})", "$1.$2");
                }
                System.out.println("PRICES WITHOUT MATCHING : " +corrected);
                pricesData.put("text", corrected);
                pricesData.put("bounding_box", element);
                prices.add(pricesData);
            }
        }
        return prices;
    }

    // True or false the detected word is price
    public static boolean isLikelyPrice(String input, double threshold) {
        int digitCount = 0;
        int length = input.length();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
            }
        }

        // Check if the ratio of digits to total length meets the threshold
        return ((double) digitCount / length) >= threshold;
    }


}
