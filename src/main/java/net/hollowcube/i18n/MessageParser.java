package net.hollowcube.i18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.internal.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.RootNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@ApiStatus.Internal
public final class MessageParser {
    static final Pointer<MessageParser> POINTER = Pointer.pointer(MessageParser.class,
            Key.key("hollowcube", "message_parser"));

    private final MiniMessage miniMessageDefault;
    private final MiniMessage miniMessagePartial;

    public MessageParser(boolean strict) {
        this.miniMessageDefault = MiniMessage.builder()
                .strict(strict)
                .build();
        this.miniMessagePartial = MiniMessage.builder()
                .tags(TagResolvable.RESOLVER)
                .strict(strict)
                .build();
    }

    public @NotNull Tree parse(@NotNull String miniMessageText) {
        // This parser is actually significantly slower than MiniMessage, so if we encounter a
        // string which has no arguments, just run minimessage on it and return it.
        if (!Util.containsArgs(miniMessageText))
            return staticParse(miniMessageText);

        // Otherwise there are args so good to resolve :)
        return partialParse(miniMessageText);
    }

    private @NotNull Tree staticParse(@NotNull String miniMessageText) {
        return new Tree.Value(miniMessageDefault.deserialize(miniMessageText));
    }

    private @NotNull Tree partialParse(@NotNull String miniMessageText) {
        Pointered context = new Util.SinglePointer<>(POINTER, this);
        ElementNode root = (ElementNode) miniMessagePartial.deserializeToTree(miniMessageText, context);
        Object/*Tree[] or Component[]*/ result = partialParseRecursive(root); //
        if (result instanceof Component[] comps) {
            if (comps.length == 0) return new Tree.Value(Component.empty());
            else if (comps.length == 1) return new Tree.Value(comps[0]);
            else return new Tree.Value(Component.empty().children(List.of(comps)));
        }
        // Otherwise it must be a tree slice
        Tree[] trees = (Tree[]) result;
        if (trees.length == 0) return new Tree.Value(Component.empty());
        else if (trees.length == 1) return trees[0];
        else return new Tree.Text(Component.empty(), (Tree[]) result);
    }

    // The API in here is pretty cursed. We need to flatten constant components but returning a
    // list we will lose that information. So instead we return either a Tree[] or a Component[]
    // which we can run instance checks on.

    private @NotNull Object/*Tree[] or Component[]*/ partialParseRecursive(@NotNull ElementNode node) {
        Object/*Tree[] or Component[]*/ children = partialParseChildren(node.unsafeChildren());

        if (node instanceof RootNode || node instanceof ValueNode) {
            Component comp = node instanceof ValueNode value ? Component.text(value.value()) : Component.empty();
            if (children instanceof Component[] comps) {
                // All children and this node are constant, so just flatten to one component.
                return new Component[]{comp.children(List.of(comps))};
            } else {
                // Something below is templated (maybe), preserve the tree.
                Tree[] trees = (Tree[]) children;
                if (trees.length == 0) return new Tree[]{new Tree.Value(comp)};
                else return new Tree[]{new Tree.Text(comp, trees)};
            }
        } else if (node instanceof TagNode tagNode) {
            return switch (tagNode.tag()) {
                case Inserting tag -> children instanceof Component[] comps
                        // Children are constant, so this is a constant component.
                        ? new Component[]{tag.value().children(List.of(comps))}
                        // Children may be dynamic, so return a tree.
                        : new Tree[]{new Tree.Text(tag.value(), (Tree[]) children)};
                case TagResolvable.InsertingWithArgs tag -> new Tree[]{new Tree.Insert(tag, children)};
                case TagResolvable.Placeholder tag -> {
                    // Placeholder tags do not pass styling down to their children, so we can just flatten them.
                    // That is, insert the placeholder first and children afterward as direct children to whatever
                    // is above.
                    Tree[] flattened;
                    if (children instanceof Component[] comps) {
                        flattened = new Tree[comps.length + 1];
                        for (int i = 0; i < comps.length; i++)
                            flattened[i + 1] = new Tree.Value(comps[i]);
                    } else {
                        Tree[] childrenTrees = (Tree[]) children;
                        flattened = new Tree[childrenTrees.length + 1];
                        System.arraycopy(childrenTrees, 0, flattened, 1, childrenTrees.length);
                    }
                    flattened[0] = new Tree.Placeholder(tag.index(), children);
                    yield flattened;
                }
                default -> throw new IllegalStateException("Unexpected tag type: " +
                        tagNode.tag().getClass().getName() + " (" + tagNode.tag() + ")");
            };
        } else {
            throw new IllegalStateException("Unexpected node type: " + node.getClass().getName() + " (" + node + ")");
        }
    }

    private @NotNull Object partialParseChildren(@NotNull List<ElementNode> children) {
        if (children.isEmpty()) return new Component[0];

        boolean isConstant = true;
        List<Tree> result = new ArrayList<>(children.size());
        for (ElementNode child : children) {
            Object/*Tree or Component*/ parsed = partialParseRecursive(child);
            if (parsed instanceof Component[] comps) {
                for (Component comp : comps) {
                    result.add(new Tree.Value(comp));
                }
            } else {
                isConstant = false;
                Collections.addAll(result, (Tree[]) parsed);
            }
        }

        if (isConstant) {
            Component[] comps = new Component[result.size()];
            for (int i = 0; i < result.size(); i++)
                comps[i] = ((Tree.Value) result.get(i)).comp();
            return comps;
        }

        return result.toArray(new Tree[0]);
    }

}
