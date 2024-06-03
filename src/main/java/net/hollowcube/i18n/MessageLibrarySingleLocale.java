package net.hollowcube.i18n;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class MessageLibrarySingleLocale implements MessageLibrary {
    private final MessageParser parser = new MessageParser(false);
    private final Map<String, String> unparsed;

    private final Map<String, Tree> partialCache = new ConcurrentHashMap<>();
    private final Cache<TranslatableComponent, Component> expandedCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .build();

    public MessageLibrarySingleLocale(@NotNull MessageLoader loader) {
        this.unparsed = loader.load().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public @NotNull Key name() {
        return NAME;
    }

    @Override
    public @NotNull Component translate(@NotNull TranslatableComponent component, @NotNull Locale ignored) {
        return expandedCache.get(component, c -> {
            Tree tree = partialCache.computeIfAbsent(c.key(), this::computePartial);
            if (tree == null) return component.fallback() != null
                    ? Component.text(Objects.requireNonNull(component.fallback()))
                    : Component.text(component.key());

            // Component is a partial tree, resolve it.
            List<Component> resolvedArgs = new ArrayList<>(c.arguments().size());
            for (TranslationArgument arg : c.arguments()) resolvedArgs.add(arg.asComponent());
            return tree.resolve(resolvedArgs);
        });
    }

    private @Nullable Tree computePartial(@NotNull String key) {
        String value = unparsed.get(key);
        if (value == null) return null;

        return parser.parse(value);
    }

}
