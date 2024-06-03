package net.hollowcube.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 10)
@Measurement(iterations = 2, time = 10)
@Fork(value = 2, warmups = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MultiArgComplex {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final MessageParser P = new MessageParser(false);

    private static final String MESSAGE = "<blue>Hello <green><0></green><gray> you just broke a <#adeeef><1></#adeeef></gray>! good job!";
    private static final List<Component> ARGS = List.of(Component.text("arg0"), Component.text("arg1"), Component.text("arg2"));
    private static final TagResolver[] ARGS_RESOLVERS = new TagResolver[ARGS.size()];

    static {
        for (int i = 0; i < ARGS.size(); i++) {
            ARGS_RESOLVERS[i] = Placeholder.component(String.valueOf(i), ARGS.get(i));
        }
    }

    @Benchmark
    public void mmParse(Blackhole bh) {
        bh.consume(MM.deserialize(MESSAGE, ARGS_RESOLVERS));
    }

    @Benchmark
    public void fastParseCold(Blackhole bh) {
        bh.consume(P.parse(MESSAGE).resolve(ARGS));
    }

    private static final Tree preParsed = P.parse(MESSAGE);

    @Benchmark
    public void fastParseReparse(Blackhole bh) {
        bh.consume(preParsed.resolve(ARGS));
    }

}
