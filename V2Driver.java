import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

public class V2Driver {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        boolean forceAnsi = arguments.remove("--ansi");
        boolean plain = arguments.remove("--plain");
        boolean animated = !plain && (forceAnsi || (System.console() != null
                && !"dumb".equals(System.getenv("TERM"))));
        boolean color = animated && System.getenv("NO_COLOR") == null;
        args = arguments.toArray(new String[0]);
        int threads;
        int games;
        try {
            if (args.length == 0) {
                Scanner input = new Scanner(System.in);
                System.out.print("How many worker threads? ");
                threads = Integer.parseInt(input.nextLine().trim());
                System.out.print("How many total games? ");
                games = Integer.parseInt(input.nextLine().trim());
            } else if (args.length == 2) {
                threads = Integer.parseInt(args[0]);
                games = Integer.parseInt(args[1]);
            } else {
                throw new IllegalArgumentException("Expected two arguments.");
            }
            if (threads <= 0 || games <= 0) {
                throw new IllegalArgumentException("Threads and games must be positive integers.");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());
            System.err.println("Usage: java V2Driver [threads totalGames] [--ansi|--plain]");
            return;
        }

        int workers = Math.min(threads, games);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        ExecutorCompletionService<VsV2Driver> completed = new ExecutorCompletionService<>(pool);
        TournamentDisplay display = new TournamentDisplay(games, workers, animated, color);
        List<VsV2Driver> running = new ArrayList<>();
        try {
            int firstGame = 0;
            for (int i = 0; i < workers; i++) {
                int count = games / workers + (i < games % workers ? 1 : 0);
                VsV2Driver worker = new VsV2Driver(count, firstGame);
                running.add(worker);
                completed.submit(() -> { worker.run(); return worker; });
                firstGame += count;
            }
            int finished = 0;
            display.render(0, 0, 0, false);
            while (finished < workers) {
                Future<VsV2Driver> result = completed.poll(120, TimeUnit.MILLISECONDS);
                if (result != null) {
                    result.get(); // Surface worker failures before displaying a final score.
                    finished++;
                }
                int p1Wins = 0;
                int p2Wins = 0;
                for (VsV2Driver worker : running) {
                    p1Wins += worker.getP1W();
                    p2Wins += worker.getP2W();
                }
                display.render(p1Wins, p2Wins, finished, finished == workers);
            }
        } catch (ExecutionException e) {
            System.err.println("Tournament failed; no final result. Cause: " + e.getCause());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            pool.shutdownNow();
        }
    }
}
