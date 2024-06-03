package net.hollowcube.i18n;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public sealed interface Tree {

    @NotNull Component resolve(@NotNull List<Component> args);

    /**
     * A static value in the tree.
     */
    record Value(@NotNull Component comp) implements Tree {
        @Override
        public @NotNull Component resolve(@NotNull List<Component> args) {
            return comp;
        }
    }

    /**
     * A constant piece of text with some dynamic children. If children are constant, Value should be used.
     */
    record Text(@NotNull Component text, @NotNull Tree[] children) implements Tree {
        @Override
        public @NotNull Component resolve(@NotNull List<Component> args) {
            List<Component> children = new ArrayList<>(text.children().size() + this.children.length);
            children.addAll(text.children());
            for (Tree child : this.children) {
                children.add(child.resolve(args));
            }
            return text.children(children);
        }
    }

    record Insert(@NotNull TagResolvable.InsertingWithArgs tag,
                  @NotNull Object/*Tree[] or Component[]*/ children) implements Tree {
        @Override
        public @NotNull Component resolve(@NotNull List<Component> args) {
            Component comp = tag.value(args);
            List<Component> children;
            if (this.children instanceof Component[] comps) {
                children = new ArrayList<>(comp.children().size() + comps.length);
                children.addAll(comp.children());
                Collections.addAll(children, comps);
            } else {
                Tree[] trees = (Tree[]) this.children;
                children = new ArrayList<>(comp.children().size() + trees.length);
                children.addAll(comp.children());
                for (Tree tree : trees) children.add(tree.resolve(args));
            }

            return comp.children(children);
        }
    }

    record Placeholder(int index, @NotNull Object/*Tree[] or Component[]*/ children) implements Tree {
        @Override
        public @NotNull Component resolve(@NotNull List<Component> args) {
            return index >= args.size() ? Component.text("$$" + index) : args.get(index);
        }
    }

}
