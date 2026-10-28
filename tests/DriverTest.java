/** Run with: javac -d out *.java tests/DriverTest.java && java -cp out DriverTest */
public class DriverTest {
    public static void main(String[] args) {
        checkStarter(true, "ABA");
        checkStarter(false, "BAB");
        Player stalled1 = new TestPlayer("A", new StringBuilder(), false);
        Player stalled2 = new TestPlayer("B", new StringBuilder(), false);
        try {
            VsV2Driver.playGame(stalled1, stalled2, true);
            throw new AssertionError("A stalled game must fail");
        } catch (IllegalStateException expected) {
            // A broken AI cannot keep a tournament running forever.
        }
        Thread.currentThread().interrupt();
        try {
            VsV2Driver.playGame(stalled1, stalled2, true);
            throw new AssertionError("An interrupted game must stop");
        } catch (java.util.concurrent.CancellationException expected) {
            // Preserve interruption for the executor's cancellation path.
        } finally {
            Thread.interrupted();
        }
        System.out.println("Driver regression tests passed");
    }

    private static void checkStarter(boolean first, String expectedOrder) {
        StringBuilder order = new StringBuilder();
        Player a = new TestPlayer("A", order, true);
        Player b = new TestPlayer("B", order, true);
        Player winner = VsV2Driver.playGame(a, b, first);
        if (winner != (first ? a : b) || !expectedOrder.equals(order.toString())) {
            throw new AssertionError("Incorrect turn order or winner: " + order);
        }
    }

    private static class TestPlayer extends Player {
        private final StringBuilder order;
        private final boolean sinks;
        private int attacks;

        TestPlayer(String name, StringBuilder order, boolean sinks) {
            super(name);
            this.order = order;
            this.sinks = sinks;
            populateShips();
        }

        public void populateShips() {
            addShip(new PatrolBoat(new Location(0, 0), new Location(0, 1)));
        }

        public boolean attack(Player enemy, Location ignored) {
            order.append(getName());
            if (++attacks == 2 && sinks) {
                enemy.removeShip(enemy.getShip(new Location(0, 0)));
                return true;
            }
            return false;
        }
    }
}
