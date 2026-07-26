package org.quinnton.chess.headless;

import org.quinnton.chess.bot.Bot;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Masks;
import org.quinnton.chess.core.SoundsPlayer;

/**
 * Headless entry point: plays one or more bot-vs-bot games with no JavaFX.
 *
 * <p>Run with Maven exec, e.g.:
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=org.quinnton.chess.headless.HeadlessMain \
 *       -Dexec.args="--games 10 --depth 5 --budget-ms 1000"
 * </pre>
 *
 * <p>Options (all optional):
 * <ul>
 *   <li>{@code --games N}      number of games to play (default 1)</li>
 *   <li>{@code --depth N}      max search depth / iterative-deepening ceiling (default 5)</li>
 *   <li>{@code --budget-ms N}  per-move wall-clock budget in ms (default 1000);
 *       use {@code 0} for an exhaustive fixed-depth search, the only mode that is
 *       bit-for-bit reproducible from {@code --seed}</li>
 *   <li>{@code --max-plies N}  hard cap on half-moves per game (default 600)</li>
 *   <li>{@code --seed N}       base seed for tie-breaking; with {@code --budget-ms 0}
 *       this makes the whole run reproducible. Omit for random</li>
 *   <li>{@code --fen "..."}    starting position (default the standard start position)</li>
 *   <li>{@code --log-moves}    print every move as it is played</li>
 * </ul>
 */
public final class HeadlessMain {

    private static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static void main(String[] args) {
        Config cfg = Config.parse(args);

        // No UI: keep the move path off the audio subsystem entirely.
        SoundsPlayer.setEnabled(false);

        Masks masks = new Masks();
        HeadlessGameRunner runner =
                new HeadlessGameRunner(cfg.depth, cfg.budgetMillis, cfg.maxPlies, cfg.logMoves);

        int whiteWins = 0, blackWins = 0, draws = 0, unfinished = 0;

        System.out.printf("Playing %d game(s): depth=%d budget=%dms maxPlies=%d%n",
                cfg.games, cfg.depth, cfg.budgetMillis, cfg.maxPlies);

        for (int g = 0; g < cfg.games; g++) {
            Board board = new Board(masks);
            board.loadFen(cfg.fen);

            // Distinct seeds per color per game so repeated games diverge; when a
            // base seed is supplied the whole run is reproducible.
            Bot white = cfg.hasSeed ? new Bot(cfg.seed + 2L * g)     : new Bot();
            Bot black = cfg.hasSeed ? new Bot(cfg.seed + 2L * g + 1) : new Bot();

            GameResult result = runner.play(board, white, black);

            if (!result.finishedNaturally()) {
                unfinished++;
            } else if (result.winnerIsWhite() == null) {
                draws++;
            } else if (result.winnerIsWhite()) {
                whiteWins++;
            } else {
                blackWins++;
            }

            System.out.printf(
                    "Game %d/%d: %s  %-22s plies=%d  nodes(W/B)=%d/%d  seeds(W/B)=%d/%d%n",
                    g + 1, cfg.games,
                    result.scoreString(),
                    result.reason() + (result.finishedNaturally() ? "" : " (stopped)"),
                    result.plies(),
                    result.whiteNodes(), result.blackNodes(),
                    result.whiteSeed(), result.blackSeed());
        }

        System.out.println("=========================================");
        System.out.printf("Result over %d game(s): white %d, black %d, draw %d%s%n",
                cfg.games, whiteWins, blackWins, draws,
                unfinished > 0 ? (", unfinished " + unfinished) : "");
    }

    /** Parsed command-line configuration. */
    private static final class Config {
        int games = 1;
        int depth = 5;
        long budgetMillis = 1000;
        int maxPlies = HeadlessGameRunner.DEFAULT_MAX_PLIES;
        boolean hasSeed = false;
        long seed = 0;
        String fen = START_FEN;
        boolean logMoves = false;

        static Config parse(String[] args) {
            Config c = new Config();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--games"     -> c.games = Integer.parseInt(args[++i]);
                    case "--depth"     -> c.depth = Integer.parseInt(args[++i]);
                    case "--budget-ms" -> c.budgetMillis = Long.parseLong(args[++i]);
                    case "--max-plies" -> c.maxPlies = Integer.parseInt(args[++i]);
                    case "--seed"      -> { c.seed = Long.parseLong(args[++i]); c.hasSeed = true; }
                    case "--fen"       -> c.fen = args[++i];
                    case "--log-moves" -> c.logMoves = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            return c;
        }
    }
}
