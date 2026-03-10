package ru.gltexture.ocr;

import ru.gltexture.BotSecret;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class YandexVisionClient {

    private static final String BASE_URL = "https://vision.api.cloud.yandex.net/";
    private static final String API_KEY_ENV = "YC_VISION_API_KEY";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();

    public static String recognize(byte[] imageBytes) throws IOException {
        String apiKey = BotSecret.YC_VISION_API_KEY;
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set " + API_KEY_ENV + " in BotSecret");
        }

        String authHeader = "Api-Key " + apiKey;

        String contentBase64 = Base64.getEncoder().encodeToString(imageBytes);
        TextDetectionConfig config = new TextDetectionConfig();
        config.languageCodes = List.of("ru", "en");
        Feature feature = new Feature();
        feature.type = "TEXT_DETECTION";
        feature.textDetectionConfig = config;
        AnalyzeSpec spec = new AnalyzeSpec();
        spec.content = contentBase64;
        spec.features = List.of(feature);
        BatchAnalyzeRequest requestBody = new BatchAnalyzeRequest();
        requestBody.analyzeSpecs = List.of(spec);

        Request request = new Request.Builder()
                .url(BASE_URL + "vision/v1/batchAnalyze")
                .header("Authorization", authHeader)
                .post(RequestBody.create(GSON.toJson(requestBody), JSON))
                .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Yandex Vision API error: HTTP " + response.code());
            }
            String rawJson = response.body().string();
            BatchAnalyzeResponse parsed = GSON.fromJson(rawJson, BatchAnalyzeResponse.class);
            return extractText(parsed);
        }
    }

    private static String extractText(BatchAnalyzeResponse response) {
        if (response.results == null || response.results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (BatchAnalyzeResponse.Result r : response.results) {
            if (r.results == null) continue;
            for (BatchAnalyzeResponse.TextDetectionResult tr : r.results) {
                if (tr.textDetection == null || tr.textDetection.pages == null) continue;
                for (BatchAnalyzeResponse.Page p : tr.textDetection.pages) {
                    if (p.blocks == null) continue;
                    for (BatchAnalyzeResponse.Block b : p.blocks) {
                        if (b.lines == null) continue;
                        for (BatchAnalyzeResponse.Line l : b.lines) {
                            if (l.words == null) continue;
                            for (BatchAnalyzeResponse.Word w : l.words) {
                                if (w.text != null && !w.text.isBlank()) {
                                    sb.append(w.text).append(' ');
                                }
                            }
                            if (l.words != null && !l.words.isEmpty()) {
                                sb.append('\n');
                            }
                        }
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    // ===== DTOs =====

    static class BatchAnalyzeRequest {
        @SerializedName("analyze_specs")
        List<AnalyzeSpec> analyzeSpecs;
    }

    static class AnalyzeSpec {
        String content;
        List<Feature> features;
    }

    static class Feature {
        String type;
        @SerializedName("text_detection_config")
        TextDetectionConfig textDetectionConfig;
    }

    static class TextDetectionConfig {
        @SerializedName("language_codes")
        List<String> languageCodes;
    }

    static class BatchAnalyzeResponse {
        List<Result> results;

        static class Result {
            @SerializedName("results")
            List<TextDetectionResult> results;
        }

        static class TextDetectionResult {
            @SerializedName("textDetection")
            TextDetection textDetection;
        }

        static class TextDetection {
            List<Page> pages;
        }

        static class Page {
            List<Block> blocks;
        }

        static class Block {
            List<Line> lines;
        }

        static class Line {
            List<Word> words;
        }

        static class Word {
            String text;
        }
    }
}

