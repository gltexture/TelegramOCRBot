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
        String command = Utils.getFirstWord(messageData.fullMessage());
        Bot.instance.getBotLogging().log("Received: " + messageData.fullMessage());
        if (!this.isCommandExists(command)) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(messageData.chatId));
            sendMessage.setText(this.defaultMessage());
            messageData.botApplication().execute(sendMessage);
        } else {
            CommandInfo commandInfo = this.getAvailableCommands().get(command);
            if (commandInfo != null) {
                Bot.instance.getBotLogging().log(command + " :Accepted: " + messageData.fullMessage());
                commandInfo.consumer().accept(messageData);
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

    public record CommandInfo(Consumer<MessageData> consumer, boolean showInMenu, String description) {
    }

    public record MessageData(long chatId, BotApplication botApplication, String fullMessage) {
            public MessageData(long chatId, BotApplication botApplication) {
                this(chatId, botApplication, "");
            }

    }
}
