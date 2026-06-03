package com.cellsimulation.cli;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import com.cellsimulation.io.SaveService;
import com.cellsimulation.model.DiseaseState;
import com.cellsimulation.model.Grid;
import com.cellsimulation.model.Person;
import com.cellsimulation.model.Position;
import com.cellsimulation.model.SimulationEngine;
import com.cellsimulation.model.SimulationSettings;
import com.cellsimulation.neighborhood.EuclideanNeighborhood;
import com.cellsimulation.neighborhood.ManhattanNeighborhood;
import com.cellsimulation.neighborhood.MooreNeighborhood;
import com.cellsimulation.neighborhood.NeighborhoodStrategy;
import com.cellsimulation.neighborhood.OrthogonalNeighborhood;
import com.cellsimulation.stats.StatisticsService;

/**
 * Interactive command-line driver for the simulation engine.
 *
 * <p>Exposes a small REPL that accepts text commands ({@code help},
 * {@code print}, {@code stats}, {@code init}, {@code add}, {@code remove},
 * {@code random}, {@code step}, {@code play}, {@code set},
 * {@code neighborhood}, {@code save}, {@code load}, {@code reset},
 * {@code quit}). The runner owns a single {@link SimulationEngine}
 * instance, which it rewires when the user runs {@code init} (to change
 * the grid dimensions) or {@code load} (to replace the current engine
 * with one read from disk).
 *
 * <p>The class is fully independent from JavaFX, so the simulation can be
 * demonstrated on any machine with only a JDK installed. The {@code save}
 * and {@code load} commands are delegated to a {@link SaveService} and
 * persist the engine state in a binary file on disk using Java native
 * serialization.
 */
public final class CliRunner {

    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 20;
    private static final boolean DEFAULT_TORIC = false;

    private SimulationEngine engine;
    private StatisticsService statisticsService;
    private final SaveService saveService = new SaveService();

