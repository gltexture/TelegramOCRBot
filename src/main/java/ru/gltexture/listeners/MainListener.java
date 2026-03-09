package ru.gltexture.listeners;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.gltexture.Bot;
import ru.gltexture.BotApplication;
import ru.gltexture.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class MainListener implements Listener {
    private final Map<String, CommandInfo> availableCommands;

    public MainListener(String listenerClassPath) {
        this.availableCommands = new HashMap<>();
        this.collectMethods(listenerClassPath);
    }

    public void handleMessage(MessageData messageData) throws TelegramApiException {
        String command = Utils.getFirstWord(messageData.getFullMessage());
        Bot.instance.getBotLogging().log("Received: " + messageData.getFullMessage());
        if (!this.isCommandExists(command)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(messageData.chatId));
            sendMessage.setText(this.defaultMessage());
            messageData.getBotApplication().execute(sendMessage);
        } else {
            CommandInfo commandInfo = this.getAvailableCommands().get(command);
            if (commandInfo != null) {
                Bot.instance.getBotLogging().log(command + " :Accepted: " + messageData.getFullMessage());
                commandInfo.getConsumer().accept(messageData);
            } else {
                Bot.instance.getBotLogging().warn(messageData + " - NULL CONSUMER!");
            }
        }
    }

    public boolean isCommandExists(String command) {
        return this.getAvailableCommands().containsKey(command);
    }

    private void collectMethods(String listenerClassPath) {
        try {
            Class<?> clazz = Class.forName(listenerClassPath);
            Set<Method> methodSet = new HashSet<>(Arrays.asList(clazz.getMethods()));
            for (Method method : methodSet) {
                CommandListener commandListener = method.getAnnotation(CommandListener.class);
                if (commandListener != null) {
                    String command = commandListener.value();
                    Consumer<MessageData> consumer = this.buildConsumer(method);
                    this.getAvailableCommands().put(command, new CommandInfo(consumer, commandListener.showOnMenu(), commandListener.description()));
                }
            }
        } catch (ClassNotFoundException e) {
            Bot.instance.getBotLogging().error(e.getMessage());
        }
    }

    public Map<String, CommandInfo> getAvailableCommands() {
        return this.availableCommands;
    }

    private Consumer<MessageData> buildConsumer(Method method) {
        return (E) -> {
            try {
                method.invoke(null, E);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public String defaultMessage() {
        return "Unknown message. /menu";
    }

    public static class CommandInfo {
        private final Consumer<MessageData> consumer;
        private final boolean showInMenu;
        private final String description;

        public CommandInfo(Consumer<MessageData> consumer, boolean showInMenu, String description) {
            this.consumer = consumer;
            this.showInMenu = showInMenu;
            this.description = description;
        }

        public Consumer<MessageData> getConsumer() {
            return this.consumer;
        }

        public boolean isShowInMenu() {
            return this.showInMenu;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class MessageData {
        private final long chatId;
        private final BotApplication botApplication;
        private final String fullMessage;

        public MessageData(long chatId, BotApplication botApplication) {
            this.chatId = chatId;
            this.fullMessage = "";
            this.botApplication = botApplication;
        }

        public MessageData(long chatId, BotApplication botApplication, String fullMessage) {
            this.chatId = chatId;
            this.fullMessage = fullMessage;
            this.botApplication = botApplication;
        }

        public long getChatId() {
            return this.chatId;
        }

        public BotApplication getBotApplication() {
            return this.botApplication;
        }

        public String getFullMessage() {
            return this.fullMessage;
        }
    }
}
