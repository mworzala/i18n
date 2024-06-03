# Internationalization

[![license](https://img.shields.io/github/license/Minestom/MinestomDataGenerator.svg)](LICENSE)

> [!WARNING]
> Still in development and not ready for use.

An opinionated (and fast) translation library based on [Adventure](https://github.com/KyoriPowered/adventure)
[MiniMessage](https://docs.advntr.dev/minimessage/index.html).

## Install

Releases will be available on Maven Central in the future.

## Usage

todo add me

## Performance

This library uses very aggressive caching of partial component results with unsubstituted arguments to be extremely
fast at substituting arguments repeatedly. This is the most common case for translations where components are reused
with different arguments (or no arguments, which is also handled appropriately).

The following benchmark is based on the minimessage string `<blue>Hello <green><0></green><gray> you just broke a
<#adeeef><1></#adeeef></gray>! good job!`. The benchmarks are as follows:

* `fastParseCold`: Parses the string from scratch each time using i18n systems, and then substitutes the arguments.
* `fastParseReparse`: Substitutes arguments into the pre-parsed output of the i18n parser. This is the most common case.
* `mmParse`: Parses and substitutes the string using MiniMessage directly (and the `TagResolver` api for placeholders).

```
Benchmark         Mode  Cnt      Score     Error  Units
fastParseCold     avgt    4  25697.926 ± 430.903  ns/op
fastParseReparse  avgt    4    161.981 ±   4.664  ns/op
mmParse           avgt    4   9999.215 ± 115.362  ns/op
```

## Contributing

Contributions via PRs and issues are always welcome.

## License

This project is licensed under the [MIT License](LICENSE).
