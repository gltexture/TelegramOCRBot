package ru.gltexture.listeners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.gltexture.Bot;
import ru.gltexture.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class CommandWaiting {
    private final String baseCommand;
    private final List<Object> objectExpectations;
    private final List<String> messages;
    private final Consumer<CommandWaiting> actionConsumerOnCompletion;
    private final int maxSize;
    private boolean completed;

    public CommandWaiting(@NotNull Consumer<CommandWaiting> actionConsumerOnCompletion, @NotNull String baseCommand, @NotNull List<String> messages, int maxSize) {
        this.baseCommand = baseCommand;
        this.objectExpectations = new ArrayList<>(maxSize);
        this.messages = messages;
        this.actionConsumerOnCompletion = actionConsumerOnCompletion;
        this.maxSize = maxSize;
        this.completed = false;
    }

    public Pair<String, List<String>> getMessageToView() {
        String baseMsg = this.messages.get(this.objectExpectations.size());
        List<String> str = null;
        if (baseMsg.contains("{") && baseMsg.contains("}")) {
            str = new ArrayList<>();
            String menuDef = baseMsg.substring(baseMsg.indexOf("{") + 1, baseMsg.indexOf("}"));
            String[] menuButtons = menuDef.split(":");
            Collections.addAll(str, menuButtons);
            baseMsg = baseMsg.substring(0, baseMsg.indexOf("{"));
        }
        return new Pair<>(baseMsg, str);
    }

    public List<String> getMessages() {
        return this.messages;
    }

    public @Nullable Object getExpectation(int id) {
        if (this.objectExpectations.size() > id) {
            return this.objectExpectations.get(id);
        }
        return null;
    }

    public Consumer<CommandWaiting> actionConsumer() {
        return this.actionConsumerOnCompletion;
    }

    public String getBaseCommand() {
        return this.baseCommand;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void addNewExpectationAndTestForCompletion(Object object) {
        this.objectExpectations.add(object);
        if (this.objectExpectations.size() >= this.maxSize) {
            Bot.instance.getBotLogging().debug("Exec");
            this.actionConsumer().accept(this);
            this.completed = true;
            return;
        }
        this.completed = false;
    }
}
