package ru.gltexture;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gltexture.listeners.CommandBase;
import ru.gltexture.listeners.CommandWaiting;
import ru.gltexture.listeners.MainListener;
import ru.gltexture.utils.Pair;
import ru.gltexture.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class BotApplication extends TelegramLongPollingBot {
    private final MainListener mainListener;
    private final ExecutorService executorService;

    public BotApplication(ExecutorService executorService) {
        super(BotSecret.TOKEN);
        this.mainListener = new MainListener("ru.gltexture.listeners.CommandBase");
        this.executorService = executorService;
    }

    public void destroy() {
        Bot.instance.getBotLogging().log("Destroy!");
        this.executorService.shutdown();
        try {
            if (!this.executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executorService.shutdownNow();
        }
    }

    public MainListener getMainListener() {
        return this.mainListener;
    }

    @Override
    public String getBotUsername() {
        return BotSecret.NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        this.executorService.submit(() -> {
            try {
                if (update.hasMessage()) {
                    Long chatId = update.getMessage().getChatId();
                    CommandWaiting commandWaiting = CommandBase.getCommandWaiting(chatId);

                    if (update.getMessage().hasText()) {
                        MainListener.MessageData messageData = new MainListener.MessageData(chatId, this, update.getMessage().getText());
                        if (commandWaiting != null) {
                            if (Utils.isCommand(update.getMessage().getText())) {
                                CommandBase.onEndCommand(messageData);
                            } else {
                                commandWaiting.addNewExpectationAndTestForCompletion(update.getMessage().getText());
                                Pair<String, List<String>> cbSet = commandWaiting.getMessageToView();
                                CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), cbSet.first(), cbSet.second());
                            }
                            Bot.instance.getBotLogging().log("Received text: " + update.getMessage().getText());
                            return;
                        }
                        this.getMainListener().handleMessage(messageData);
                    } else if (update.getMessage().hasPhoto() && commandWaiting != null) {
                        List<PhotoSize> photos = update.getMessage().getPhoto();
                        PhotoSize photo = photos.get(photos.size() - 1);

                        File file = this.execute(new GetFile(photo.getFileId()));
                        java.io.File localFile = this.downloadFile(file);
                        byte[] fileBytes;
                        try (FileInputStream fis = new FileInputStream(localFile)) {
                            fileBytes = fis.readAllBytes();
                        }

                        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                        commandWaiting.addNewExpectationAndTestForCompletion(base64Image);
                        CommandBase.sendMessage(chatId, Bot.instance.getBotApplication(), "Got photo.");
                        Bot.instance.getBotLogging().log("Received photo (Base64 saved), size: " + fileBytes.length);
                    }
                } else if (update.hasCallbackQuery()) {
                    String command = update.getCallbackQuery().getData();
                    Long chatId = update.getCallbackQuery().getMessage().getChatId();

                    if (!CommandBase.checkIfCanExecCommand(new MainListener.MessageData(chatId, this))) {
                        CommandBase.onEndCommand(new MainListener.MessageData(chatId, this));
                    }

                    this.getMainListener().handleMessage(new MainListener.MessageData(chatId, this, command));
                    this.executeAsync(new AnswerCallbackQuery(update.getCallbackQuery().getId()));
                }
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace(System.err);
            }
        });
    }
}