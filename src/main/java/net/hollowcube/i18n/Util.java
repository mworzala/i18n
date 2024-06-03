package net.hollowcube.i18n;

import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

final class Util {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final Pattern PLAIN_ARG_PATTERN = Pattern.compile("<[0-9]+>");

    static boolean containsArgs(@NotNull String plainText) {
        return PLAIN_ARG_PATTERN.matcher(plainText).find();
    }

    static @NotNull String substArgs(@NotNull String plainText, @NotNull List<Component> args) {
        return PLAIN_ARG_PATTERN.matcher(plainText)
                .replaceAll(match -> {
                    var rawGroup = match.group();
                    var index = Integer.parseInt(rawGroup.substring(1, rawGroup.length() - 1));
                    if (index < 0 || index >= args.size()) {
                        return "$$" + index;
                    } else {
                        return PLAIN_TEXT.serialize(args.get(index));
                    }
                });
    }

    record SinglePointer<T>(@NotNull Pointer<T> pointer, T value) implements Pointered {
        @Override
        public @NotNull <T> Optional<T> get(@NotNull Pointer<T> pointer) {
            return this.pointer.equals(pointer) ? Optional.of((T) this.value) : Optional.empty();
        }
    }

}
