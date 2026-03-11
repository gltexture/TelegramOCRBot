package ru.gltexture.listeners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gltexture.Bot;
import ru.gltexture.BotApplication;
import ru.gltexture.db.HashUtil;
import ru.gltexture.db.RequestRepository;
import ru.gltexture.db.UserRepository;
import ru.gltexture.ocr.OCRService;
import ru.gltexture.utils.Pair;

import java.util.*;

public class CommandBase {
    private static final Map<Long, CommandWaiting> commandWaitingMap = new HashMap<>();

    public static synchronized @Nullable CommandWaiting getCommandWaiting(long chatId) {
        CommandWaiting commandWaiting = CommandBase.commandWaitingMap.getOrDefault(chatId, null);
        if (commandWaiting != null && commandWaiting.isCompleted()) {
            Bot.instance.getBotLogging().debug("Removed waiting for " + chatId + " - " + commandWaiting.getBaseCommand());
            CommandBase.commandWaitingMap.remove(chatId);
            return null;
        }
        return commandWaiting;
    }

    public static void putNewWait(long chatId, @NotNull CommandWaiting commandWaiting) {
        CommandBase.commandWaitingMap.put(chatId, commandWaiting);
        try {
            Pair<String, List<String>> cbSet = commandWaiting.getMessageToView();
            CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), cbSet.first(), cbSet.second());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("all")
    public static InlineKeyboardMarkup buildStandardMenu(MainListener.MessageData messageData) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        Map<String, MainListener.CommandInfo> commands = Bot.instance.getBotApplication().getMainListener().getAvailableCommands();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        int j = 0;
        for (Map.Entry<String, MainListener.CommandInfo> entry : commands.entrySet()) {
            if (!entry.getValue().showInMenu()) {
                continue;
            }
            String command = entry.getKey();
            String description = entry.getValue().description();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(description);
            button.setCallbackData(command);
            currentRow.add(button);

            if (j++ >= 0) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                j = 0;
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public static boolean checkIfCanExecCommand(MainListener.MessageData messageData) {
        CommandWaiting commandWaiting = CommandBase.getCommandWaiting(messageData.chatId());
        return commandWaiting != null;
    }

    @CommandListener(value = "/exit", showOnMenu = false, description = "Exit Current Command")
    public static void onEndCommand(MainListener.MessageData messageData) {
        try {
            if (CommandBase.commandWaitingMap.remove(messageData.chatId()) != null) {
                CommandBase.sendMessage(messageData.chatId(), Bot.instance.getBotApplication(), "Cancelled the current thread! 🤖");
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @CommandListener(value = "/start", showOnMenu = false, description = "Start 🔧")
    public static void onStart(MainListener.MessageData messageData) {
        if (CommandBase.checkIfCanExecCommand(messageData)) {
            return;
        }
        try {
            InlineKeyboardMarkup keyboardMarkup = CommandBase.buildStandardMenu(messageData);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(messageData.chatId()));
            sendMessage.setText("📱Commands Menu:");
            sendMessage.setReplyMarkup(keyboardMarkup);
            messageData.botApplication().execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @CommandListener(value = "/help", showOnMenu = true, description = "Help")
    public static void help(MainListener.MessageData messageData) {
        if (CommandBase.checkIfCanExecCommand(messageData)) {
            return;
        }
        try {
            String text =
                    "🤖 *OCR Bot*\n\n" +
                            "Бот предназначен для распознавания рукописного текста на изображениях.\n\n" +
                            "📋 *Как пользоваться:*\n" +
                            "1. Отправьте изображение с рукописным текстом.\n" +
                            "2. Бот обработает изображение.\n" +
                            "3. Через несколько секунд вы получите распознанный текст.\n\n" +
                            "⚙️ *Особенности:*\n" +
                            "• Поддержка изображений (PNG, JPG)\n" +
                            "• Кэширование результатов распознавания\n" +
                            "• Быстрая обработка запросов\n\n" +
                            "📌 *Команды:*\n" +
                            "/start — открыть меню\n" +
                            "/help — показать справку\n\n" +
                            "ℹ️ Если отправить то же изображение повторно, бот вернёт результат из кэша.";
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(messageData.chatId()));
            sendMessage.setText(text);
            sendMessage.setParseMode("Markdown");
            messageData.botApplication().execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @CommandListener(value = "/aiRead", showOnMenu = true, description = "AI Read")
    public static void aiRead(MainListener.MessageData messageData) {
        CommandWaiting waiting = new CommandWaiting(
                (cw) -> {
                    try {
                        long chatId = messageData.chatId();
                        Object obj = cw.getExpectation(0);
                        if (!(obj instanceof String base64)) {
                            CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), "❌ Please send a photo.");
                            return;
                        }
                        byte[] imageBytes;
                        try {
                            imageBytes = Base64.getDecoder().decode(base64);
                        } catch (IllegalArgumentException e) {
                            CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), "❌ Invalid image format. Send a photo.");
                            return;
                        }
                        long userId = UserRepository.getOrCreateUser(chatId);
                        String hash = HashUtil.sha256(imageBytes);

                        // Пытаемся найти уже распознанный текст в БД по хэшу изображения
                        String cached = RequestRepository.findByHash(hash);
                        if (cached != null && !cached.isBlank()) {
                            CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), "OCR Result:\n\n" + cached);
                            return;
                        }

                        // Выполняем OCR прямо сейчас через локально установленный tesseract
                        String text = OCRService.recognize(imageBytes);
                        if (text == null || text.isBlank()) {
                            text = "[пусто / ничего не распознано]";
                        }

                        RequestRepository.saveRequest(userId, hash, imageBytes, text);
                        CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), "OCR Result:\n\n" + text);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        try {
                            CommandBase.sendMessage(messageData.chatId(), Bot.instance.getBotApplication(), "⚠ Error while processing image.");
                        } catch (TelegramApiException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                },
                "/aiRead",
                List.of("📷 Send photo with handwritten text"), 1
        );
        CommandBase.putNewWait(messageData.chatId(), waiting);
    }

