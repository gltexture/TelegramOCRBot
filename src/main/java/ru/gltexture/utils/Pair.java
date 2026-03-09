package ru.gltexture.utils;

import org.jetbrains.annotations.NotNull;

public record Pair<K, V>(K first, V second) {

    @SuppressWarnings("all")
    @SafeVarargs
    public static <K, V> Pair<K, V>[] get(Pair<K, V>... pairs) {
        Pair[] kvPair = new Pair[pairs.length];
        System.arraycopy(pairs, 0, kvPair, 0, kvPair.length);
        return kvPair;
    }

    @NotNull
    @Override
    public String toString() {
        return first + " + " + second;
    }
}