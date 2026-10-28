import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class IloWawa extends Player
{
    // Persistent data
    private String opponentName;
    private int gameNumber;

    private Offense offense;
    private Defense defense;

    private final File stateFile;

    public IloWawa(String name) {
        this(name, new File(name + ".txt"));
    }

    public IloWawa(String name, File stateFile) {
        super(name);
        this.stateFile = stateFile;
        populateShips();
    }

    /**
     * Because populateShips is called in the Player constructor, I'm using this
     * as a fake constructor and initializing stuff here.
     */
    @Override
    public void populateShips() {
        this.load();

        Ship[] ships = this.defense.createShips();

        for (Ship ship : ships) {
            this.addShip(ship);
        }
    }

    @Override
    public boolean attack(Player enemy, Location _loc) {
        // If this is a new match, reinitialize the offense and defense.
        if (!enemy.getName().equals(this.opponentName)) {
            this.opponentName = enemy.getName();
            this.gameNumber = 1;
            this.offense = new Offense(this, null, null);
            this.defense = new Defense(this, null);

            this.save();
        }

        Location loc = this.offense.getNextAttackLocation();
        Offense.SeaState result;
        List<Location> sunkLocations = null;

        if (enemy.hasShipAtLocation(loc)) {
            Ship ship = enemy.getShip(loc);
            ship.takeHit(loc);

            getGuessBoard()[loc.getRow()][loc.getCol()] = 1;

            if (ship.isSunk()) {
                enemy.removeShip(ship);
                sunkLocations = ship.getLocations();
                result = Offense.SeaState.SUNK;
            } else {
                result = Offense.SeaState.HIT;
            }
        } else {
            getGuessBoard()[loc.getRow()][loc.getCol()] = -1;

            result = Offense.SeaState.MISS;
        }

        this.offense.handleResult(loc, result, sunkLocations);

        return result != Offense.SeaState.MISS;
    }

    @Override
    public boolean hasShipAtLocation(Location loc) {
        boolean hasShip = super.hasShipAtLocation(loc);

        this.defense.handleAttack(loc, hasShip);

        return hasShip;
    }

    /**
     * Manages attacks over the course of a match.
     */
    private static class Offense
    {
        IloWawa player;

        // Persistent data
        int[][] rollingHitsHeatMap;
        int[][] rollingMissesHeatMap;

        int hitsGiven;
        SeaState[][] gameState;
        ArrayList<Integer> shipsLeft;

        Offense(IloWawa player,
                int[][] rollingHitsHeatMap,
                int[][] rollingMissesHeatMap) {
            this.player = player;
            this.rollingHitsHeatMap = rollingHitsHeatMap == null
                    ? new int[10][10]
                    : rollingHitsHeatMap;
            this.rollingMissesHeatMap = rollingMissesHeatMap == null
                    ? new int[10][10]
                    : rollingMissesHeatMap;

            this.hitsGiven = 0;
            this.gameState = new SeaState[10][10];
            this.shipsLeft = new ArrayList<>(Arrays.asList(5, 4, 3, 3, 2));

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    this.gameState[i][j] = SeaState.UNKNOWN;
                }
            }
        }

        /**
         * Determines the next location to attack.
         *
         * @return The next location to attack.
         */
        Location getNextAttackLocation() {
            int[][] heatMap = createHeatMap();

            return pickBestLocation(heatMap);
        }

        /**
         * Picks the best location to attack based on the heatmap. If there are
         * multiple locations with the same score, one is chosen at random.
         *
         * @param heatMap The heatmap.
         * @return The best location to attack.
         */
        private Location pickBestLocation(int[][] heatMap) {
            int max = 0;
            int count = 0;

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (heatMap[i][j] > max) {
                        max = heatMap[i][j];
                        count = 1;
                    } else if (heatMap[i][j] == max) {
                        count++;
                    }
                }
            }

            int index = (int) (Math.random() * count);

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (heatMap[i][j] == max) {
                        if (index == 0)
                            return new Location(i, j);
                        else
                            index--;
                    }
                }
            }

            return null;
        }

        /**
         * Creates a heatmap of the opponent's ship locations.
         *
         * @return The heatmap.
         */
        private int[][] createHeatMap() {
            int[][] heatMap = new int[10][10];

            addOpenSpaces(heatMap);
            applyRollingHeatMaps(heatMap);
            addHitSpaces(heatMap);

            return heatMap;
        }

        /**
         * Scales values in the heatmap based on the rolling heatmap. Values are
         * scaled by a factor from 1 to 5 from the number of hits to the number
         * of misses on a space.
         *
         * @param heatMap The heatmap to modify.
         */
        private void applyRollingHeatMaps(int[][] heatMap) {
            double sum = 0;

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    sum += this.rollingHitsHeatMap[i][j] + this.rollingMissesHeatMap[i][j];
                }
            }

            if (sum == 0)
                return;

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    int total = this.rollingHitsHeatMap[i][j] + this.rollingMissesHeatMap[i][j];

                    if (total == 0)
                        continue;

                    double factor = 1 + (9 * this.rollingHitsHeatMap[i][j]) / (double) total;

                    heatMap[i][j] = (int) (heatMap[i][j] * factor);
                }
            }
        }

        /**
         * For each unknown space, add the number of possible arrangements of
         * ships that could be there.
         *
         * @param heatMap The heatmap to modify.
         */
        private void addOpenSpaces(int[][] heatMap) {
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    if (this.gameState[row][col] != SeaState.UNKNOWN)
                        continue;

                    int left = 0;
                    while (col - left >= 0 && this.gameState[row][col - left] == SeaState.UNKNOWN)
                        left++;

                    int right = 0;
                    while (col + right < 10 && this.gameState[row][col + right] == SeaState.UNKNOWN)
                        right++;

                    int up = 0;
                    while (row - up >= 0 && this.gameState[row - up][col] == SeaState.UNKNOWN)
                        up++;

                    int down = 0;
                    while (row + down < 10 && this.gameState[row + down][col] == SeaState.UNKNOWN)
                        down++;

                    for (int length : this.shipsLeft) {
                        int l = Math.min(left, length);
                        int r = Math.min(right, length);
                        int u = Math.min(up, length);
                        int d = Math.min(down, length);

                        int width = l + r - 1;
                        int height = u + d - 1;

                        if (width >= length)
                            heatMap[row][col] += width - length + 1;

                        if (height >= length)
                            heatMap[row][col] += height - length + 1;
                    }
                }
            }
        }

        /**
         * For each hit space, increase the weight of the surrounding spaces
         * based on the number of ships that could be there.
         *
         * @param heatMap The heatmap to modify.
         */
        private void addHitSpaces(int[][] heatMap) {
            final int WEIGHT = 50;

            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    if (this.gameState[row][col] != SeaState.HIT)
                        continue;

                    int left = 0;
                    while (col - left >= 0 && (
                            this.gameState[row][col - left] == SeaState.UNKNOWN ||
                                    this.gameState[row][col - left] == SeaState.HIT))
                        left++;

                    int right = 0;
                    while (col + right < 10 && (
                            this.gameState[row][col + right] == SeaState.UNKNOWN ||
                                    this.gameState[row][col + right] == SeaState.HIT))
                        right++;

                    int up = 0;
                    while (row - up >= 0 && (
                            this.gameState[row - up][col] == SeaState.UNKNOWN ||
                                    this.gameState[row - up][col] == SeaState.HIT))
                        up++;

                    int down = 0;
                    while (row + down < 10 && (
                            this.gameState[row + down][col] == SeaState.UNKNOWN ||
                                    this.gameState[row + down][col] == SeaState.HIT))
                        down++;

                    for (int length : this.shipsLeft) {
                        int l = Math.min(left, length);
                        int r = Math.min(right, length);
                        int u = Math.min(up, length);
                        int d = Math.min(down, length);

                        for (int i = col - l + 1; i <= col + r - 1; i++)
                            if (this.gameState[row][i] == SeaState.UNKNOWN)
                                heatMap[row][i] += WEIGHT;

                        for (int i = row - u + 1; i <= row + d - 1; i++)
                            if (this.gameState[i][col] == SeaState.UNKNOWN)
                                heatMap[i][col] += WEIGHT;
                    }
                }
            }
        }

        /**
         * Given a location and the result of an attack, update the game state
         * and rolling heatmap.
         *
         * @param loc The location of the attack.
         * @param result The result of the attack.
         * @param sunkLocations The locations of the ship that was sunk. If no
         *                      ship was sunk, this should be null.
         */
        void handleResult(Location loc, SeaState result, List<Location> sunkLocations) {
            this.hitsGiven++;

            int row = loc.getRow();
            int col = loc.getCol();

            this.gameState[row][col] = result;

            // Weight earlier games more heavily.
            int gameNumber = this.player.getGameNumber();
            int weight = Math.max(1, (10 - gameNumber) / 2);

            if (result == SeaState.MISS) {
                this.rollingMissesHeatMap[row][col] += weight;
            } else {
                this.rollingHitsHeatMap[row][col] += weight;
            }

            if (result == SeaState.SUNK) {
                int length = sunkLocations.size();

                this.shipsLeft.remove((Integer) length);

                for (Location sunkLoc : sunkLocations) {
                    this.gameState[sunkLoc.getRow()][sunkLoc.getCol()] =
                            SeaState.SUNK;
                }
            }

            // Save at 16 hits just in case.
            if (this.hitsGiven >= 16) {
                this.player.save();
            }
        }

        enum SeaState {
            UNKNOWN,
            MISS,
            HIT,
            SUNK
        }
    }

    /**
     * Manages ship arrangement over the course of a match.
     */
    private static class Defense
    {
        IloWawa player;
        int hitsTaken;
        int attacksTaken;

        // Persistent data
        int[][] rollingHeatMap;

        Defense(IloWawa player, int[][] rollingHeatMap) {
            this.player = player;
            this.hitsTaken = 0;
            this.rollingHeatMap = rollingHeatMap == null
                    ? new int[10][10]
                    : rollingHeatMap;
        }

        /**
         * Handles a hit when the opponent asks if a ship is at the location.
         *
         * @param loc The location the opponent is hitting.
         */
        void handleAttack(Location loc, boolean isHit) {
            this.attacksTaken++;
            if (isHit)
                this.hitsTaken++;

            // Attacks earlier into the game are more important, and we should
            // weight earlier games more heavily.
            int gameWeight = Math.max(1, 10 - this.player.getGameNumber());
            int attackWeight = (int) Math.sqrt(100 * Math.max(100 - this.attacksTaken, 1));

            this.rollingHeatMap[loc.getRow()][loc.getCol()] += gameWeight * attackWeight;

            // Save at 16 hits taken in case defense is loaded after the first
            // hit has already been taken.
            if (this.hitsTaken >= 16) {
                this.player.save();
            }
        }

        /**
         * Generate a new ship configuration for the current game.
         *
         * @return The ship configuration to use.
         */
        Ship[] createShips() {
            final int TESTS = 1000;

            Ship[] bestShips = generateRandomShips();
            int minScore = scoreConfiguration(bestShips);

            for (int i = 0; i < TESTS; i++) {
                Ship[] ships = generateRandomShips();
                int score = scoreConfiguration(ships);

                if (score < minScore) {
                    bestShips = ships;
                    minScore = score;
                }
            }

            return bestShips;
        }

        /**
         * Scores a configuration of ships. A score is generated by summing the
         * densities on the normalized opponent squares heatmap.
         *
         * @param ships The ship configuration to score.
         * @return The score. Lower is better.
         */
        private int scoreConfiguration(Ship[] ships) {
            int score = 0;
            int max = 0;

            for (Ship ship : ships) {
                int length = ship.getLocations().size();

                for (Location loc : ship.getLocations()) {
                    int value = this.rollingHeatMap[loc.getRow()][loc.getCol()];

                    score += value / length;

                    if (value > max) {
                        max = value;
                    }
                }
            }

            return score;
        }

        /**
         * Generates a random ship configuration.
         *
         * @return The ship configuration.
         */
        private Ship[] generateRandomShips() {
            int[] lengths = {5, 4, 3, 3, 2};
            Ship[] ships = {
                    new AircraftCarrier(),
                    new Destroyer(),
                    new Submarine(),
                    new Cruiser(),
                    new PatrolBoat()
            };

            for (int i = 0; i < 5; i++) {
                Location start = new Location((int) (Math.random() * 10), (int) (Math.random() * 10));
                int length = lengths[i];
                boolean isHorizontal = Math.random() > 0.5;

                while (!isValidShip(ships, start, length, isHorizontal)) {
                    start = new Location((int) (Math.random() * 10), (int) (Math.random() * 10));
                    length = lengths[i];
                    isHorizontal = Math.random() > 0.5;
                }

                Location[] locations = generateLocations(start, length, isHorizontal);

                ships[i].addLocation(locations);
            }

            return ships;
        }

        /**
         * Given the existing ships, a start location, the length of the ship,
         * and the orientation, determine whether the three parameters form a
         * valid ship arrangement. This also adds the additional requirement
         * that ships cannot be adjacent to each other.
         *
         * @param ships The ships to test against.
         * @param start The ship's start position.
         * @param length The length of the ship.
         * @param isHorizontal Whether the ship is horizontal.
         * @return If the ship's placement is valid.
         */
        private boolean isValidShip(Ship[] ships, Location start, int length, boolean isHorizontal) {
            Location[] locations = generateLocations(start, length, isHorizontal);

            for (Location location : locations) {
                int row = location.getRow();
                int col = location.getCol();

                if (row < 0 || row > 9 || col < 0 || col > 9)
                    return false;

                for (Ship ship : ships)
                    for (Location loc : ship.getLocations())
                        if (isAdjacent(location, loc))
                            return false;
            }

            return true;
        }

        /**
         * Given a start location, the length of the ship, and whether the ship
         * is horizontal, return an array of the ship's locations.
         *
         * @param start The ship's start location.
         * @param length The ship's length.
         * @param isHorizontal Whether the ship is horizontal.
         * @return An array of the ship's locations.
         */
        private Location[] generateLocations(Location start, int length, boolean isHorizontal) {
            int dr = isHorizontal ? 0 : 1;
            int dc = isHorizontal ? 1 : 0;

            Location[] locations = new Location[length];

            for (int i = 0; i < length; i++) {
                locations[i] = new Location(
                        start.getRow() + dr * i,
                        start.getCol() + dc * i
                );
            }

            return locations;
        }

        /**
         * Determines if two locations are adjacent.
         *
         * @param loc1 The first location.
         * @param loc2 The second location.
         */
        private boolean isAdjacent(Location loc1, Location loc2) {
            int row1 = loc1.getRow();
            int col1 = loc1.getCol();
            int row2 = loc2.getRow();
            int col2 = loc2.getCol();

            return (row1 == row2 && Math.abs(col1 - col2) == 1)
                    || (col1 == col2 && Math.abs(row1 - row2) == 1);
        }
    }

    /**
     * Saves the player's persistent state to a file.
     */
    private void save() {
        File file = stateFile;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(stringify());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts the persistent state of {@link Offense} and {@link Defense} into
     * a String to be saved. The format is as follows:
     *
     * <pre>
     * Opponent Name
     * Game Number
     * Offense Rolling Heat Map
     * Defense Rolling Heat Map
     * </pre>
     *
     * The heat maps are saved as a 10x10 grid of integers, with each row on a
     * separate line.
     *
     * @return The String representation of the persistent state.
     */
    private String stringify() {
        StringBuilder output = new StringBuilder();

        output.append(this.opponentName).append("\n");
        output.append(this.gameNumber).append("\n");

        output.append(stringifyHeatMap(this.offense.rollingHitsHeatMap));
        output.append(stringifyHeatMap(this.offense.rollingMissesHeatMap));
        output.append(stringifyHeatMap(this.defense.rollingHeatMap));

        // Remove final newline
        return output.substring(0, output.length() - 1);
    }

    /**
     * Converts a 2D array of ints into a String.
     *
     * @param heatMap The 2D array of ints to convert.
     * @return The String representation of the 2D array.
     */
    private static StringBuilder stringifyHeatMap(int[][] heatMap) {
        StringBuilder output = new StringBuilder();

        for (int[] row : heatMap) {
            for (int col : row) {
                output.append(col).append(" ");
            }

            output.append("\n");
        }

        return output;
    }

    /**
     * Loads the current data and initializes the {@link Offense} and
     * {@link Defense} classes for the current instance.
     */
    private void load() {
        File file = stateFile;

        if (!file.exists()) {
            this.opponentName = Double.toString(Math.random());
            this.gameNumber = 1;
            this.offense = new Offense(this, null, null);
            this.defense = new Defense(this, null);

            this.save();

            return;
        }
                                  
        try (Scanner input = new Scanner(file)) {
            this.opponentName = input.nextLine();
            this.gameNumber = Integer.parseInt(input.nextLine()) + 1;

            int[][] offenseHitsHeatMap = new int[10][10];
            int[][] offenseMissesHeatMap = new int[10][10];
            int[][] defenseHeatMap = new int[10][10];

            for (int i = 0; i < 10; i++) {
                String[] row = input.nextLine().split(" ");

                for (int j = 0; j < 10; j++) {
                    offenseHitsHeatMap[i][j] = Integer.parseInt(row[j]);
                }
            }

            for (int i = 0; i < 10; i++) {
                String[] row = input.nextLine().split(" ");

                for (int j = 0; j < 10; j++) {
                    offenseMissesHeatMap[i][j] = Integer.parseInt(row[j]);
                }
            }

            for (int i = 0; i < 10; i++) {
                String[] row = input.nextLine().split(" ");

                for (int j = 0; j < 10; j++) {
                    defenseHeatMap[i][j] = Integer.parseInt(row[j]);
                }
            }

            this.offense = new Offense(this, offenseHitsHeatMap, offenseMissesHeatMap);
            this.defense = new Defense(this, defenseHeatMap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the game number.
     *
     * @return The game number.
     */
    private int getGameNumber() {
        return gameNumber;
    }
}