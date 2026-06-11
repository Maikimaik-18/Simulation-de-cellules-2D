package com.cellsimulation.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

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
import javafx.scene.chart.BarChart;
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

    // Slider permettant de régler l'immunité moyenne
// de toute la population.
//
// Exemple :
//
// 0 %  -> aucune protection naturelle
// 50 % -> population moyennement résistante
// 80 % -> population très résistante
//
// Cette valeur est stockée dans
// SimulationSettings.meanImmunity.
//
// Lorsqu'une nouvelle personne est créée,
// son immunité individuelle sera générée
// autour de cette moyenne.
    @FXML private Slider meanImmunitySlider;


    // Label affichant la valeur actuelle
// du slider d'immunité moyenne.
//
// Exemple :
// "40 %"
// "65 %"

    @FXML private Label meanImmunityValueLabel;

    // Slider permettant de contrôler
// la dispersion des immunités individuelles.
//
// Plus la variance est faible,
// plus les individus se ressemblent.
//
// Plus la variance est élevée,
// plus certaines personnes seront
// très fragiles et d'autres très résistantes.
//
// Cette valeur est utilisée dans
// SimulationEngine.drawImmunity().

    @FXML private Slider immunityVarianceSlider;


    @FXML private Label immunityVarianceValueLabel;

    // Slider permettant de régler
