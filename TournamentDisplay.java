import java.util.Locale;

/** Rendered only by the coordinator; workers never write to the console. */
final class TournamentDisplay {
    private final int games;
    private final int workers;
    private final boolean animated;
    private final boolean color;
    private final long start = System.nanoTime();
    private long lastRender;
    private int previousLines;
    private int frame;

    TournamentDisplay(int games, int workers, boolean animated, boolean color) {
        this.games = games;
        this.workers = workers;
        this.animated = animated;
        this.color = color;
    }

    void render(int expert, int ilo, int finishedWorkers, boolean done) {
        long now = System.nanoTime();
        if (!done && lastRender != 0 && now - lastRender < (animated ? 120_000_000L : 2_000_000_000L)) {
            return;
        }
        lastRender = now;
        int played = expert + ilo;
        double seconds = (now - start) / 1_000_000_000.0;
        double rate = played / Math.max(seconds, 0.001);
        String eta = played == 0 ? "--" : duration((games - played) / rate);
        String status = done ? "COMPLETE" : "LIVE " + "|/-\\".charAt(frame++ % 4);
        String leader = expert == ilo ? "Tied" : (expert > ilo ? "ExpertComp" : "IloWawa")
                + " leads by " + Math.abs(expert - ilo);
        if (done) leader = expert == ilo ? "DRAW" : (expert > ilo ? "ExpertComp" : "IloWawa") + " WINS";
        String[] lines = {
            "+----------------------------------------------------------+",
            "  B A T T L E S H I P   /   A I   A R E N A",
            "  ~~~~~~~  __/___  ~~~~~~~       " + status,
            "+----------------------------------------------------------+",
            format("  MATCHES  %s %6.2f%%", bar(played, games, 26), 100.0 * played / games),
            format("           %,d / %,d played  |  %d / %d workers done", played, games, finishedWorkers, workers),
            "",
            format("  ExpertComp  %s  %,d wins  %6.2f%%", bar(expert, played, 18), expert, percent(expert, played)),
            format("  IloWawa     %s  %,d wins  %6.2f%%", bar(ilo, played, 18), ilo, percent(ilo, played)),
            "  " + leader,
            "",
            format("  TIME %s   |   %.1f games/s   |   ETA %s", duration(seconds), rate, done ? "0:00" : eta),
            "  Alternating starts / independent learning per worker",
            "+----------------------------------------------------------+"
        };
        StringBuilder output = new StringBuilder();
        if (animated && previousLines > 0) output.append("\033[").append(previousLines).append('A');
        for (int i = 0; i < lines.length; i++) {
            if (animated) output.append("\r\033[2K");
            if (color) output.append(i == 7 ? "\033[96m" : i == 8 ? "\033[95m" : "\033[36m");
            output.append(lines[i]);
            if (color) output.append("\033[0m");
            output.append(System.lineSeparator());
        }
        System.out.print(output);
        System.out.flush();
        previousLines = lines.length;
    }

    private static String bar(int value, int total, int width) {
        int filled = total == 0 ? 0 : (int) ((long) value * width / total);
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < width; i++) result.append(i < filled ? '#' : '.');
        return result.append(']').toString();
    }

    private static double percent(int value, int total) {
        return total == 0 ? 0 : 100.0 * value / total;
    }

    private static String duration(double seconds) {
        long whole = (long) seconds;
        return format("%d:%02d", whole / 60, whole % 60);
    }

    private static String format(String template, Object... values) {
        return String.format(Locale.ROOT, template, values);
    }
}
