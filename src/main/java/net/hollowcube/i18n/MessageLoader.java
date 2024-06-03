package net.hollowcube.i18n;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Provides messages for a given locale.
 */
public interface MessageLoader {

    @NotNull Stream<Map.Entry<String, String>> load();

}