// l'efficacité du vaccin.
//
// Exemple :
//
// 0 %
// -> vaccin inutile
//
// 50 %
// -> protection moyenne
//
// 90 %
// -> très forte protection
//
// Cette valeur est stockée dans
// SimulationSettings.vaccineEfficacy.
//
// Lors du calcul du risque d'infection,
// une personne vaccinée utilise cette
// efficacité vaccinale pour augmenter
// son immunité effective.
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
    @FXML private BarChart<String, Number> infectionHistogramChart;
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
     * Méthode appelée automatiquement par JavaFX après le chargement
     * du fichier FXML et l'injection des composants @FXML.
     *
     * Son rôle est de préparer entièrement l'application avant que
     * l'utilisateur puisse interagir avec l'interface.
     */
    @FXML
    public void initialize() {

        // Création de la grille par défaut.
        //
        // DEFAULT_WIDTH = 20
        // DEFAULT_HEIGHT = 20
        // false = mode torique désactivé.
        //
        // On démarre donc avec une grille 20x20 bornée.
        Grid grid = new Grid(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);

        // Création des paramètres de simulation.
        //
        // Contient :
        // - transmission
        // - recovery
        // - mortality
        // - mobility
        // - max infection days
        // - speed
        // - mean immunity
        // - immunity variance
        // - vaccine efficacy
        SimulationSettings settings = new SimulationSettings();

        // Création du voisinage par défaut.
        //
        // Orthogonal = 4 voisins :
        // haut, bas, gauche, droite.
        NeighborhoodStrategy neighborhood = new OrthogonalNeighborhood();

        // Création du moteur principal de simulation.
        //
        // Le moteur centralise :
        // - la grille
        // - les paramètres
        // - le voisinage
        //
        // C'est lui qui exécutera les ticks de simulation.
        engine = new SimulationEngine(grid, settings, neighborhood);

        // Création du service de statistiques.
        //
        // Il enregistrera l'historique des données
        // à chaque tick de simulation.
        statisticsService = new StatisticsService();

        // Enregistrement du service de statistiques
        // comme observateur du moteur.
        //
        // À chaque tick :
        // engine.step()
        //
        // le moteur appellera :
        // statisticsService.onTick(...)
        engine.addListener(statisticsService);

        // Enregistrement du MainController
        // comme observateur du moteur.
        //
        // "this" représente l'objet MainController courant.
        //
        // Grâce à :
        // implements SimulationListener
        //
        // le moteur pourra appeler :
        // onTick(...)
        //
        // afin de mettre à jour automatiquement l'interface.
        engine.addListener(this);

        // Création des séries du graphique :
        // Susceptible
        // Infected
        // Recovered
        // Deceased
        initializeChartSeries();

        // Initialisation des contrôles JavaFX
        // avec les valeurs par défaut.
        //
        // Cette méthode configure :
        // - les sliders
        // - les labels
        // - les champs texte
        // - les nouveaux paramètres d'immunité
        // - les paramètres de vaccination
        populateDefaultControls(settings);

        // Création de la Timeline JavaFX.
        //
        // La Timeline est un minuteur qui appellera
        // automatiquement engine.step()
        // lorsque l'utilisateur lancera la simulation.
        //
        // La vitesse initiale est récupérée
        // depuis SimulationSettings.
        initializeTimeline(settings.getSimulationSpeed());

        // Branchement de tous les listeners dynamiques.
        //
        // Exemple :
        // - déplacement des sliders
        // - changement du voisinage
        // - dessin sur le Canvas
        wireDynamicListeners();

        // Adapte la taille du Canvas
        // aux dimensions de la grille.
        resizeCanvasToGrid();

        // Dessine la grille vide
        // sur le Canvas.
        drawGrid();

        // Met à jour les statistiques affichées.
        //
        // Au démarrage :
        // population = 0
        updateStats();

        // Met à jour la barre d'état.
        //
        // Exemple :
        // Tick : 0
        // Neighborhood : Orthogonal
        // Grid : 20x20
        updateStatusBar();
    }

    /**
     * Initialise les séries du graphique d'évolution.
     *
     * Chaque série représente l'évolution d'un état
     * de la population au cours du temps :
     * - Susceptible
     * - Infected
     * - Recovered
     * - Deceased
     *
     * Ces séries seront ensuite alimentées à chaque tick.
     */
    private void initializeChartSeries() {

        // Création de la série des personnes susceptibles.
        susceptibleSeries = new XYChart.Series<>();
        susceptibleSeries.setName("Susceptible");

        // Création de la série des personnes infectées.
        infectedSeries = new XYChart.Series<>();
        infectedSeries.setName("Infected");

        // Création de la série des personnes guéries.
        recoveredSeries = new XYChart.Series<>();
        recoveredSeries.setName("Recovered");

        // Création de la série des personnes décédées.
        deceasedSeries = new XYChart.Series<>();
        deceasedSeries.setName("Deceased");

        // Ajout des séries au graphique.
        evolutionChart.getData().add(susceptibleSeries);
        evolutionChart.getData().add(infectedSeries);
        evolutionChart.getData().add(recoveredSeries);
        evolutionChart.getData().add(deceasedSeries);
    }

    /**
     * Initialise tous les contrôles JavaFX avec
     * leurs valeurs par défaut.
     *
     * Cette méthode synchronise l'interface graphique
     * avec les paramètres initiaux de la simulation.
     */
    private void populateDefaultControls(SimulationSettings settings) {

        // Valeurs par défaut de la grille.
        widthField.setText(String.valueOf(DEFAULT_WIDTH));
        heightField.setText(String.valueOf(DEFAULT_HEIGHT));

        // Mode torique désactivé au démarrage.
        toricCheckBox.setSelected(false);

        // Remplit la ComboBox avec les différents voisinages.
        neighborhoodCombo.getItems().setAll(
                NEIGHBORHOOD_ORTHOGONAL,
                NEIGHBORHOOD_MOORE,
                NEIGHBORHOOD_EUCLIDEAN,
                NEIGHBORHOOD_MANHATTAN);

        // Sélection du voisinage par défaut.
        neighborhoodCombo.setValue(NEIGHBORHOOD_ORTHOGONAL);

        // Rayon par défaut pour les voisinages
        // Euclidean et Manhattan.
        radiusField.setText("2");

        // Caché tant que le voisinage ne nécessite pas de rayon.
        radiusField.setVisible(false);

        // Si invisible -> ne prend pas de place dans le layout.
        radiusField.managedProperty().bind(
                radiusField.visibleProperty());

        // Synchronisation des sliders avec SimulationSettings.

        transmissionSlider.setValue(
                settings.getTransmissionProbability() * 100.0);
        transmissionValueLabel.setText(
                formatProbability(
                        settings.getTransmissionProbability()));

        recoverySlider.setValue(
                settings.getRecoveryProbability() * 100.0);
        recoveryValueLabel.setText(
                formatProbability(
                        settings.getRecoveryProbability()));

        mortalitySlider.setValue(
                settings.getMortalityProbability() * 100.0);
        mortalityValueLabel.setText(
                formatProbability(
                        settings.getMortalityProbability()));

        mobilitySlider.setValue(
                settings.getMobilityRate() * 100.0);
        mobilityValueLabel.setText(
                formatProbability(
                        settings.getMobilityRate()));

        maxDaysSlider.setValue(
                settings.getMaxInfectionDays());
        maxDaysValueLabel.setText(
                String.valueOf(
                        settings.getMaxInfectionDays()));

        // Nouveaux paramètres ajoutés par Maikel.

        meanImmunitySlider.setValue(
                settings.getMeanImmunity() * 100.0);
        meanImmunityValueLabel.setText(
                formatProbability(
                        settings.getMeanImmunity()));

        immunityVarianceSlider.setValue(
                settings.getImmunityVariance() * 100.0);
        immunityVarianceValueLabel.setText(
                formatProbability(
                        settings.getImmunityVariance()));

        vaccineEfficacySlider.setValue(
                settings.getVaccineEfficacy() * 100.0);
        vaccineEfficacyValueLabel.setText(
                formatProbability(
                        settings.getVaccineEfficacy()));

        // Vitesse initiale de simulation.
        speedSlider.setValue(
                settings.getSimulationSpeed());
        speedValueLabel.setText(
                settings.getSimulationSpeed() + " ticks/s");

        // Le pinceau infecté est sélectionné par défaut.
        infectedBrush.setSelected(true);

        // Valeurs par défaut pour les fonctionnalités
        // de vaccination et remplissage aléatoire.
        vaccinationPercentField.setText("30");
        randomCountField.setText("50");
        randomInfectedField.setText("3");
    }

    /**
     * Initialise la Timeline JavaFX utilisée pour exécuter
     * automatiquement la simulation lorsqu'on clique sur Play.
     *
     * La Timeline agit comme un minuteur :
     * elle appelle périodiquement engine.step()
     * à une fréquence dépendant de la vitesse choisie.
     */
    private void initializeTimeline(int initialSpeed) {

        // Sécurité :
        // on empêche une vitesse inférieure à 1.
        //
        // Exemples :
        // initialSpeed = 0  -> speed = 1
        // initialSpeed = 5  -> speed = 5
        int speed = Math.max(1, initialSpeed);

        // Création de la Timeline JavaFX.
        timeline = new Timeline(

                // Un KeyFrame représente une action
                // exécutée à intervalle régulier.
                new KeyFrame(

                        // Calcul de l'intervalle entre deux ticks.
                        //
                        // speed = 1  -> 1000 ms -> 1 tick/seconde
                        // speed = 2  -> 500 ms  -> 2 ticks/seconde
                        // speed = 10 -> 100 ms  -> 10 ticks/seconde
                        Duration.millis(1000.0 / speed),

                        // Action exécutée à chaque déclenchement.
                        //
                        // e représente l'événement JavaFX.
                        //
                        // engine.step() fait avancer la simulation
                        // d'un tour (tick).
                        e -> engine.step()
                )
        );

        // La Timeline doit se répéter indéfiniment.
        //
        // Sans cette ligne :
        // engine.step() ne serait exécuté qu'une seule fois.
        //
        // Avec Timeline.INDEFINITE :
        //
        // tick 1
        // tick 2
        // tick 3
        // tick 4
        // ...
        // jusqu'à ce que l'utilisateur clique sur Pause.
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * Branche tous les écouteurs dynamiques
     * de l'interface graphique.
     *
     * Ces listeners permettent de réagir immédiatement
     * aux actions de l'utilisateur sans cliquer sur
     * un bouton supplémentaire. = "Je veux surveiller les actions de l'utilisateur
     * et réagir automatiquement lorsqu'il modifie un composant."
     */
    private void wireDynamicListeners() {

        // Changement de vitesse de simulation.
        //
        // Quand le slider bouge :
        // - SimulationSettings est mis à jour
        // - la Timeline est reconstruite
        // - le label est actualisé
        speedSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    int s = Math.max(1, newVal.intValue());

                    engine.getSettings().setSimulationSpeed(s);

                    speedValueLabel.setText(
                            s + " ticks/s");

                    timeline.stop();

                    timeline.getKeyFrames().setAll(
                            new KeyFrame(
                                    Duration.millis(1000.0 / s),
                                    e -> engine.step()));

                    if (engine.isRunning()) {
                        timeline.play();
                    }
                });

        // Transmission.
        transmissionSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double prob = newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setTransmissionProbability(prob);

                    transmissionValueLabel.setText(
                            formatProbability(prob));
                });

        // Recovery.
        recoverySlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double prob = newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setRecoveryProbability(prob);

                    recoveryValueLabel.setText(
                            formatProbability(prob));
                });

        // Mortality.
        mortalitySlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double prob = newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setMortalityProbability(prob);

                    mortalityValueLabel.setText(
                            formatProbability(prob));
                });

        // Mobility.
        mobilitySlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double prob = newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setMobilityRate(prob);

                    mobilityValueLabel.setText(
                            formatProbability(prob));
                });

        // Nombre maximum de jours d'infection.
        maxDaysSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    int days =
                            Math.max(1, newVal.intValue());

                    engine.getSettings()
                            .setMaxInfectionDays(days);

                    maxDaysValueLabel.setText(
                            String.valueOf(days));
                });

        // Immunité moyenne.
        meanImmunitySlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double prob =
                            newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setMeanImmunity(prob);

                    meanImmunityValueLabel.setText(
                            formatProbability(prob));
                });

        // Variance d'immunité.
        immunityVarianceSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double variance =
                            newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setImmunityVariance(variance);

                    immunityVarianceValueLabel.setText(
                            formatProbability(variance));
                });

        // Efficacité vaccinale.
        vaccineEfficacySlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    double efficacy =
                            newVal.doubleValue() / 100.0;

                    engine.getSettings()
                            .setVaccineEfficacy(efficacy);

                    vaccineEfficacyValueLabel.setText(
                            formatProbability(efficacy));
                });

        // Changement de voisinage.
        neighborhoodCombo.setOnAction(
                e -> handleNeighborhoodSelection());

        // Gestion du dessin sur le Canvas.
        gridCanvas.setOnMousePressed(
                this::handleCanvasPaint);

        gridCanvas.setOnMouseDragged(
                this::handleCanvasPaint);

        // Fin du dessin.
        gridCanvas.setOnMouseReleased(
                e -> lastPaintedPosition = null);


        radiusField.textProperty().addListener((obs, oldValue, newValue) -> {
            String selected = neighborhoodCombo.getValue();

            if (NEIGHBORHOOD_EUCLIDEAN.equals(selected)
                    || NEIGHBORHOOD_MANHATTAN.equals(selected)) {
                handleNeighborhoodSelection();
            }
        });
    }


    /**
     * Démarre ou met en pause la simulation.
     *
     * <p>Cette méthode est appelée lorsque l'utilisateur clique
     * sur le bouton Play/Pause de l'interface.
     *
     */

    @FXML
    private void onPlayPause() {

        // Vérifie si la simulation est actuellement en cours d'exécution
        if (engine.isRunning()) {

            // Met le moteur de simulation en pause
            engine.pause();

            // Arrête la génération automatique des ticks
            timeline.pause();

            // Met à jour le texte du bouton
            playPauseButton.setText("Play");

        } else {

            // Démarre le moteur de simulation
            engine.start();

            // Lance la génération automatique des ticks
            timeline.play();

            // Met à jour le texte du bouton
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
        drawGrid();
        updateStats();
        updateStatusBar();
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
     * Applique une nouvelle configuration de grille.
     *
     * <p>Cette méthode est appelée lorsque l'utilisateur clique
     * sur le bouton "Apply" dans la section Grid.
     *
     * <p>Le processus est le suivant :
     * <ol>
     *   <li>Lecture de la largeur et de la hauteur saisies.</li>
     *   <li>Lecture de l'état du mode torique.</li>
     *   <li>Création d'une nouvelle grille.</li>
     *   <li>Récupération des paramètres actuels de simulation.</li>
     *   <li>Récupération du voisinage actuellement sélectionné.</li>
     *   <li>Création d'un nouveau SimulationEngine utilisant la nouvelle grille.</li>
     *   <li>Réinitialisation de l'affichage et des statistiques.</li>
     * </ol>
     *
     * <p>La simulation en cours est donc remplacée par une nouvelle
     * simulation vide utilisant la nouvelle configuration.
     *
     * <p>Si les dimensions saisies sont invalides, un message d'erreur
     * est affiché à l'utilisateur.
     */
    @FXML
    private void onApplyGridConfig() {
        try {

            // Récupère la largeur saisie par l'utilisateur
            int width = Integer.parseInt(widthField.getText().trim());

            // Récupère la hauteur saisie par l'utilisateur
            int height = Integer.parseInt(heightField.getText().trim());

            // Vérifie si le mode torique est activé
            boolean toric = toricCheckBox.isSelected();

            // Création de la nouvelle grille
            Grid newGrid = new Grid(width, height, toric);

            // Conserve les paramètres actuels de simulation
            SimulationSettings currentSettings = engine.getSettings();

            // Conserve le voisinage actuellement sélectionné
            NeighborhoodStrategy currentNeighborhood = engine.getNeighborhood();

            // Arrête la simulation en cours
            timeline.pause();
            playPauseButton.setText("Play");

            // Création d'un nouveau moteur utilisant la nouvelle grille
            engine = new SimulationEngine(
                    newGrid,
                    currentSettings,
                    currentNeighborhood);

            // Création d'un nouveau service de statistiques
            statisticsService = new StatisticsService();

            // Réenregistrement des listeners
            engine.addListener(statisticsService);
            engine.addListener(this);

            // Réinitialise le graphique d'évolution
            clearChart();

            // Redimensionne le canvas à la nouvelle taille
            resizeCanvasToGrid();

            // Redessine la grille vide
            drawGrid();

            // Met à jour les statistiques
            updateStats();

            // Met à jour la barre d'état
            updateStatusBar();

            // Affiche un message de confirmation
            statusLabel.setText(
                    "Grid: " + width + "x" + height
                            + " (" + (toric ? "toric" : "bounded") + ")");

        } catch (NumberFormatException ex) {

            // Erreur de conversion des dimensions
            showError("Invalid grid dimensions: " + ex.getMessage());

        } catch (IllegalArgumentException ex) {

            // Erreur lors de la création de la grille
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
     * Sauvegarde la simulation actuelle dans un fichier.
     *
     * <p>Cette méthode est appelée lorsque l'utilisateur clique sur :
     *
     * File -> Save...
     *
     * <p>Le déroulement est le suivant :
     * <ol>
     *   <li>Ouverture d'une fenêtre de sélection de fichier.</li>
     *   <li>Choix de l'emplacement de sauvegarde par l'utilisateur.</li>
     *   <li>Appel du SaveService pour sauvegarder la simulation.</li>
     *   <li>Affichage d'un message de confirmation dans la barre d'état.</li>
     * </ol>
     *
     * <p>En cas d'erreur lors de la sauvegarde, un message d'erreur est affiché.
     */
    @FXML
    private void onSave() {

        // Création de la fenêtre de sélection de fichier
        FileChooser chooser = new FileChooser();

        // Titre affiché en haut de la fenêtre
        chooser.setTitle("Save simulation");

        // Limitation aux fichiers de type .sim
        chooser.getExtensionFilters().add(
                new ExtensionFilter("Simulation files", "*.sim"));

        // Ouverture de la fenêtre de sauvegarde
        File file = chooser.showSaveDialog(
                playPauseButton.getScene().getWindow());

        // Si l'utilisateur clique sur "Annuler"
        if (file == null) {
            return;
        }

        try {

            // Délégation de la sauvegarde au SaveService
            saveService.save(engine, file.getAbsolutePath());

            // Mise à jour de la barre d'état
            statusLabel.setText("Saved to " + file.getName());

        } catch (IOException ex) {

            // Affichage d'un message d'erreur si la sauvegarde échoue
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
     * average infection duration, the immunity range among living persons and
     * the histogram of infection durations among infected persons.
     */
    private void updatePropertyStats() {
        Statistics snapshot = new Statistics(engine.getGrid(), engine.getTickCount());
        avgInfectionDaysLabel.setText(String.format(Locale.US,
                "Avg infection: %.1f days", snapshot.getAverageInfectionDays()));
        immunityRangeLabel.setText(String.format(Locale.US,
                "Immunity min/max: %.2f / %.2f",
                snapshot.getMinImmunity(), snapshot.getMaxImmunity()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<Integer, Integer> histogram =
                new TreeMap<>(snapshot.getInfectionDaysHistogram());
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            series.getData().add(new XYChart.Data<>(
                    String.valueOf(entry.getKey()), entry.getValue()));
        }
        infectionHistogramChart.getData().clear();
        infectionHistogramChart.getData().add(series);
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
