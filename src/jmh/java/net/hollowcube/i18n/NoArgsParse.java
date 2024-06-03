package net.hollowcube.i18n;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 10)
@Measurement(iterations = 1, time = 10)
@Fork(value = 1, warmups = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NoArgsParse {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final MessageParser P = new MessageParser(false);

    @Benchmark
    public void mmParseNoArgs(Blackhole bh) {
        bh.consume(MM.deserialize("<red>Hello, <blue>world!"));
    }

    @Benchmark
    public void fastParseNoArgs(Blackhole bh) {
        var tree = P.parse("<red>Hello, <blue>world!");
        bh.consume(tree.resolve(List.of()));
    }
}
