package com.cellsimulation.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
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
import com.cellsimulation.stats.SimulationListener;
import com.cellsimulation.stats.Statistics;
import com.cellsimulation.stats.StatisticsService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;

/**
 * JavaFX controller of the main application window.
 *
 * <p>Acts as the glue between the FXML view and the simulation model: it
 * owns a {@link SimulationEngine}, attaches a {@link StatisticsService} for
 * the live chart, and uses a {@link SaveService} to persist or restore a
 * simulation through the {@code File} menu.
 *
 * <p>The controller also implements {@link SimulationListener} so that the
 * canvas, the population counters and the chart are refreshed automatically
 * after every tick. A JavaFX {@link Timeline} drives the simulation cadence
 * (no custom thread is started); each key-frame simply asks the engine to
 * play one tick, which in turn fires the listener callbacks that update the
 * UI.
 *
 * <p>The naming of {@link #onResetButton()} (rather than {@code onReset})
 * is deliberate: the listener callback {@link #onReset()} carries the same
 * signature as the toolbar handler would, so the toolbar handler was renamed
 * to avoid a compile-time clash. The two methods serve different purposes:
 * the toolbar handler pauses the engine and asks for a reset, the listener
 * callback merely refreshes the UI after the engine has reset itself.
 */
public class MainController implements SimulationListener {

    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 20;
    private static final int CELL_SIZE = 20;

    private static final String NEIGHBORHOOD_ORTHOGONAL = "Orthogonal (4)";
    private static final String NEIGHBORHOOD_MOORE = "Moore (8)";
    private static final String NEIGHBORHOOD_EUCLIDEAN = "Euclidean (radius)";
    private static final String NEIGHBORHOOD_MANHATTAN = "Manhattan (distance)";

    private static final String BRUSH_SUSCEPTIBLE = "S";
    private static final String BRUSH_INFECTED = "I";
    private static final String BRUSH_RECOVERED = "R";
    private static final String BRUSH_DECEASED = "D";
    private static final String BRUSH_ERASE = "Erase";
    private static final String BRUSH_VACCINATED = "V";

    @FXML private MenuItem newMenuItem;
    @FXML private MenuItem openMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem aboutMenuItem;

    @FXML private Button playPauseButton;
    @FXML private Button stepButton;
    @FXML private Button resetButton;
    @FXML private Slider speedSlider;
    @FXML private Label speedValueLabel;
    @FXML private Label tickLabel;
    @FXML private Label populationLabel;

    @FXML private TextField widthField;
    @FXML private TextField heightField;
    @FXML private CheckBox toricCheckBox;
    @FXML private Button applyButton;

    @FXML private ComboBox<String> neighborhoodCombo;
    @FXML private TextField radiusField;

    @FXML private Slider transmissionSlider;
    @FXML private Slider recoverySlider;
    @FXML private Slider mortalitySlider;
    @FXML private Slider mobilitySlider;
    @FXML private Slider maxDaysSlider;
    @FXML private Label transmissionValueLabel;
    @FXML private Label recoveryValueLabel;
    @FXML private Label mortalityValueLabel;
    @FXML private Label mobilityValueLabel;
    @FXML private Label maxDaysValueLabel;
    @FXML private Slider meanImmunitySlider;
    @FXML private Label meanImmunityValueLabel;
    @FXML private Slider immunityVarianceSlider;
    @FXML private Label immunityVarianceValueLabel;
    @FXML private Slider vaccineEfficacySlider;
    @FXML private Label vaccineEfficacyValueLabel;

    @FXML private ToggleGroup brushGroup;
    @FXML private RadioButton susceptibleBrush;
    @FXML private RadioButton infectedBrush;
    @FXML private RadioButton recoveredBrush;
    @FXML private RadioButton deceasedBrush;
    @FXML private RadioButton vaccinatedBrush;
    @FXML private RadioButton eraseBrush;

    @FXML private TextField vaccinationPercentField;
    @FXML private Button vaccinateRandomButton;

