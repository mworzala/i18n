package net.hollowcube.i18n;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Holds alternative implementations of minimessage tags required to do partial arg resolution.
 */
interface TagResolvable {
    TagResolver RESOLVER = TagResolver.resolver(
            // Create a resolver with the following notable omissions:
            // gradient, rainbow. These are not supported by the partial arg resolution.
            // They can still be used, but minimessage will be used directly to resolve.
            StandardTags.color(),
            StandardTags.keybind(),
            StandardTags.transition(),
            StandardTags.translatable(),
            StandardTags.translatableFallback(),
            StandardTags.insertion(),
            StandardTags.font(),
            StandardTags.decorations(),
            StandardTags.reset(),
            StandardTags.newline(),
            StandardTags.transition(),
            StandardTags.selector(),
            StandardTags.score(),
            StandardTags.nbt(),

            // Overrides
            Placeholder.RESOLVER,
            Click.RESOLVER,
            Hover.RESOLVER
    );

    /**
     * MiniMessage {@link Inserting}, but supports partial resolution.
     */
    interface InsertingWithArgs {

        @NotNull Component value(@NotNull List<Component> args);

    }

    record Placeholder(int index) implements Tag {
        static final TagResolver RESOLVER = new TagResolver() {
            @Override
            public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                try {
                    var index = Integer.parseInt(name);
                    if (index < 0) return null;
                    return new Placeholder(index);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            @Override
            public boolean has(@NotNull String name) {
                try {
                    return Integer.parseInt(name) >= 0;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        };
    }

    record Click(@NotNull ClickEvent.Action action, @NotNull String text) implements Tag, InsertingWithArgs {
        static final TagResolver RESOLVER = TagResolver.resolver("click", Click::create);

        @Override
        public @NotNull Component value(@NotNull List<Component> args) {
            var replacedText = Util.substArgs(this.text, args);
            return Component.text("", Style.style(ClickEvent.clickEvent(action, replacedText)));
        }

        /**
         * Tag impl creation from MiniMessage builtin tag.
         */
        private static Tag create(final ArgumentQueue args, final Context ctx) throws ParsingException {
            final String actionName = args.popOr(() -> "A click tag requires an action of one of " + ClickEvent.Action.NAMES.keys()).lowerValue();
            final ClickEvent.@Nullable Action action = ClickEvent.Action.NAMES.value(actionName);
            if (action == null) {
                throw ctx.newException("Unknown click event action '" + actionName + "'", args);
            }

            final String value = args.popOr("Click event actions require a value").value();
            if (!Util.containsArgs(value)) {
                // No placeholders, so just resolve it now
                return Tag.styling(ClickEvent.clickEvent(action, value));
            }

            return new Click(action, value); // It is dynamic, very sad
        }
    }

    record Hover(@NotNull Tree partial) implements Tag, InsertingWithArgs {
        static final TagResolver RESOLVER = TagResolver.resolver("hover", Hover::create);

        @Override
        public @NotNull Component value(@NotNull List<Component> args) {
            return Component.text("", Style.style(HoverEvent.showText(this.partial.resolve(args))));
        }

        static Tag create(final ArgumentQueue args, final Context ctx) throws ParsingException {
            final String actionName = args.popOr("Hover event requires an action as its first argument").value();
            final HoverEvent.Action<Object> action = (HoverEvent.Action<Object>) HoverEvent.Action.NAMES.value(actionName);
            final ActionHandler<?> value = actionHandler(action);
            if (value == null) {
                throw ctx.newException("Don't know how to turn '" + args + "' into a hover event", args);
            }

            if (value instanceof ShowText showText) {
                var content = (Tree) value.parse(args, ctx);
                if (content instanceof Tree.Value constantValue) {
                    // The content has no arguments, so we can resolve it now and return the constant.
                    return Tag.styling(HoverEvent.showText(constantValue.comp()));
                }

                return new Hover(content); // It is dynamic, very sad
            } else {
                return Tag.styling(HoverEvent.hoverEvent(action, value.parse(args, ctx)));
            }
        }

        // Below is all standard minimessage parsing logic.

        interface ActionHandler<V> {
            @NotNull V parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException;
        }

        @SuppressWarnings("unchecked")
        static @Nullable <V> ActionHandler<V> actionHandler(final HoverEvent.Action<V> action) {
            ActionHandler<?> ret = null;
            if (action == HoverEvent.Action.SHOW_TEXT) {
                ret = ShowText.INSTANCE;
            } else if (action == HoverEvent.Action.SHOW_ITEM) {
                ret = ShowItem.INSTANCE;
            } else if (action == HoverEvent.Action.SHOW_ENTITY) {
                ret = ShowEntity.INSTANCE;
            }

            return (ActionHandler<V>) ret;
        }

        static final class ShowText implements ActionHandler<Tree> {
            private static final ShowText INSTANCE = new ShowText();

            @Override
            public @NotNull Tree parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
                MessageParser parser = Objects.requireNonNull(ctx.target()).get(MessageParser.POINTER).orElseThrow();
                return parser.parse(args.popOr("show_text action requires a message").value());
            }
        }

        static final class ShowItem implements ActionHandler<HoverEvent.ShowItem> {
            private static final ShowItem INSTANCE = new ShowItem();

            @Override
            public HoverEvent.@NotNull ShowItem parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
                try {
                    final Key key = Key.key(args.popOr("Show item hover needs at least an item ID").value());
                    final int count = args.hasNext() ? args.pop().asInt().orElseThrow(() -> ctx.newException("The count argument was not a valid integer")) : 1;
                    if (args.hasNext()) {
                        return HoverEvent.ShowItem.showItem(key, count, BinaryTagHolder.binaryTagHolder(args.pop().value()));
                    } else {
                        return HoverEvent.ShowItem.showItem(key, count);
                    }
                } catch (final InvalidKeyException | NumberFormatException ex) {
                    throw ctx.newException("Exception parsing show_item hover", ex, args);
                }
            }
        }

        static final class ShowEntity implements ActionHandler<HoverEvent.ShowEntity> {
            static final ShowEntity INSTANCE = new ShowEntity();

            private ShowEntity() {
            }

            @Override
            public HoverEvent.@NotNull ShowEntity parse(final @NotNull ArgumentQueue args, final @NotNull Context ctx) throws ParsingException {
                try {
                    final Key key = Key.key(args.popOr("Show entity needs a type argument").value());
                    final UUID id = UUID.fromString(args.popOr("Show entity needs an entity UUID").value());
                    if (args.hasNext()) {
                        final Component name = ctx.deserialize(args.pop().value());
                        return HoverEvent.ShowEntity.showEntity(key, id, name);
                    }
                    return HoverEvent.ShowEntity.showEntity(key, id);
                } catch (final IllegalArgumentException | InvalidKeyException ex) {
                    throw ctx.newException("Exception parsing show_entity hover", ex, args);
                }
            }
        }
    }

}
