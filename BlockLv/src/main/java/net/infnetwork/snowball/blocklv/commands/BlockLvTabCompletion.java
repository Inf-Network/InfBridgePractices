package net.infnetwork.snowball.blocklv.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

final class BlockLvTabCompletion {
    private static final List<Subcommand> SUBCOMMANDS = List.of(
            new Subcommand("add", "blocklv.add", true, true, false),
            new Subcommand("addlevel", "blocklv.addlevel", true, true, false),
            new Subcommand("decrease", "blocklv.decrease", true, true, false),
            new Subcommand("decreaselevel", "blocklv.decreaselevel", true, true, false),
            new Subcommand("clear", "blocklv.clear", true, false, false),
            new Subcommand("setrank", "blocklv.setrank", false, false, true),
            new Subcommand("refresh", "blocklv.refresh", false, false, false));
    private static final List<String> AMOUNTS = List.of("1", "10", "100", "1000");

    private BlockLvTabCompletion() {
    }

    static List<String> complete(
            String[] args,
            Predicate<String> hasPermission,
            boolean playerSender,
            Collection<String> onlinePlayers) {
        if (args.length == 1) {
            List<String> allowed = SUBCOMMANDS.stream()
                    .filter(subcommand -> hasPermission.test(subcommand.permission()))
                    .filter(subcommand -> playerSender || !subcommand.playerOnly())
                    .map(Subcommand::name)
                    .toList();
            return matches(args[0], allowed);
        }
        if (args.length < 2) {
            return List.of();
        }

        Subcommand subcommand = find(args[0]);
        if (subcommand == null
                || !hasPermission.test(subcommand.permission())
                || (subcommand.playerOnly() && !playerSender)) {
            return List.of();
        }
        if (args.length == 2 && subcommand.playerArgument()) {
            return matches(args[1], onlinePlayers);
        }
        if (args.length == 3 && subcommand.amountArgument()) {
            return matches(args[2], AMOUNTS);
        }
        return List.of();
    }

    private static Subcommand find(String name) {
        for (Subcommand subcommand : SUBCOMMANDS) {
            if (subcommand.name().equalsIgnoreCase(name)) {
                return subcommand;
            }
        }
        return null;
    }

    private static List<String> matches(String input, Collection<String> candidates) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(candidate);
            }
        }
        matches.sort(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)));
        return List.copyOf(matches);
    }

    private record Subcommand(
            String name,
            String permission,
            boolean playerArgument,
            boolean amountArgument,
            boolean playerOnly) {
    }
}
