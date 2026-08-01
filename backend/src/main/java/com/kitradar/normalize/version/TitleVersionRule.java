package com.kitradar.normalize.version;

import com.kitradar.domain.KitVersion;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class TitleVersionRule implements VersionRule {

    private static final Pattern KIDS = Pattern.compile("\\b(kids?|youth|junior)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WOMEN = Pattern.compile("\\b(women|womens|ladies)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RETRO_WORD = Pattern.compile("\\bretro\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("(?<!\\d)(?:18|19|20)\\d{2}(?!\\d)");
    private static final Pattern MASTER = Pattern.compile("\\bmaster(?:\\s+version)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER = Pattern.compile(
            "\\b(player|authentic|heat\\s?rdy|adv\\b|match\\s+version)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAN = Pattern.compile("\\b(fan\\s+version|stadium|replica|fv)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_SLEEVE = Pattern.compile("\\blong[ -]?sleeve\\b", Pattern.CASE_INSENSITIVE);

    private final Clock clock;

    public TitleVersionRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<KitVersion> resolve(VersionCandidate candidate) {
        String title = candidate.title();
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        for (RuleMatch rule : rules()) {
            if (rule.matches(title)) {
                return Optional.of(rule.version());
            }
        }
        return Optional.empty();
    }

    private List<RuleMatch> rules() {
        int latestRetroYear = Year.now(clock).getValue() - 4;
        return List.of(
                new RuleMatch(KitVersion.KIDS, title -> KIDS.matcher(title).find()),
                new RuleMatch(KitVersion.WOMEN, title -> WOMEN.matcher(title).find()),
                new RuleMatch(KitVersion.RETRO, title -> RETRO_WORD.matcher(title).find() || containsRetroYear(title, latestRetroYear)),
                new RuleMatch(KitVersion.MASTER, title -> MASTER.matcher(title).find()),
                new RuleMatch(KitVersion.PLAYER, title -> PLAYER.matcher(title).find()),
                new RuleMatch(KitVersion.FAN, title -> FAN.matcher(title).find()),
                new RuleMatch(KitVersion.LONG_SLEEVE, title -> LONG_SLEEVE.matcher(title).find()));
    }

    private boolean containsRetroYear(String title, int latestRetroYear) {
        Matcher matcher = FOUR_DIGIT_YEAR.matcher(title);
        while (matcher.find()) {
            if (Integer.parseInt(matcher.group()) <= latestRetroYear) {
                return true;
            }
        }
        return false;
    }

    private record RuleMatch(KitVersion version, TitleMatcher matcher) {
        private boolean matches(String title) {
            return matcher.matches(title);
        }
    }

    @FunctionalInterface
    private interface TitleMatcher {
        boolean matches(String title);
    }
}
