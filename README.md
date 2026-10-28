# BattleShip-AI
Winner of CSA Battle Ship AI Competition.
Java-based AI using a per-ship weighted rolling heatmap.

## Why Per-Ship?
By using a per-ship weighted heatmap, I was able to gain an advantage over AIs that placed ships in order. Placing ships in order leads to a detectable pattern of placement choice which was then picked up by may AI.

## Why a weighted rolling heatmap?
A weighted rolling heat map allowed me to track the previous games against an opponent. Through the thousands of games played, this setup yields a heat map that can pick up the patterns of the opponent for both placing ships and hitting ships. By tracking these statistics, my AI chose careful spots to place ships as well as optimal positions to guess, leading to hits.

## Run a threaded AI tournament

Run `V2Driver.main` in IntelliJ and enter the number of worker threads and the
**total number of games**. Alternatively, set program arguments to `4 1000`
to play 1,000 games across four workers. From a terminal:

```sh
mkdir -p out
javac -d out *.java
java -cp out V2Driver 4 1000
```

The driver alternates the starting player, divides the requested games among
workers (including any remainder), waits for every result, and prints combined
wins, percentages, and elapsed time. An odd total gives ExpertComp one extra
start. Worker errors abort the tournament rather than reporting partial wins as
a final result.

Each worker starts with fresh, separate temporary learning files and learns
across its own games. These files are deleted afterward; existing `R.txt`,
`Blue.txt`, and other saved AI histories are untouched. Changing the number of
workers changes the length of each learning series, so results are not directly
equivalent to one long single-threaded series. `VsV2Driver` now interprets its
count as individual games, rather than pairs.

To run the driver regression checks:

```sh
javac -d out *.java tests/DriverTest.java
java -cp out DriverTest
```

### Terminal scoreboard

The tournament now displays an AI Arena scoreboard: live match progress,
per-AI win bars and percentages, leader, completed workers, elapsed time,
games per second, and estimated time remaining. A terminal redraws the panel
in place; IntelliJ's regular Run console and redirected output get periodic
plain-text snapshots with no escape codes.

```sh
java -cp out V2Driver 4 1000         # automatic display mode
java -cp out V2Driver 4 1000 --ansi  # force animated terminal display
java -cp out V2Driver 4 1000 --plain # plain snapshots
```

For animation in IntelliJ, use its Terminal tab or a run configuration with
terminal emulation enabled, then add `--ansi` if needed. Set `NO_COLOR` to
disable colors. Win percentages are based on completed games; a final result
appears only after every worker succeeds.
