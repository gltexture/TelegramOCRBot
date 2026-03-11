import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.gltexture.db.HashUtil;
import ru.gltexture.db.RequestRepository;
import ru.gltexture.db.UserRepository;

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
}