import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.gltexture.db.HashUtil;
import ru.gltexture.db.RequestRepository;
import ru.gltexture.db.UserRepository;
import ru.gltexture.ocr.OCRService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class OcrRequestTest {
    @Test
    @DisplayName("TC-01 Проверка сохранения OCR запроса")
    public void testSaveRequest() {
        long telegramId = 123456789;
        long userId = UserRepository.getOrCreateUser(telegramId);
        byte[] image = ("testImage" + System.nanoTime()).getBytes();
        String hash = HashUtil.sha256(image);
        String text = "Hello world";
        RequestRepository.saveRequest(userId, hash, image, text);
        String result = RequestRepository.findByHash(hash);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(text, result);
        System.out.println("Success!");
    }

    @Test
    @DisplayName("TC-02 Проверка получения или создания пользователя")
    public void testGetOrCreateUser() {
        long telegramId = 987654321L;
        long user1 = UserRepository.getOrCreateUser(telegramId);
        long user2 = UserRepository.getOrCreateUser(telegramId);
        Assertions.assertEquals(user1, user2);
    }

    @Test
    @DisplayName("НС-01 Проверка нагрузки")
    public void loadTest() {
        long telegramId = 111111111;
        long userId = UserRepository.getOrCreateUser(telegramId);

        int requests = 100;
        for (int i = 0; i < requests; i++) {
            try {
                BufferedImage image = new BufferedImage(200, 80, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, 200, 80);
                g.setColor(Color.BLACK);
                g.drawString("TEST " + i, 50, 40);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                String text = OCRService.recognize(imageBytes);

                String hash = HashUtil.sha256(imageBytes);
                RequestRepository.saveRequest(userId, hash, imageBytes, text);

                String result = RequestRepository.findByHash(hash);

                Assertions.assertNotNull(result);

            } catch (Exception e) {
                Assertions.fail(e);
            }
        }

        System.out.println("Load test completed: " + requests + " OCR requests");
    }
}