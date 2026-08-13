package net.infnetwork.snowball.cpscounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class CpsTabCompletion {
    private CpsTabCompletion() {
    }

    static List<String> complete(
            String[] args,
            boolean canManage,
            boolean playerSender,
            Collection<String> visiblePlayers) {
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>(visiblePlayers);
            if (canManage && playerSender) {
                candidates.add("#mon");
                candidates.add("#silent");
            }
            return matches(args[0], candidates);
        }
        if (args.length == 2
                && canManage
                && playerSender
                && args[0].equalsIgnoreCase("#mon")) {
            return matches(args[1], visiblePlayers);
        }
        return List.of();
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
}
