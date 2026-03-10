package ru.gltexture.ocr;

import java.io.IOException;

public class OCRService {
    public static String recognize(byte[] imageBytes) throws IOException {
        return YandexVisionClient.recognize(imageBytes);
    }
}