    @FXML private TextField randomCountField;
    @FXML private TextField randomInfectedField;
    @FXML private Button randomFillButton;

    @FXML private StackPane canvasContainer;
    @FXML private Canvas gridCanvas;

    @FXML private Label susceptibleCount;
    @FXML private Label infectedCount;
    @FXML private Label recoveredCount;
    @FXML private Label deceasedCount;
    @FXML private Label totalCount;
    @FXML private Label susceptiblePercent;
    @FXML private Label infectedPercent;
    @FXML private Label recoveredPercent;
    @FXML private Label deceasedPercent;

    @FXML private LineChart<Number, Number> evolutionChart;
    @FXML private Label avgInfectionDaysLabel;
    @FXML private Label immunityRangeLabel;

    @FXML private Label statusLabel;
    @FXML private Label currentNeighborhoodLabel;
    @FXML private Label currentModeLabel;
    @FXML private Label gridSizeLabel;

    private SimulationEngine engine;
    private StatisticsService statisticsService;
    private final SaveService saveService = new SaveService();
    private Timeline timeline;
    private Position lastPaintedPosition;

    private XYChart.Series<Number, Number> susceptibleSeries;
    private XYChart.Series<Number, Number> infectedSeries;
    private XYChart.Series<Number, Number> recoveredSeries;
    private XYChart.Series<Number, Number> deceasedSeries;

