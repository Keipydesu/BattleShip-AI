import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

/** A sequential learning series; each worker owns its AI state files. */
public class VsV2Driver implements Runnable {
    private final int games;
    private final int firstGame;
    private volatile int p1W;
    private volatile int p2W;

    public VsV2Driver(int games) {
        this(games, 0);
    }

    public VsV2Driver(int games, int firstGame) {
        if (games <= 0 || firstGame < 0) {
            throw new IllegalArgumentException("Games must be positive and firstGame nonnegative.");
        }
        this.games = games;
        this.firstGame = firstGame;
    }

    public int getP1W() { return p1W; }
    public int getP2W() { return p2W; }

    @Override
    public void run() {
        p1W = 0;
        p2W = 0;
        try {
            Path directory = Files.createTempDirectory("battleship-worker-");
            Path expertState = directory.resolve("ExpertComp.txt");
            Path iloState = directory.resolve("IloWawa.txt");
            try {
                for (int i = 0; i < games; i++) {
                    checkInterrupted();
                    Player p1 = new ExpertComp("ExpertComp", expertState.toFile());
                    Player p2 = new IloWawa("IloWawa", iloState.toFile());
                    Player winner = playGame(p1, p2, ((long) firstGame + i) % 2 == 0);
                    if (winner == p1) p1W++;
                    else p2W++;
                }
            } finally {
                Files.deleteIfExists(expertState);
                Files.deleteIfExists(iloState);
                Files.deleteIfExists(directory);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not manage worker learning files", e);
        }
    }

    static Player playGame(Player p1, Player p2, boolean p1Turn) {
        Battleship battleship = new Battleship();
        battleship.addPlayer(p1);
        battleship.addPlayer(p2);
        int turns = 0;
        while (!battleship.gameOver()) {
            checkInterrupted();
            // Each player has 100 cells. Repeated guesses must not hang a run.
            if (++turns > 200) {
                throw new IllegalStateException("Game exceeded 200 turns; an AI may be repeating shots.");
            }
            if (p1Turn) p1.attack(p2, new Location(0, 0));
            else p2.attack(p1, new Location(0, 0));
            battleship.upkeep();
            p1Turn = !p1Turn;
        }
        return battleship.getWinner();
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Tournament interrupted");
        }
    }
}
