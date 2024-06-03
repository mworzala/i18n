package net.hollowcube.i18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * <p>A message library is a collection of messages in multiple locales.</p>
 *
 * <p>Also implements an Adventure {@link Translator} so can be used for automatic translation.</p>
 */
public interface MessageLibrary extends Translator {
    Key NAME = Key.key("hollowcube", "message_library");

    @Override
    default @NotNull Key name() {
        return NAME;
    }
    
    @Override
    default @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        throw new UnsupportedOperationException("Translation to MessageFormat is unsupported.");
    }

    @Override
    @NotNull Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale);

}