    /**
     * Called by the JavaFX runtime after the FXML view has been loaded and
     * every {@code fx:id}-annotated control has been injected.
     *
     * <p>Builds the default 20x20 bounded engine, attaches a fresh
     * {@link StatisticsService} and the controller itself as listeners,
     * primes the chart series, populates every control with its default
     * value, creates the playback {@link Timeline} and wires the dynamic
     * listeners (sliders, combobox, canvas click, canvas resize).
     *
     * <p>The order is deliberate: defaults are written to the controls
     * <em>before</em> the slider listeners are attached, so the initial
     * {@code setValue} calls do not echo back into {@link SimulationSettings}
     * mutators.
     */
    @FXML
    public void initialize() {
        Grid grid = new Grid(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
        SimulationSettings settings = new SimulationSettings();
        NeighborhoodStrategy neighborhood = new OrthogonalNeighborhood();
        engine = new SimulationEngine(grid, settings, neighborhood);
        statisticsService = new StatisticsService();
        engine.addListener(statisticsService);
        engine.addListener(this);

        initializeChartSeries();
        populateDefaultControls(settings);
        initializeTimeline(settings.getSimulationSpeed());
        wireDynamicListeners();

        resizeCanvasToGrid();
        drawGrid();
        updateStats();
        updateStatusBar();
    }

    private void initializeChartSeries() {
        susceptibleSeries = new XYChart.Series<>();
        susceptibleSeries.setName("Susceptible");
        infectedSeries = new XYChart.Series<>();
        infectedSeries.setName("Infected");
        recoveredSeries = new XYChart.Series<>();
        recoveredSeries.setName("Recovered");
        deceasedSeries = new XYChart.Series<>();
        deceasedSeries.setName("Deceased");
        evolutionChart.getData().add(susceptibleSeries);
        evolutionChart.getData().add(infectedSeries);
        evolutionChart.getData().add(recoveredSeries);
        evolutionChart.getData().add(deceasedSeries);
    }

    private void populateDefaultControls(SimulationSettings settings) {
        widthField.setText(String.valueOf(DEFAULT_WIDTH));
        heightField.setText(String.valueOf(DEFAULT_HEIGHT));
        toricCheckBox.setSelected(false);

        neighborhoodCombo.getItems().setAll(
                NEIGHBORHOOD_ORTHOGONAL,
                NEIGHBORHOOD_MOORE,
                NEIGHBORHOOD_EUCLIDEAN,
                NEIGHBORHOOD_MANHATTAN);
        neighborhoodCombo.setValue(NEIGHBORHOOD_ORTHOGONAL);
        radiusField.setText("2");
        radiusField.setVisible(false);
        radiusField.managedProperty().bind(radiusField.visibleProperty());

        transmissionSlider.setValue(settings.getTransmissionProbability() * 100.0);
        transmissionValueLabel.setText(formatProbability(settings.getTransmissionProbability()));
        recoverySlider.setValue(settings.getRecoveryProbability() * 100.0);
        recoveryValueLabel.setText(formatProbability(settings.getRecoveryProbability()));
        mortalitySlider.setValue(settings.getMortalityProbability() * 100.0);
        mortalityValueLabel.setText(formatProbability(settings.getMortalityProbability()));
        mobilitySlider.setValue(settings.getMobilityRate() * 100.0);
        mobilityValueLabel.setText(formatProbability(settings.getMobilityRate()));
        maxDaysSlider.setValue(settings.getMaxInfectionDays());
        maxDaysValueLabel.setText(String.valueOf(settings.getMaxInfectionDays()));
        meanImmunitySlider.setValue(settings.getMeanImmunity() * 100.0);
        meanImmunityValueLabel.setText(formatProbability(settings.getMeanImmunity()));
        immunityVarianceSlider.setValue(settings.getImmunityVariance() * 100.0);
        immunityVarianceValueLabel.setText(formatProbability(settings.getImmunityVariance()));
        vaccineEfficacySlider.setValue(settings.getVaccineEfficacy() * 100.0);
        vaccineEfficacyValueLabel.setText(formatProbability(settings.getVaccineEfficacy()));
        speedSlider.setValue(settings.getSimulationSpeed());
        speedValueLabel.setText(settings.getSimulationSpeed() + " ticks/s");

        infectedBrush.setSelected(true);
        vaccinationPercentField.setText("30");
        randomCountField.setText("50");
        randomInfectedField.setText("3");
    }

    private void initializeTimeline(int initialSpeed) {
        int speed = Math.max(1, initialSpeed);
        timeline = new Timeline(new KeyFrame(
                Duration.millis(1000.0 / speed),
                e -> engine.step()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void wireDynamicListeners() {
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int s = Math.max(1, newVal.intValue());
            engine.getSettings().setSimulationSpeed(s);
            speedValueLabel.setText(s + " ticks/s");
            timeline.stop();
            timeline.getKeyFrames().setAll(new KeyFrame(
                    Duration.millis(1000.0 / s),
                    e -> engine.step()));
            if (engine.isRunning()) {
                timeline.play();
            }
        });

        transmissionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double prob = newVal.doubleValue() / 100.0;
            engine.getSettings().setTransmissionProbability(prob);
            transmissionValueLabel.setText(formatProbability(prob));
        });
        recoverySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double prob = newVal.doubleValue() / 100.0;
            engine.getSettings().setRecoveryProbability(prob);
            recoveryValueLabel.setText(formatProbability(prob));
        });
        mortalitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double prob = newVal.doubleValue() / 100.0;
            engine.getSettings().setMortalityProbability(prob);
            mortalityValueLabel.setText(formatProbability(prob));
        });
        mobilitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double prob = newVal.doubleValue() / 100.0;
            engine.getSettings().setMobilityRate(prob);
            mobilityValueLabel.setText(formatProbability(prob));
        });
        maxDaysSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int days = Math.max(1, newVal.intValue());
            engine.getSettings().setMaxInfectionDays(days);
            maxDaysValueLabel.setText(String.valueOf(days));
        });
        meanImmunitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double prob = newVal.doubleValue() / 100.0;
            engine.getSettings().setMeanImmunity(prob);
            meanImmunityValueLabel.setText(formatProbability(prob));
        });
        immunityVarianceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double variance = newVal.doubleValue() / 100.0;
            engine.getSettings().setImmunityVariance(variance);
            immunityVarianceValueLabel.setText(formatProbability(variance));
        });
        vaccineEfficacySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double efficacy = newVal.doubleValue() / 100.0;
            engine.getSettings().setVaccineEfficacy(efficacy);
            vaccineEfficacyValueLabel.setText(formatProbability(efficacy));
        });

        neighborhoodCombo.setOnAction(e -> handleNeighborhoodSelection());
        gridCanvas.setOnMousePressed(this::handleCanvasPaint);
        gridCanvas.setOnMouseDragged(this::handleCanvasPaint);
        gridCanvas.setOnMouseReleased(e -> lastPaintedPosition = null);
    }

    /**
     * Toolbar handler that toggles the engine between its running and paused
     * states.
     *
     * <p>Starts (or pauses) the playback {@link Timeline} accordingly and
     * updates the button label.
     */
    @FXML
    private void onPlayPause() {
        if (engine.isRunning()) {
            engine.pause();
            timeline.pause();
            playPauseButton.setText("Play");
        } else {
            engine.start();
            timeline.play();
            playPauseButton.setText("Pause");
        }
    }

    /**
     * Toolbar handler that advances the engine by exactly one tick.
     *
     * <p>The UI refresh is performed indirectly: the engine notifies its
     * listeners (including this controller) inside {@code step()}, which
     * triggers {@link #onTick(Grid, int)}.
     */
    @FXML
    private void onStep() {
        engine.step();
    }

    /**
     * Toolbar handler that resets the simulation.
     *
     * <p>Renamed from {@code onReset} to avoid a clash with
     * {@link #onReset()}, the listener callback inherited from
     * {@link SimulationListener}. Pauses the engine and the {@link Timeline},
     * then calls {@link SimulationEngine#reset()}; the listener callback is
     * fired by the engine and takes care of refreshing the UI.
     */
    @FXML
    private void onResetButton() {
        engine.pause();
        timeline.pause();
        playPauseButton.setText("Play");
        engine.reset();
    }

    /**
     * Handler for the "Apply" button of the grid configuration section.
     *
     * <p>Reads the width, height and toric flag from the form, builds a new
     * {@link Grid} and rebuilds the {@link SimulationEngine} around it while
     * preserving the current {@link SimulationSettings} and
     * {@link NeighborhoodStrategy}. A fresh {@link StatisticsService} is
     * attached so that the chart starts from a clean slate.
     */
    @FXML
    private void onApplyGridConfig() {
        try {
            int width = Integer.parseInt(widthField.getText().trim());
            int height = Integer.parseInt(heightField.getText().trim());
            boolean toric = toricCheckBox.isSelected();
            Grid newGrid = new Grid(width, height, toric);
            SimulationSettings currentSettings = engine.getSettings();
            NeighborhoodStrategy currentNeighborhood = engine.getNeighborhood();
            timeline.pause();
            playPauseButton.setText("Play");
            engine = new SimulationEngine(newGrid, currentSettings, currentNeighborhood);
            statisticsService = new StatisticsService();
            engine.addListener(statisticsService);
            engine.addListener(this);
            clearChart();
            resizeCanvasToGrid();
            drawGrid();
            updateStats();
            updateStatusBar();
            statusLabel.setText("Grid: " + width + "x" + height
                    + " (" + (toric ? "toric" : "bounded") + ")");
        } catch (NumberFormatException ex) {
            showError("Invalid grid dimensions: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Handler for the "Random fill" button.
     *
     * <p>Reads the requested counts from the two text fields, validates them
     * the same way the {@code random} CLI command does, then populates the
     * grid with distinct positions chosen at random. The first {@code
     * infected} positions get an {@code INFECTED} occupant, the remaining
     * ones get a {@code SUSCEPTIBLE} occupant.
     */
    @FXML
    private void onRandomFill() {
        try {
            int count = Integer.parseInt(randomCountField.getText().trim());
            int infected = Integer.parseInt(randomInfectedField.getText().trim());
            if (count < 1) {
                showError("count must be >= 1, got " + count);
                return;
            }
            if (infected < 0) {
                showError("infected must be >= 0, got " + infected);
                return;
            }
            if (infected > count) {
                showError("infected (" + infected + ") must be <= count (" + count + ")");
                return;
            }
            Grid grid = engine.getGrid();
            int capacity = grid.getWidth() * grid.getHeight();
            if (count > capacity) {
                showError("count (" + count + ") must be <= grid capacity ("
                        + capacity + ")");
                return;
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
                engine.removeCell(position);
                engine.addCell(position, state);
                placed++;
            }
            drawGrid();
            updateStats();
            statusLabel.setText("Placed " + count + " persons ("
                    + infected + " infected)");
        } catch (NumberFormatException ex) {
            showError("Invalid random fill numbers: " + ex.getMessage());
        }
    }

    /**
     * Handler for the "Vaccinate random N%" button.
     *
     * <p>Reads the requested percentage from {@code vaccinationPercentField},
     * counts the living persons on the grid (anything other than DECEASED),
     * picks {@code percent / 100 * living} of them uniformly at random and
     * marks them as vaccinated. Already-vaccinated persons can be picked
     * again (no-op).
     */
    @FXML
    private void onVaccinateRandom() {
        try {
            int percent = Integer.parseInt(vaccinationPercentField.getText().trim());
            if (percent < 0 || percent > 100) {
                showError("percent must be in [0, 100], got " + percent);
                return;
            }
            Grid grid = engine.getGrid();
            List<Person> living = new ArrayList<>();
            for (int row = 0; row < grid.getHeight(); row++) {
                for (int col = 0; col < grid.getWidth(); col++) {
                    Person p = grid.getCell(new Position(row, col));
                    if (p != null && !p.isDead()) {
                        living.add(p);
                    }
                }
            }
            if (living.isEmpty()) {
                showError("No living person to vaccinate");
                return;
            }
            int toVaccinate = (int) Math.round(living.size() * percent / 100.0);
            Collections.shuffle(living);
            for (int i = 0; i < toVaccinate; i++) {
                living.get(i).setVaccinated(true);
            }
            drawGrid();
            statusLabel.setText("Vaccinated " + toVaccinate + " persons ("
                    + percent + "% of " + living.size() + " living)");
        } catch (NumberFormatException ex) {
            showError("Invalid vaccination percent: " + ex.getMessage());
        }
    }

    /**
     * Handler for the {@code File &gt; New} menu item.
     *
     * <p>Equivalent to clicking the toolbar "Reset" button: pauses the
     * engine and empties the grid, but keeps the current settings,
     * neighborhood strategy and grid dimensions.
     */
    @FXML
    private void onNew() {
        onResetButton();
    }

    /**
     * Handler for the {@code File &gt; Open} menu item.
     *
     * <p>Opens a {@link FileChooser} restricted to {@code *.sim} files,
     * deserializes the chosen file through {@link SaveService#load(String)}
     * and replaces the current engine with the loaded one. A fresh
     * {@link StatisticsService} is attached because the previous chart
     * history is dropped on purpose.
     */
    @FXML
    private void onOpen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load simulation");
        chooser.getExtensionFilters().add(
                new ExtensionFilter("Simulation files", "*.sim"));
        File file = chooser.showOpenDialog(playPauseButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            timeline.pause();
            playPauseButton.setText("Play");
            engine = saveService.load(file.getAbsolutePath());
            statisticsService = new StatisticsService();
            engine.addListener(statisticsService);
            engine.addListener(this);
            syncControlsFromEngine();
            clearChart();
            resizeCanvasToGrid();
            drawGrid();
            updateStats();
            updateStatusBar();
            statusLabel.setText("Loaded " + file.getName());
        } catch (IOException | ClassNotFoundException ex) {
            showError("Cannot load: " + ex.getMessage());
        }
    }

    /**
     * Handler for the {@code File &gt; Save} menu item.
     *
     * <p>Opens a {@link FileChooser} restricted to {@code *.sim} files and
     * serializes the current engine through
     * {@link SaveService#save(SimulationEngine, String)}.
     */
    @FXML
    private void onSave() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save simulation");
        chooser.getExtensionFilters().add(
                new ExtensionFilter("Simulation files", "*.sim"));
        File file = chooser.showSaveDialog(playPauseButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            saveService.save(engine, file.getAbsolutePath());
            statusLabel.setText("Saved to " + file.getName());
        } catch (IOException ex) {
            showError("Cannot save: " + ex.getMessage());
        }
    }

    /**
     * Handler for the {@code File &gt; Exit} menu item.
     *
     * <p>Closes the JavaFX runtime, which terminates the application.
     */
    @FXML
    private void onExit() {
        Platform.exit();
    }

    /**
     * Handler for the {@code Help &gt; About} menu item.
     *
     * <p>Displays a modal information dialog describing the project.
     */
    @FXML
    private void onAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About Cell Simulation 2D");
        alert.setHeaderText("Cell Simulation 2D");
        alert.setContentText("CYTech ING1-GI - 2025-2026\n\n"
                + "Disease propagation simulation\n"
                + "using the SIR + Deceased model.");
        alert.showAndWait();
    }

    private void handleNeighborhoodSelection() {
        String selected = neighborhoodCombo.getValue();
        if (selected == null) {
            return;
        }
        NeighborhoodStrategy strategy = switch (selected) {
            case NEIGHBORHOOD_ORTHOGONAL -> {
                radiusField.setVisible(false);
                yield new OrthogonalNeighborhood();
            }
            case NEIGHBORHOOD_MOORE -> {
                radiusField.setVisible(false);
                yield new MooreNeighborhood();
            }
            case NEIGHBORHOOD_EUCLIDEAN -> {
                radiusField.setVisible(true);
                yield new EuclideanNeighborhood(parseRadius());
            }
            case NEIGHBORHOOD_MANHATTAN -> {
                radiusField.setVisible(true);
                yield new ManhattanNeighborhood(parseRadius());
            }
            default -> throw new IllegalStateException(
                    "unknown neighborhood: " + selected);
        };
        engine.setNeighborhood(strategy);
        updateStatusBar();
    }

    /**
     * Mouse handler used for both {@code MOUSE_PRESSED} and
     * {@code MOUSE_DRAGGED} events on the canvas. Paints the cell under
     * the cursor with the currently selected brush. The
     * {@link #lastPaintedPosition} field is used to deduplicate
     * consecutive events that target the same cell, which avoids
     * redundant grid mutations and redraws during a long drag.
     *
     * @param event the mouse event that triggered the paint
     */
    private void handleCanvasPaint(MouseEvent event) {
        int cellSize = computeCellSize();
        if (cellSize <= 0) {
            return;
        }
        int col = (int) (event.getX() / cellSize);
        int row = (int) (event.getY() / cellSize);
        Position pos = new Position(row, col);
        if (!engine.getGrid().isInside(pos)) {
            return;
        }
        if (pos.equals(lastPaintedPosition)) {
            return;
        }
        lastPaintedPosition = pos;

        String brush = getSelectedBrush();
        if (BRUSH_VACCINATED.equals(brush)) {
            Person existing = engine.getGrid().getCell(pos);
            if (existing == null) {
                engine.addCell(pos, DiseaseState.SUSCEPTIBLE);
                Person fresh = engine.getGrid().getCell(pos);
                if (fresh != null) {
                    fresh.setVaccinated(true);
                }
            } else {
                existing.setVaccinated(true);
            }
        } else if (BRUSH_ERASE.equals(brush)) {
            engine.removeCell(pos);
        } else {
            engine.removeCell(pos);
            engine.addCell(pos, parseStateBrush(brush));
        }
        drawGrid();
        updateStats();
    }

    /**
     * Listener callback invoked by the engine after each tick.
     *
     * <p>Redraws the grid, refreshes the population counters and appends
     * the latest snapshot to the four chart series.
     *
     * @param grid      the current grid (unused here: this controller reads
     *                  the grid from the engine via {@link
     *                  SimulationEngine#getGrid()})
     * @param tickCount the tick number reported by the engine
     */
    @Override
    public void onTick(Grid grid, int tickCount) {
        drawGrid();
        updateStats();
        appendChartData(tickCount);
    }

    /**
     * Listener callback invoked by the engine when it is reset.
     *
     * <p>Wipes the chart, redraws the (now empty) grid and refreshes the
     * counters. The toolbar reset action is implemented in
     * {@link #onResetButton()}.
     */
    @Override
    public void onReset() {
        clearChart();
        drawGrid();
        updateStats();
    }

    /**
     * Resizes the canvas to {@code gridWidth * CELL_SIZE} by
     * {@code gridHeight * CELL_SIZE}. A fixed pixel size is used on purpose:
     * binding the canvas to its parent's size would create a layout feedback
     * loop, because the {@link Canvas} (not being a {@code Region}) feeds
     * its own width and height into the parent {@code StackPane}'s preferred
     * size, which would then grow by the container padding each layout pulse.
     */
    private void resizeCanvasToGrid() {
        Grid grid = engine.getGrid();
        gridCanvas.setWidth(grid.getWidth() * CELL_SIZE);
        gridCanvas.setHeight(grid.getHeight() * CELL_SIZE);
    }

    private void drawGrid() {
        Grid grid = engine.getGrid();
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        int cellSize = computeCellSize();
        double width = grid.getWidth() * cellSize;
        double height = grid.getHeight() * cellSize;
        gc.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                Person occupant = grid.getCell(new Position(row, col));
                if (occupant == null) {
                    continue;
                }
                gc.setFill(colorOf(occupant.getState()));
                gc.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
                if (occupant.isVaccinated()) {
                    gc.setStroke(Color.web("#F39C12"));
                    gc.setLineWidth(Math.max(2.0, cellSize / 8.0));
                    gc.strokeRect(
                            col * cellSize + 1,
                            row * cellSize + 1,
                            cellSize - 2,
                            cellSize - 2);
                }
            }
        }

        gc.setStroke(Color.gray(0.85));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= grid.getWidth(); i++) {
            gc.strokeLine(i * cellSize, 0, i * cellSize, height);
        }
        for (int i = 0; i <= grid.getHeight(); i++) {
            gc.strokeLine(0, i * cellSize, width, i * cellSize);
        }
    }

    private int computeCellSize() {
        return CELL_SIZE;
    }

    private Color colorOf(DiseaseState state) {
        return switch (state) {
            case SUSCEPTIBLE -> Color.web("#2E86DE");
            case INFECTED    -> Color.web("#EE5253");
            case RECOVERED   -> Color.web("#10AC84");
            case DECEASED    -> Color.web("#576574");
        };
    }

    private void updateStats() {
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
                    case INFECTED    -> infected++;
                    case RECOVERED   -> recovered++;
                    case DECEASED    -> deceased++;
                }
            }
        }
        int total = susceptible + infected + recovered + deceased;
        susceptibleCount.setText(String.valueOf(susceptible));
        infectedCount.setText(String.valueOf(infected));
        recoveredCount.setText(String.valueOf(recovered));
        deceasedCount.setText(String.valueOf(deceased));
        totalCount.setText(String.valueOf(total));
        susceptiblePercent.setText(percent(susceptible, total));
        infectedPercent.setText(percent(infected, total));
        recoveredPercent.setText(percent(recovered, total));
        deceasedPercent.setText(percent(deceased, total));
        tickLabel.setText("Tick: " + engine.getTickCount());
        populationLabel.setText("Population: " + total);
        updatePropertyStats();
    }

    /**
     * Recomputes the property-level statistics shown on the right panel: the
     * average infection duration and the immunity range among living persons.
     */
    private void updatePropertyStats() {
        Statistics snapshot = new Statistics(engine.getGrid(), engine.getTickCount());
        avgInfectionDaysLabel.setText(String.format(Locale.US,
                "Avg infection: %.1f days", snapshot.getAverageInfectionDays()));
        immunityRangeLabel.setText(String.format(Locale.US,
                "Immunity min/max: %.2f / %.2f",
                snapshot.getMinImmunity(), snapshot.getMaxImmunity()));
    }

    private String percent(int part, int total) {
        if (total == 0) {
            return "0%";
        }
        return Math.round(100.0 * part / total) + "%";
    }

    private void appendChartData(int tick) {
        Statistics latest = statisticsService.getLatest();
        if (latest == null) {
            return;
        }
        susceptibleSeries.getData().add(new XYChart.Data<>(
                tick, latest.getCount(DiseaseState.SUSCEPTIBLE)));
        infectedSeries.getData().add(new XYChart.Data<>(
                tick, latest.getCount(DiseaseState.INFECTED)));
        recoveredSeries.getData().add(new XYChart.Data<>(
                tick, latest.getCount(DiseaseState.RECOVERED)));
        deceasedSeries.getData().add(new XYChart.Data<>(
                tick, latest.getCount(DiseaseState.DECEASED)));
    }

    private void clearChart() {
        susceptibleSeries.getData().clear();
        infectedSeries.getData().clear();
        recoveredSeries.getData().clear();
        deceasedSeries.getData().clear();
    }

    private void updateStatusBar() {
        Grid grid = engine.getGrid();
        currentNeighborhoodLabel.setText(engine.getNeighborhood().getName());
        currentModeLabel.setText(grid.isToricMode() ? "toric" : "bounded");
        gridSizeLabel.setText(grid.getWidth() + "x" + grid.getHeight() + " cells");
    }

    private void syncControlsFromEngine() {
        Grid grid = engine.getGrid();
        SimulationSettings settings = engine.getSettings();
        widthField.setText(String.valueOf(grid.getWidth()));
        heightField.setText(String.valueOf(grid.getHeight()));
        toricCheckBox.setSelected(grid.isToricMode());
        transmissionSlider.setValue(settings.getTransmissionProbability() * 100.0);
        recoverySlider.setValue(settings.getRecoveryProbability() * 100.0);
        mortalitySlider.setValue(settings.getMortalityProbability() * 100.0);
        mobilitySlider.setValue(settings.getMobilityRate() * 100.0);
        maxDaysSlider.setValue(settings.getMaxInfectionDays());
        meanImmunitySlider.setValue(settings.getMeanImmunity() * 100.0);
        immunityVarianceSlider.setValue(settings.getImmunityVariance() * 100.0);
        vaccineEfficacySlider.setValue(settings.getVaccineEfficacy() * 100.0);
        speedSlider.setValue(settings.getSimulationSpeed());
    }

    private String getSelectedBrush() {
        if (susceptibleBrush.isSelected()) {
            return BRUSH_SUSCEPTIBLE;
        }
        if (infectedBrush.isSelected()) {
            return BRUSH_INFECTED;
        }
        if (recoveredBrush.isSelected()) {
            return BRUSH_RECOVERED;
        }
        if (deceasedBrush.isSelected()) {
            return BRUSH_DECEASED;
        }
        if (vaccinatedBrush.isSelected()) {
            return BRUSH_VACCINATED;
        }
        if (eraseBrush.isSelected()) {
            return BRUSH_ERASE;
        }
        return BRUSH_INFECTED;
    }

    private DiseaseState parseStateBrush(String brush) {
        return switch (brush) {
            case BRUSH_SUSCEPTIBLE -> DiseaseState.SUSCEPTIBLE;
            case BRUSH_INFECTED    -> DiseaseState.INFECTED;
            case BRUSH_RECOVERED   -> DiseaseState.RECOVERED;
            case BRUSH_DECEASED    -> DiseaseState.DECEASED;
            default -> throw new IllegalStateException("unknown brush: " + brush);
        };
    }

    private int parseRadius() {
        try {
            return Math.max(1, Integer.parseInt(radiusField.getText().trim()));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    private String formatProbability(double prob) {
        return String.format(Locale.US, "%.2f", prob);
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
