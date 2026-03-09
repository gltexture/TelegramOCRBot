package ru.gltexture.utils;

import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import ru.gltexture.Bot;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class Utils {
    public static String getFirstWord(@NotNull String s) {
        return Utils.splitMessage(s)[0];
    }

    public static String[] splitMessage(@NotNull String s) {
        return s.split(" ");
    }

    public static boolean isCommand(@NotNull String s) {
        return s.startsWith("/");
    }

    public static void writeLPCMtoWav(byte[] lpcmBytes, float sampleRate, int channels, String outputPath) throws IOException {
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(lpcmBytes);
             AudioInputStream ais = new AudioInputStream(bais, format, lpcmBytes.length / (2L * channels))) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outputPath));
        }
    }
}