    /**
     * Entry point of the CLI front-end.
     *
     * <p>Builds a new {@code CliRunner} with the default configuration,
     * prints the welcome banner and starts the REPL loop. Command-line
     * arguments are ignored.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        CliRunner runner = new CliRunner();
        Grid grid = runner.engine.getGrid();
        println("Cell Simulation 2D - CLI mode");
        println("Grid: " + grid.getWidth() + "x" + grid.getHeight()
                + " (" + (grid.isToricMode() ? "toric" : "bounded") + ") | "
                + "Neighborhood: " + runner.engine.getNeighborhood().getName()
                + " | Tick: " + runner.engine.getTickCount());
        println("Type 'help' for the list of commands.");
        runner.run();
    }

    /**
     * Builds a new CLI runner wired to a default engine: a 20x20 bounded
     * grid, the default {@link SimulationSettings} and the orthogonal
     * neighborhood strategy. The welcome banner is printed by
     * {@link #main(String[])}, not by this constructor.
     */
    public CliRunner() {
        Grid grid = new Grid(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_TORIC);
        SimulationSettings settings = new SimulationSettings();
        NeighborhoodStrategy neighborhood = new OrthogonalNeighborhood();
        this.engine = new SimulationEngine(grid, settings, neighborhood);
        this.statisticsService = new StatisticsService();
        this.engine.addListener(this.statisticsService);
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        boolean keepRunning = true;
        while (keepRunning) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            String command = tokens[0].toLowerCase();
            try {
                switch (command) {
                    case "help" -> handleHelp();
                    case "print" -> handlePrint();
                    case "stats" -> handleStats();
                    case "init" -> handleInit(tokens);
                    case "add" -> handleAdd(tokens);
                    case "remove" -> handleRemove(tokens);
                    case "random" -> handleRandom(tokens);
                    case "step" -> handleStep();
                    case "play" -> handlePlay(tokens);
                    case "set" -> handleSet(tokens);
                    case "neighborhood" -> handleNeighborhood(tokens);
                    case "save" -> handleSave(tokens);
                    case "load" -> handleLoad(tokens);
                    case "reset" -> handleReset();
                    case "quit", "exit" -> keepRunning = false;
                    default -> error("Unknown command: " + command + ". Type 'help'.");
                }
            } catch (IllegalArgumentException | IOException e) {
                error(e.getMessage());
            } catch (ClassNotFoundException e) {
                error("cannot load: " + e.getMessage());
            }
        }
        println("Goodbye.");
    }

    private void handleHelp() {
        println("Available commands:");
        println("  help                                 Display this help message");
        println("  print                                Display the grid in ASCII");
        println("  stats                                Display population counters and tick");
        println("  init <width> <height> [toric]        Recreate the grid (resets the engine)");
        println("  add <row> <col> <state>              Place a person; state = S, I, R, D");
        println("  remove <row> <col>                   Empty the cell at (row, col)");
        println("  random <count> [infected]            Place <count> persons at random,");
        println("                                       of which [infected] are INFECTED");
        println("                                       (default: 1)");
        println("  step                                 Play one simulation tick");
        println("  play <n>                             Play <n> ticks, printing the grid");
        println("                                       between each");
        println("  set <param> <value>                  Modify a setting; param in");
        println("                                       transmission, recovery, mortality,");
        println("                                       mobility, max-days, speed");
        println("  neighborhood <mode> [radius]         Switch neighborhood; mode in");
        println("                                       orthogonal, moore, euclidean,");
        println("                                       manhattan; radius required for");
        println("                                       euclidean and manhattan");
        println("  save <filepath>                      Save the simulation state to a binary file");
        println("  load <filepath>                      Load a previously saved simulation");
        println("  reset                                Empty the grid and reset the tick");
        println("  quit                                 Exit the program");
    }

    private void handlePrint() {
        Grid grid = engine.getGrid();
        for (int row = 0; row < grid.getHeight(); row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < grid.getWidth(); col++) {
                if (col > 0) {
                    line.append(' ');
                }
                Person occupant = grid.getCell(new Position(row, col));
                line.append(occupant == null ? '.' : symbolOf(occupant));
            }
            println(line.toString());
        }
    }

    private void handleStats() {
        Grid grid = engine.getGrid();
        int susceptible = 0;
        int infected = 0;
        int recovered = 0;
        int deceased = 0;
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Person occupant = grid.getCell(new Position(row, col));
                if (occupant == null) {
                    continue;
                }
                switch (occupant.getState()) {
                    case SUSCEPTIBLE -> susceptible++;
                    case INFECTED -> infected++;
                    case RECOVERED -> recovered++;
                    case DECEASED -> deceased++;
                }
            }
        }
        int total = susceptible + infected + recovered + deceased;
        int cells = grid.getWidth() * grid.getHeight();
        println("Tick: " + engine.getTickCount());
        println(String.format("%-12s %d", "Susceptible:", susceptible));
        println(String.format("%-12s %d", "Infected:", infected));
        println(String.format("%-12s %d", "Recovered:", recovered));
        println(String.format("%-12s %d", "Deceased:", deceased));
        println(String.format("%-12s %d (over %d cells)", "Total:", total, cells));
    }

    private void handleInit(String[] tokens) {
        if (tokens.length < 3) {
            throw new IllegalArgumentException("usage: init <width> <height> [toric]");
        }
        int width = Integer.parseInt(tokens[1]);
        int height = Integer.parseInt(tokens[2]);
        boolean toric = tokens.length >= 4 && tokens[3].equalsIgnoreCase("toric");
        Grid newGrid = new Grid(width, height, toric);
        SimulationSettings currentSettings = engine.getSettings();
        NeighborhoodStrategy currentNeighborhood = engine.getNeighborhood();
        engine = new SimulationEngine(newGrid, currentSettings, currentNeighborhood);
        statisticsService = new StatisticsService();
        engine.addListener(statisticsService);
        println("Grid reinitialized to " + width + "x" + height
                + " (" + (toric ? "toric" : "bounded") + ").");
    }

    private void handleAdd(String[] tokens) {
        if (tokens.length < 4) {
            throw new IllegalArgumentException("usage: add <row> <col> <state>");
        }
        int row = Integer.parseInt(tokens[1]);
        int col = Integer.parseInt(tokens[2]);
        DiseaseState state = parseState(tokens[3]);
        engine.addCell(new Position(row, col), state);
    }

    private void handleRemove(String[] tokens) {
        if (tokens.length < 3) {
            throw new IllegalArgumentException("usage: remove <row> <col>");
        }
        int row = Integer.parseInt(tokens[1]);
        int col = Integer.parseInt(tokens[2]);
        engine.removeCell(new Position(row, col));
    }

    private void handleRandom(String[] tokens) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("usage: random <count> [infected]");
        }
        int count = Integer.parseInt(tokens[1]);
        int infected = tokens.length >= 3 ? Integer.parseInt(tokens[2]) : 1;
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, got " + count);
        }
        if (infected < 0) {
            throw new IllegalArgumentException("infected must be >= 0, got " + infected);
        }
        if (infected > count) {
            throw new IllegalArgumentException(
                    "infected (" + infected + ") must be <= count (" + count + ")");
        }
        Grid grid = engine.getGrid();
        int capacity = grid.getWidth() * grid.getHeight();
        if (count > capacity) {
            throw new IllegalArgumentException(
                    "count (" + count + ") must be <= grid capacity (" + capacity + ")");
        }
        Set<Position> positions = new HashSet<>();
        Random random = new Random();
        while (positions.size() < count) {
            int row = random.nextInt(grid.getHeight());
            int col = random.nextInt(grid.getWidth());
            positions.add(new Position(row, col));
        }
        int placed = 0;
        for (Position position : positions) {
            DiseaseState state = placed < infected
                    ? DiseaseState.INFECTED
                    : DiseaseState.SUSCEPTIBLE;
            engine.addCell(position, state);
            placed++;
        }
        println("Placed " + count + " persons (" + infected + " infected).");
    }

    private void handleStep() {
        engine.step();
        handlePrint();
        handleStats();
    }

    private void handlePlay(String[] tokens) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("usage: play <n>");
        }
        int n = Integer.parseInt(tokens[1]);
        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1, got " + n);
        }
        for (int turn = 0; turn < n; turn++) {
            engine.step();
            Grid grid = engine.getGrid();
            int susceptible = 0;
            int infected = 0;
            int recovered = 0;
            int deceased = 0;
            for (int row = 0; row < grid.getHeight(); row++) {
                for (int col = 0; col < grid.getWidth(); col++) {
                    Person occupant = grid.getCell(new Position(row, col));
                    if (occupant == null) {
                        continue;
                    }
                    switch (occupant.getState()) {
                        case SUSCEPTIBLE -> susceptible++;
                        case INFECTED -> infected++;
                        case RECOVERED -> recovered++;
                        case DECEASED -> deceased++;
                    }
                }
            }
            println("Tick " + engine.getTickCount() + " - "
                    + "S:" + susceptible + " I:" + infected
                    + " R:" + recovered + " D:" + deceased);
            handlePrint();
        }
    }

    private void handleSet(String[] tokens) {
        if (tokens.length < 3) {
            throw new IllegalArgumentException("usage: set <param> <value>");
        }
        String param = tokens[1].toLowerCase();
        SimulationSettings settings = engine.getSettings();
        switch (param) {
            case "transmission" -> settings.setTransmissionProbability(Double.parseDouble(tokens[2]));
            case "recovery" -> settings.setRecoveryProbability(Double.parseDouble(tokens[2]));
            case "mortality" -> settings.setMortalityProbability(Double.parseDouble(tokens[2]));
            case "mobility" -> settings.setMobilityRate(Double.parseDouble(tokens[2]));
            case "max-days" -> settings.setMaxInfectionDays(Integer.parseInt(tokens[2]));
            case "speed" -> settings.setSimulationSpeed(Integer.parseInt(tokens[2]));
            default -> throw new IllegalArgumentException("unknown param: " + param);
        }
        println(param + " = " + tokens[2]);
    }

    private void handleNeighborhood(String[] tokens) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("usage: neighborhood <mode> [radius]");
        }
        String mode = tokens[1].toLowerCase();
        NeighborhoodStrategy strategy = switch (mode) {
            case "orthogonal" -> new OrthogonalNeighborhood();
            case "moore" -> new MooreNeighborhood();
            case "euclidean" -> {
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("usage: neighborhood euclidean <radius>");
                }
                yield new EuclideanNeighborhood(Integer.parseInt(tokens[2]));
            }
            case "manhattan" -> {
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("usage: neighborhood manhattan <radius>");
                }
                yield new ManhattanNeighborhood(Integer.parseInt(tokens[2]));
            }
            default -> throw new IllegalArgumentException("unknown mode: " + mode);
        };
        engine.setNeighborhood(strategy);
        println("Neighborhood = " + strategy.getName());
    }

    /**
     * Handles the {@code save <filepath>} command.
     *
     * <p>Delegates to {@link SaveService#save(SimulationEngine, String)}.
     *
     * @param tokens the command tokens; {@code tokens[1]} must be the
     *               target file path
     * @throws IOException              if writing the file fails
     * @throws IllegalArgumentException if no file path is provided
     */
    private void handleSave(String[] tokens) throws IOException {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("usage: save <filepath>");
        }
        String filePath = tokens[1];
        saveService.save(engine, filePath);
        println("Saved to " + filePath + " at tick " + engine.getTickCount() + ".");
    }

    /**
     * Handles the {@code load <filepath>} command.
     *
     * <p>Replaces the current engine with the one read from the file and
     * attaches a fresh {@link StatisticsService} (the previous history is
     * discarded by design).
     *
     * @param tokens the command tokens; {@code tokens[1]} must be the
     *               source file path
     * @throws IOException            if reading the file fails or the file
     *                                does not contain a
     *                                {@link SimulationEngine}
     * @throws ClassNotFoundException if a class needed to deserialize the
     *                                engine cannot be found on the class
     *                                path
     * @throws IllegalArgumentException if no file path is provided
     */
    private void handleLoad(String[] tokens) throws IOException, ClassNotFoundException {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("usage: load <filepath>");
        }
        String filePath = tokens[1];
        engine = saveService.load(filePath);
        statisticsService = new StatisticsService();
        engine.addListener(statisticsService);
        Grid grid = engine.getGrid();
        println("Loaded from " + filePath
                + ". Grid: " + grid.getWidth() + "x" + grid.getHeight()
                + " (" + (grid.isToricMode() ? "toric" : "bounded") + ")"
                + " | Neighborhood: " + engine.getNeighborhood().getName()
                + " | Tick: " + engine.getTickCount());
    }

    private void handleReset() {
        engine.reset();
        println("Engine reset.");
    }

    private DiseaseState parseState(String token) {
        return switch (token.toLowerCase()) {
            case "s", "susceptible" -> DiseaseState.SUSCEPTIBLE;
            case "i", "infected" -> DiseaseState.INFECTED;
            case "r", "recovered" -> DiseaseState.RECOVERED;
            case "d", "deceased" -> DiseaseState.DECEASED;
            default -> throw new IllegalArgumentException("unknown state: " + token);
        };
    }

    private char symbolOf(Person person) {
        return switch (person.getState()) {
            case SUSCEPTIBLE -> 'S';
            case INFECTED -> 'I';
            case RECOVERED -> 'R';
            case DECEASED -> 'D';
        };
    }

    private static void println(String message) {
        System.out.println(message);
    }

    private static void error(String message) {
        System.out.println("[error] " + message);
    }
}