    @CommandListener(value = "/history", showOnMenu = true, description = "History")
    public static void history(MainListener.MessageData messageData) {
        long userId = UserRepository.getOrCreateUser(messageData.chatId());
        List<String> history = RequestRepository.getHistory(userId);
        if (history.isEmpty()) {
            try {
                CommandBase.sendMessage(messageData.chatId(), Bot.instance.getBotApplication(), "History empty");
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : history) {
            sb.append("• ").append(s).append("\n\n");
        }
        try {
            CommandBase.sendMessage(messageData.chatId(), Bot.instance.getBotApplication(), sb.toString());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @CommandListener(value = "/menu", showOnMenu = false, description = "Menu 🔧")
    public static void onHelp(MainListener.MessageData messageData) {
        if (CommandBase.checkIfCanExecCommand(messageData)) {
            return;
        }
        CommandBase.onStart(messageData);
    }

    public static void sendMessage(long chatId, BotApplication botApplication, String toSend) throws TelegramApiException {
        CommandBase.sendMessage(chatId, botApplication, toSend, null);
    }

    public static ReplyKeyboardMarkup buildButtonMenu(List<String> str) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow currentRow = new KeyboardRow();

        int j = 0;
        for (String s : str) {
            currentRow.add(s);

            if (j++ >= 2) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
                j = 0;
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public static void sendMessage(long chatId, BotApplication botApplication, String toSend, @Nullable List<String> str) throws TelegramApiException {
        Bot.instance.getBotLogging().debug(chatId + ": " + toSend);
        SendMessage sendMessage = new SendMessage();
        if (str != null) {
            ReplyKeyboardMarkup replyKeyboardMarkup = CommandBase.buildButtonMenu(str);
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            replyKeyboardMarkup.setIsPersistent(true);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(toSend);
        botApplication.execute(sendMessage);
    }
}