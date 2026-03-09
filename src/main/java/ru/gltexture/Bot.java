package ru.gltexture;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.gltexture.logger.BotLogging;
import ru.gltexture.db.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class Bot {
    public static Bot instance = new Bot();
    private final BotApplication botApplication;
    private final BotLogging botLogging;

    public Bot() {
        this.botApplication = new BotApplication(Executors.newFixedThreadPool(128));
        this.botLogging = new BotLogging();
    }

    public synchronized BotLogging getBotLogging() {
        return this.botLogging;
    }

    public synchronized BotApplication getBotApplication() {
        return this.botApplication;
    }

    public void init() {
        this.getBotLogging().log("Starting bot!");
        this.init(this.getBotApplication());

        Runtime.getRuntime().addShutdownHook(new Thread(this.botApplication::destroy));
    }

    private void init(BotApplication botApplication) {
        try {
            try (Connection c = Database.get().getConnection()) {
                if (!c.isClosed()) {
                    Bot.instance.getBotLogging().debug("DB connected");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(botApplication);
        } catch (TelegramApiException e) {
            this.getBotLogging().error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Bot.instance.init();
    }
}