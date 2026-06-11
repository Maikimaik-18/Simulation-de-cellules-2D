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
 *
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
     * Point d'entrée du mode CLI.
     *
     * Cette méthode est exécutée quand on lance cette classe depuis le terminal.
     * Elle prépare le programme, affiche les informations de départ,
     * puis lance la boucle qui attend les commandes de l'utilisateur.
     *
     * @param args arguments de ligne de commande, non utilisés ici
     */
    public static void main(String[] args) {

        // On crée un objet CliRunner.
        // Le constructeur CliRunner() va créer une simulation par défaut :
        // - une grille 20x20
        // - des paramètres par défaut
        // - un voisinage orthogonal
        // - un moteur SimulationEngine
        // - un service de statistiques branché au moteur
        CliRunner runner = new CliRunner();

        // On récupère la grille actuellement utilisée par le moteur.
        // runner.engine = le moteur de simulation
        // getGrid() = donne accès à la grille stockée dans ce moteur
        Grid grid = runner.engine.getGrid();

        // On affiche un titre pour indiquer que l'application est lancée en mode console.
        println("Cell Simulation 2D - CLI mode");

        // On affiche les informations principales de la simulation actuelle :
        // - largeur de la grille
        // - hauteur de la grille
        // - type de grille : torique ou bornée
        // - type de voisinage utilisé
        // - numéro du tick actuel
        println("Grid: " + grid.getWidth() + "x" + grid.getHeight()
                + " (" + (grid.isToricMode() ? "toric" : "bounded") + ") | "
                + "Neighborhood: " + runner.engine.getNeighborhood().getName()
                + " | Tick: " + runner.engine.getTickCount());

        // On indique à l'utilisateur qu'il peut taper help
        // pour voir toutes les commandes disponibles.
        println("Type 'help' for the list of commands.");

        // On lance la boucle interactive du CLI.
        // À partir d'ici, le programme attend les commandes de l'utilisateur :
        // help, print, stats, add, step, play, save, load, quit, etc.
        runner.run();
    }
    public CliRunner() {

        Grid grid = new Grid(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_TORIC);

        SimulationSettings settings = new SimulationSettings();

        NeighborhoodStrategy neighborhood = new OrthogonalNeighborhood();

        this.engine = new SimulationEngine(grid, settings, neighborhood);

        this.statisticsService = new StatisticsService();

        this.engine.addListener(this.statisticsService);
    }


    //run() est le point d'entrée du CLI : c'est elle qui fait le lien entre les commandes tapées par
    // l'utilisateur et les différentes fonctionnalités du projet.

    private void run() {

        // Scanner permet de lire ce que tape l'utilisateur dans le terminal.
        Scanner scanner = new Scanner(System.in); //Via son clavier

        // Variable qui contrôle la boucle principale.
        // Tant qu'elle vaut true, le programme continue.
        boolean keepRunning = true;

        // Boucle principale du CLI.
        while (keepRunning) {

            // Affiche le symbole > comme dans un terminal.
            System.out.print("> ");

            // Vérifie qu'il reste bien une ligne à lire.
            // Si l'entrée est fermée, on sort.
            if (!scanner.hasNextLine()) {
                break;
            }

            // Lit la ligne tapée par l'utilisateur.
            String line = scanner.nextLine().trim(); //Permet de supp les espaces inutiles

            // Ignore les lignes vides.
            if (line.isEmpty()) {
                continue;
            }

            // Découpe la commande en morceaux.
            String[] tokens = line.split("\\s+");

            // Premier mot = nom de la commande.
            String command = tokens[0].toLowerCase(); //eviter les problème de majuscule

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

                    default ->
                            error("Unknown command: "
                                    + command
                                    + ". Type 'help'.");
                }

            } catch (IllegalArgumentException | IOException e) {

                error(e.getMessage()); //recup le message d'erreur et l'affiche dans le terminam

            } catch (ClassNotFoundException e) { // Si un objet sauvegardé n'est pas trouvable / a été supprimé

                error("cannot load: " + e.getMessage());
            }
        }

        println("Goodbye.");
    }


    //
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


    //Cette méthode sert à afficher l'état actuel de la grille dans le terminal afin que
    // l'utilisateur puisse visualiser la simulation sans interface graphique.

    private void handlePrint() {

        // On récupère la grille actuellement utilisée
        // par le moteur de simulation.
        Grid grid = engine.getGrid();

        // On parcourt toutes les lignes.
        for (int row = 0; row < grid.getHeight(); row++) {

            // StringBuilder est utilisé pour construire
            // progressivement la ligne à afficher.
            StringBuilder line = new StringBuilder();

            // On parcourt toutes les colonnes.
            for (int col = 0; col < grid.getWidth(); col++) {

                // Ajoute un espace entre deux cellules
                // pour rendre l'affichage plus lisible.
                if (col > 0) {
                    line.append(' ');
                }

                // On récupère la personne présente
                // à la position (row, col).
                Person occupant =
                        grid.getCell(new Position(row, col));

                // Si la case est vide :
                // afficher '.'
                // Sinon afficher S, I, R ou D.
                line.append(
                        occupant == null
                                ? '.'
                                : symbolOf(occupant)
                );
            }

            // Affiche la ligne complète.
            println(line.toString());
        }
    }


    /**
     * Affiche les statistiques actuelles de la simulation.
     *
     * Cette méthode parcourt toute la grille afin de compter
     * combien de personnes sont dans chaque état :
     * - SUSCEPTIBLE
     * - INFECTED
     * - RECOVERED
     * - DECEASED
     *
     * Puis elle affiche un résumé dans le terminal.
     */
    private void handleStats() {

        // On récupère la grille actuellement utilisée
        // par le moteur de simulation.
        Grid grid = engine.getGrid();

        // Compteur du nombre de personnes susceptibles.
        int susceptible = 0;

        // Compteur du nombre de personnes infectées.
        int infected = 0;

        // Compteur du nombre de personnes guéries.
        int recovered = 0;

        // Compteur du nombre de personnes décédées.
        int deceased = 0;

        // Parcours de toutes les lignes de la grille.
        for (int row = 0; row < grid.getHeight(); row++) {

            // Parcours de toutes les colonnes de la grille.
            for (int col = 0; col < grid.getWidth(); col++) {

                // Récupère la personne située à la position
                // (row, col).
                Person occupant =
                        grid.getCell(new Position(row, col));

                // Si la case est vide,
                // on passe directement à la suivante.
                if (occupant == null) {
                    continue;
                }

                // On regarde l'état de la personne
                // puis on incrémente le compteur correspondant.
                switch (occupant.getState()) {

                    // Personne saine mais pouvant être contaminée.
                    case SUSCEPTIBLE -> susceptible++;

                    // Personne actuellement malade.
                    case INFECTED -> infected++;

                    // Personne guérie.
                    case RECOVERED -> recovered++;

                    // Personne décédée.
                    case DECEASED -> deceased++;
                }
            }
        }

        // Calcul du nombre total de personnes présentes
        // dans la simulation.
        int total =
                susceptible +
                        infected +
                        recovered +
                        deceased;

        // Calcul du nombre total de cases de la grille.
        // Exemple :
        // grille 20x20 = 400 cases.
        int cells =
                grid.getWidth() *
                        grid.getHeight();

        // Affiche le numéro du tick actuel.
        println("Tick: " + engine.getTickCount());

        // Affiche le nombre de personnes susceptibles.
        println(String.format(
                "%-12s %d",
                "Susceptible:",
                susceptible));

        // Affiche le nombre de personnes infectées.
        println(String.format(
                "%-12s %d",
                "Infected:",
                infected));

        // Affiche le nombre de personnes guéries.
        println(String.format(
                "%-12s %d",
                "Recovered:",
                recovered));

        // Affiche le nombre de personnes décédées.
        println(String.format(
                "%-12s %d",
                "Deceased:",
                deceased));

        // Affiche le nombre total de personnes
        // ainsi que la capacité totale de la grille.
        println(String.format(
                "%-12s %d (over %d cells)",
                "Total:",
                total,
                cells));
    }





    /**
     * Réinitialise complètement la simulation
     * avec une nouvelle grille.
     *
     * Syntaxe :
     * init <width> <height> [toric]
     *
     * Exemple :
     * init 30 20
     * init 30 20 toric
     */
    private void handleInit(String[] tokens) {

        // Vérifie que l'utilisateur a fourni
        // au moins largeur et hauteur.
        //
        // Exemple valide :
        // init 30 20
        //
        // Exemple invalide :
        // init 30
        if (tokens.length < 3) {

            // Génère une exception qui sera récupérée
            // par le try/catch de la méthode run().
            throw new IllegalArgumentException(
                    "usage: init <width> <height> [toric]");
        }

        // Conversion de la largeur.
        //
        // tokens[1] contient un texte :
        //
        // "30"
        //
        // Integer.parseInt transforme ce texte
        // en entier 30.
        int width = Integer.parseInt(tokens[1]);

        // Conversion de la hauteur.
        int height = Integer.parseInt(tokens[2]);

        // Détermine si la grille doit être torique.
        //
        // Exemple :
        //
        // init 30 20 toric
        //
        // tokens[3] = "toric"
        //
        // toric devient true.
        //
        // Sinon toric vaut false.
        boolean toric =
                tokens.length >= 4
                        && tokens[3].equalsIgnoreCase("toric");

        // Création d'une nouvelle grille.
        //
        // Exemple :
        //
        // new Grid(30, 20, true)
        //
        // crée une grille
        // 30 colonnes
        // 20 lignes
        // torique
        Grid newGrid =
                new Grid(width, height, toric);

        // On récupère les paramètres actuels.
        //
        // Exemple :
        // transmission = 0.7
        // recovery = 0.3
        //
        // On veut les conserver.
        SimulationSettings currentSettings =
                engine.getSettings();

        // On récupère le voisinage actuel.
        //
        // Exemple :
        // MooreNeighborhood
        //
        // On veut également le conserver.
        NeighborhoodStrategy currentNeighborhood =
                engine.getNeighborhood();

        // Création d'un NOUVEAU moteur.
        //
        // C'est la ligne la plus importante.
        //
        // L'ancien moteur est remplacé.
        //
        // Le nouveau moteur utilise :
        // - la nouvelle grille
        // - les anciens paramètres
        // - le même voisinage
        engine = new SimulationEngine(
                newGrid,
                currentSettings,
                currentNeighborhood);

        // Création d'un nouveau service de statistiques.
        //
        // Pourquoi ?
        //
        // Parce qu'on redémarre complètement
        // la simulation.
        statisticsService =
                new StatisticsService();

        // On reconnecte les statistiques
        // au nouveau moteur.
        //
        // Sinon StatisticsService
        // ne recevrait plus les ticks.
        engine.addListener(statisticsService);

        // Message de confirmation.
        //
        // Exemple :
        //
        // Grid reinitialized to 30x20 (toric).
        println("Grid reinitialized to "
                + width + "x" + height
                + " ("
                + (toric ? "toric" : "bounded")
                + ").");
    }

    /**
     * Ajoute une personne dans la grille.
     *
     * Syntaxe :
     * add <row> <col> <state>
     *
     * Exemples :
     *
     * add 5 3 I
     * add 10 4 susceptible
     */
    private void handleAdd(String[] tokens) {

        // Vérifie que l'utilisateur a fourni
        // les 3 paramètres obligatoires :
        //
        // ligne
        // colonne
        // état
        //
        // Exemple valide :
        // add 5 3 I
        //
        // Exemple invalide :
        // add 5 3
        if (tokens.length < 4) {

            // Génère une exception qui sera
            // récupérée dans le try/catch de run().
            throw new IllegalArgumentException(
                    "usage: add <row> <col> <state>");
        }

        // Récupère le numéro de ligne.
        //
        // Exemple :
        // "5" → 5
        int row = Integer.parseInt(tokens[1]);

        // Récupère le numéro de colonne.
        //
        // Exemple :
        // "3" → 3
        int col = Integer.parseInt(tokens[2]);

        // Convertit le texte saisi
        // en valeur DiseaseState.
        //
        // Exemple :
        // "I" → DiseaseState.INFECTED
        //
        // "S" → DiseaseState.SUSCEPTIBLE
        //
        // Cette conversion est réalisée
        // par la méthode parseState().
        DiseaseState state =
                parseState(tokens[3]);

        // Création d'un objet Position.
        //
        // Exemple :
        // Position(5,3)
        //
        // Puis demande au moteur de simulation
        // d'ajouter une personne à cette position
        // avec l'état indiqué.
        engine.addCell(
                new Position(row, col),
                state);
    }

    /**
     * Supprime une personne de la grille.
     *
     * Syntaxe :
     * remove <row> <col>
     *
     * Exemple :
     * remove 5 3
     */
    private void handleRemove(String[] tokens) {

        // Vérifie que l'utilisateur a fourni
        // les deux paramètres obligatoires :
        //
        // ligne
        // colonne
        //
        // Exemple valide :
        // remove 5 3
        //
        // Exemple invalide :
        // remove 5
        if (tokens.length < 3) {

            // Génère une exception qui sera
            // récupérée dans le try/catch de run().
            throw new IllegalArgumentException(
                    "usage: remove <row> <col>");
        }

        // Convertit le numéro de ligne
        // saisi par l'utilisateur.
        //
        // Exemple :
        // "5" → 5
        int row = Integer.parseInt(tokens[1]);

        // Convertit le numéro de colonne
        // saisi par l'utilisateur.
        //
        // Exemple :
        // "3" → 3
        int col = Integer.parseInt(tokens[2]);

        // Création d'un objet Position.
        //
        // Exemple :
        // Position(5,3)
        //
        // Puis demande au moteur de simulation
        // de supprimer la personne située
        // à cette position.
        engine.removeCell(
                new Position(row, col));
    }

    /**
     * Place plusieurs personnes aléatoirement dans la grille.
     *
     * Syntaxe :
     * random <count> [infected]
     *
     * Exemples :
     *
     * random 10
     * -> place 10 personnes au hasard,
     *    avec 1 infectée par défaut.
     *
     * random 10 3
     * -> place 10 personnes au hasard,
     *    dont 3 infectées.
     */
    private void handleRandom(String[] tokens) {

        // Vérifie que l'utilisateur a au moins donné
        // le nombre total de personnes à placer.
        //
        // Exemple valide :
        // random 10
        //
        // Exemple invalide :
        // random
        if (tokens.length < 2) {

            // Si l'utilisateur n'a pas donné le nombre,
            // on lance une erreur qui sera récupérée
            // dans le try/catch de run().
            throw new IllegalArgumentException(
                    "usage: random <count> [infected]");
        }

        // Convertit le nombre total de personnes à placer.
        //
        // Exemple :
        // tokens[1] = "10"
        // count = 10
        int count = Integer.parseInt(tokens[1]);

        // Détermine le nombre de personnes infectées.
        //
        // Si l'utilisateur a donné un 2e nombre :
        // random 10 3
        // alors infected = 3.
        //
        // Sinon, par défaut, infected = 1.
        int infected =
                tokens.length >= 3
                        ? Integer.parseInt(tokens[2])
                        : 1;

        // Vérifie qu'on place au moins une personne.
        //
        // Exemple invalide :
        // random 0
        if (count < 1) {
            throw new IllegalArgumentException(
                    "count must be >= 1, got " + count);
        }

        // Vérifie que le nombre d'infectés
        // n'est pas négatif.
        //
        // Exemple invalide :
        // random 10 -2
        if (infected < 0) {
            throw new IllegalArgumentException(
                    "infected must be >= 0, got " + infected);
        }

        // Vérifie qu'il n'y a pas plus d'infectés
        // que de personnes placées au total.
        //
        // Exemple invalide :
        // random 10 15
        if (infected > count) {
            throw new IllegalArgumentException(
                    "infected (" + infected + ") must be <= count (" + count + ")");
        }

        // Récupère la grille actuelle depuis le moteur.
        Grid grid = engine.getGrid();

        // Calcule la capacité maximale de la grille.
        //
        // Exemple :
        // grille 20x20 = 400 cases.
        int capacity =
                grid.getWidth() *
                        grid.getHeight();

        // Vérifie qu'on ne demande pas à placer
        // plus de personnes qu'il n'y a de cases.
        //
        // Exemple :
        // grille 20x20 = 400 cases
        // random 500 -> impossible.
        if (count > capacity) {
            throw new IllegalArgumentException(
                    "count (" + count + ") must be <= grid capacity (" + capacity + ")");
        }

        // Set = collection qui n'accepte pas les doublons.
        //
        // Ici, on l'utilise pour garantir qu'une même position
        // ne sera pas tirée deux fois.
        //
        // Exemple :
        // si Position(2,3) est déjà dedans,
        // elle ne sera pas ajoutée une deuxième fois.
        Set<Position> positions =
                new HashSet<>();

        // Random permet de générer des nombres aléatoires.
        Random random =
                new Random();

        // Tant qu'on n'a pas généré assez de positions uniques,
        // on continue à tirer des coordonnées aléatoires.
        while (positions.size() < count) {

            // Tire une ligne aléatoire entre 0
            // et grid.getHeight() - 1.
            //
            // Exemple si height = 20 :
            // valeur possible entre 0 et 19 (grace a nextInt)
            int row =
                    random.nextInt(grid.getHeight());

            // Tire une colonne aléatoire entre 0
            // et grid.getWidth() - 1.
            //
            // Exemple si width = 20 :
            // valeur possible entre 0 et 19.
            int col =
                    random.nextInt(grid.getWidth());

            // Crée une position avec les coordonnées tirées.
            //
            // Comme positions est un Set,
            // si cette position existe déjà,
            // elle ne sera pas ajoutée une deuxième fois.
            positions.add(
                    new Position(row, col));
        }

        // Compteur du nombre de personnes déjà placées.
        int placed = 0;

        // Parcourt toutes les positions générées aléatoirement.
        for (Position position : positions) {

            // Détermine l'état de la personne à placer.
            //
            // Tant que placed est inférieur au nombre
            // d'infectés demandé, on place des INFECTED.
            //
            // Ensuite, on place des SUSCEPTIBLE.
            DiseaseState state =
                    placed < infected
                            ? DiseaseState.INFECTED
                            : DiseaseState.SUSCEPTIBLE;

            // Demande au moteur d'ajouter une personne
            // à cette position avec l'état choisi.
            engine.addCell(
                    position,
                    state);

            // On incrémente le nombre de personnes placées.
            placed++;
        }

        // Message de confirmation pour l'utilisateur.
        println("Placed "
                + count
                + " persons ("
                + infected
                + " infected).");
    }





    /**
     * Exécute un seul tick de simulation.
     *
     * Cette méthode demande au moteur
     * d'avancer d'un tour, puis affiche
     * la nouvelle grille et les statistiques
     * mises à jour.
     */
    private void handleStep() {

        // Demande au moteur de simulation
        // d'exécuter un tick.
        //
        // C'est LA ligne la plus importante.
        //
        // À l'intérieur de step(),
        // le moteur :
        //
        // - parcourt les personnes
        // - applique les règles de contamination
        // - applique les règles de guérison
        // - applique les règles de mortalité
        // - applique éventuellement les déplacements
        // - met à jour le compteur de ticks
        // - notifie les listeners
        //
        // Toute la logique métier est ici.
        engine.step();

        // Affiche la grille après la mise à jour.
        //
        // Cela permet à l'utilisateur de voir
        // le résultat du tick qui vient d'être exécuté.
        handlePrint();

        // Affiche les statistiques mises à jour.
        //
        // Exemple :
        // nombre d'infectés
        // nombre de guéris
        // nombre de décès
        handleStats();
    }

    /**
     * Exécute plusieurs ticks de simulation à la suite.
     *
     * Syntaxe :
     * play <n>
     *
     * Exemple :
     * play 5
     *
     * Cette commande fait avancer la simulation
     * de 5 tours, puis affiche la grille
     * et les statistiques à chaque tour.
     */
    private void handlePlay(String[] tokens) {

        // Vérifie que l'utilisateur a bien donné
        // le nombre de ticks à jouer.
        //
        // Exemple valide :
        // play 5
        //
        // Exemple invalide :
        // play
        if (tokens.length < 2) {
            throw new IllegalArgumentException(
                    "usage: play <n>");
        }

        // Convertit le nombre de ticks saisi
        // par l'utilisateur en entier.
        //
        // Exemple :
        // tokens[1] = "5"
        // n = 5
        int n = Integer.parseInt(tokens[1]);

        // Vérifie que le nombre de ticks est au moins 1.
        //
        // Exemple invalide :
        // play 0
        if (n < 1) {
            throw new IllegalArgumentException(
                    "n must be >= 1, got " + n);
        }

        // Boucle qui répète la simulation n fois.
        //
        // Exemple :
        // si n = 5,
        // turn va prendre les valeurs :
        // 0, 1, 2, 3, 4
        //
        // Donc engine.step() sera appelé 5 fois.
        for (int turn = 0; turn < n; turn++) {

            // Demande au moteur d'exécuter un tick.
            //
            // C'est ici que la simulation avance réellement.
            engine.step();

            // Récupère la grille après le tick.
            //
            // On la récupère après engine.step()
            // pour analyser son nouvel état.
            Grid grid = engine.getGrid();

            // Compteurs pour les statistiques du tour actuel.
            int susceptible = 0;
            int infected = 0;
            int recovered = 0;
            int deceased = 0;

            // Parcourt toutes les lignes de la grille.
            for (int row = 0; row < grid.getHeight(); row++) {

                // Parcourt toutes les colonnes de la grille.
                for (int col = 0; col < grid.getWidth(); col++) {

                    // Récupère la personne située
                    // à la position actuelle.
                    Person occupant =
                            grid.getCell(new Position(row, col));

                    // Si la case est vide,
                    // on passe à la case suivante.
                    if (occupant == null) {
                        continue;
                    }

                    // Regarde l'état de la personne
                    // puis incrémente le compteur correspondant.
                    switch (occupant.getState()) {

                        // Personne saine mais contaminable.
                        case SUSCEPTIBLE -> susceptible++;

                        // Personne infectée.
                        case INFECTED -> infected++;

                        // Personne guérie.
                        case RECOVERED -> recovered++;

                        // Personne décédée.
                        case DECEASED -> deceased++;
                    }
                }
            }

            // Affiche un résumé compact du tick actuel.
            //
            // Exemple :
            // Tick 4 - S:10 I:3 R:2 D:1
            println("Tick " + engine.getTickCount() + " - "
                    + "S:" + susceptible + " I:" + infected
                    + " R:" + recovered + " D:" + deceased);

            // Affiche la grille après ce tick.
            handlePrint();
        }
    }

    /**
     * Modifie un paramètre de la simulation.
     *
     * Syntaxe :
     * set <param> <value>
     *
     * Exemples :
     *
     * set transmission 0.8
     * set recovery 0.3
     * set mortality 0.1
     * set mobility 0.5
     * set max-days 10
     * set speed 100
     *
     * Les paramètres sont stockés dans
     * l'objet SimulationSettings.
     */
    private void handleSet(String[] tokens) {

        // Vérifie que l'utilisateur a fourni
        // un paramètre et une valeur.
        //
        // Exemple valide :
        // set transmission 0.8
        //
        // Exemple invalide :
        // set transmission
        if (tokens.length < 3) {

            throw new IllegalArgumentException(
                    "usage: set <param> <value>");
        }

        // Récupère le nom du paramètre.
        //
        // Exemple :
        // "Transmission"
        //
        // devient :
        // "transmission"
        //
        // grâce à toLowerCase().
        String param =
                tokens[1].toLowerCase();

        // Récupère les paramètres actuels
        // stockés dans le moteur.
        //
        // C'est cet objet qui contient :
        //
        // transmissionProbability
        // recoveryProbability
        // mortalityProbability
        // mobilityRate
        // maxInfectionDays
        // simulationSpeed
        SimulationSettings settings =
                engine.getSettings();

        // Analyse le paramètre demandé.
        switch (param) {

            // Modifie la probabilité
            // de transmission.
            //
            // Exemple :
            // set transmission 0.8
            //
            // Double.parseDouble transforme :
            // "0.8" → 0.8
            case "transmission" ->

                    settings.setTransmissionProbability(
                            Double.parseDouble(tokens[2]));

            // Modifie la probabilité
            // de guérison.
            case "recovery" ->

                    settings.setRecoveryProbability(
                            Double.parseDouble(tokens[2]));

            // Modifie la probabilité
            // de mortalité.
            case "mortality" ->

                    settings.setMortalityProbability(
                            Double.parseDouble(tokens[2]));

            // Modifie la probabilité
            // de déplacement.
            case "mobility" ->

                    settings.setMobilityRate(
                            Double.parseDouble(tokens[2]));

            // Modifie le nombre maximum
            // de jours d'infection.
            //
            // Exemple :
            // set max-days 14
            case "max-days" ->

                    settings.setMaxInfectionDays(
                            Integer.parseInt(tokens[2]));

            // Modifie la vitesse
            // de la simulation.
            //
            // Exemple :
            // set speed 100
            case "speed" ->

                    settings.setSimulationSpeed(
                            Integer.parseInt(tokens[2]));

            // Paramètre inconnu.
            default ->

                    throw new IllegalArgumentException(
                            "unknown param: " + param);
        }

        // Affiche une confirmation.
        //
        // Exemple :
        // transmission = 0.8
        println(param + " = " + tokens[2]);
    }

    /**
     * Modifie la stratégie de voisinage utilisée
     * par la simulation.
     *
     * Syntaxe :
     *
     * neighborhood orthogonal
     * neighborhood moore
     * neighborhood euclidean <radius>
     * neighborhood manhattan <radius>
     *
     * Cette méthode illustre le Design Pattern Strategy :
     * on peut remplacer dynamiquement l'algorithme
     * de calcul des voisins.
     */
    private void handleNeighborhood(String[] tokens) {

        // Vérifie que l'utilisateur a indiqué
        // au moins le type de voisinage.
        //
        // Exemple valide :
        // neighborhood moore
        //
        // Exemple invalide :
        // neighborhood
        if (tokens.length < 2) {

            throw new IllegalArgumentException(
                    "usage: neighborhood <mode> [radius]");
        }

        // Récupère le mode demandé.
        //
        // Exemple :
        // "Moore"
        //
        // devient :
        // "moore"
        String mode =
                tokens[1].toLowerCase();

        // Création de la stratégie correspondant
        // au mode demandé.
        NeighborhoodStrategy strategy =

                switch (mode) {

                    // Voisinage orthogonal :
                    //
                    //     X
                    //   X C X
                    //     X
                    //
                    // 4 voisins.
                    case "orthogonal" ->

                            new OrthogonalNeighborhood();

                    // Voisinage Moore :
                    //
                    // X X X
                    // X C X
                    // X X X
                    //
                    // 8 voisins.
                    case "moore" ->

                            new MooreNeighborhood();

                    // Voisinage Euclidien.
                    //
                    // Nécessite un rayon.
                    //
                    // Exemple :
                    // neighborhood euclidean 2
                    case "euclidean" -> {

                        // Vérifie que le rayon est fourni.
                        if (tokens.length < 3) {

                            throw new IllegalArgumentException(
                                    "usage: neighborhood euclidean <radius>");
                        }

                        // yield sert à renvoyer la valeur
                        // du bloc switch.
                        //
                        // Ici on crée un voisinage
                        // euclidien avec le rayon demandé.
                        yield new EuclideanNeighborhood(
                                Integer.parseInt(tokens[2]));
                    }

                    // Voisinage Manhattan.
                    //
                    // Exemple :
                    // neighborhood manhattan 3
                    case "manhattan" -> {

                        // Vérifie que le rayon existe.
                        if (tokens.length < 3) {

                            throw new IllegalArgumentException(
                                    "usage: neighborhood manhattan <radius>");
                        }

                        // Création de la stratégie Manhattan.
                        yield new ManhattanNeighborhood(
                                Integer.parseInt(tokens[2]));
                    }

                    // Si le mode n'existe pas.
                    default ->

                            throw new IllegalArgumentException(
                                    "unknown mode: " + mode);
                };

        // Remplace la stratégie actuelle
        // dans le moteur de simulation.
        //
        // À partir du prochain step(),
        // SimulationEngine utilisera
        // cette nouvelle stratégie.
        engine.setNeighborhood(strategy);

        // Affiche une confirmation.
        //
        // Exemple :
        // Neighborhood = Moore
        println("Neighborhood = "
                + strategy.getName());
    }

    /**
     * Sauvegarde l'état actuel de la simulation
     * dans un fichier.
     *
     * Syntaxe :
     * save <filepath>
     *
     * Exemple :
     * save simulation.bin
     */
    private void handleSave(String[] tokens) throws IOException {

        // Vérifie que l'utilisateur a fourni
        // un chemin de fichier.
        //
        // Exemple valide :
        // save simulation.bin
        //
        // Exemple invalide :
        // save
        if (tokens.length < 2) {

            throw new IllegalArgumentException(
                    "usage: save <filepath>");
        }

        // Récupère le chemin du fichier.
        //
        // Exemple :
        // "simulation.bin"
        String filePath = tokens[1];

        // Demande au SaveService de sauvegarder
        // le moteur de simulation dans ce fichier.
        //
        // On transmet :
        //
        // - engine : tout l'état de la simulation
        // - filePath : où enregistrer
        //
        // SaveService se charge ensuite
        // de l'écriture du fichier.
        saveService.save(
                engine,
                filePath);

        // Affiche un message de confirmation.
        //
        // Exemple :
        // Saved to simulation.bin at tick 42.
        println(
                "Saved to "
                        + filePath
                        + " at tick "
                        + engine.getTickCount()
                        + ".");
    }

    /**
     * Charge une simulation sauvegardée.
     *
     * Syntaxe :
     * load <filepath>
     *
     * Exemple :
     * load simulation.bin
     *
     * Cette méthode remplace complètement
     * la simulation actuelle par celle
     * contenue dans le fichier.
     */

    private void handleLoad(String[] tokens)
            throws IOException, ClassNotFoundException {

        // Vérifie que l'utilisateur a fourni
        // le chemin du fichier à charger.
        //
        // Exemple valide :
        // load simulation.bin
        //
        // Exemple invalide :
        // load
        if (tokens.length < 2) {

            throw new IllegalArgumentException(
                    "usage: load <filepath>");
        }

        // Récupère le chemin du fichier.
        //
        // Exemple :
        // "simulation.bin"
        String filePath = tokens[1];

        // Demande au SaveService de charger
        // le fichier.
        //
        // Cette ligne est la plus importante.
        //
        // SaveService va :
        // - ouvrir le fichier
        // - lire les données
        // - reconstruire un SimulationEngine
        //
        // L'ancien moteur est remplacé.
        engine = saveService.load(filePath);

        // Création d'un nouveau service
        // de statistiques.
        //
        // Pourquoi ?
        //
        // Parce que le moteur chargé depuis
        // le fichier ne possède pas forcément
        // le listener actuellement utilisé.
        statisticsService =
                new StatisticsService();

        // On reconnecte le StatisticsService
        // au moteur qui vient d'être chargé.
        //
        // Ainsi les statistiques seront
        // mises à jour lors des prochains ticks.
        engine.addListener(
                statisticsService);

        // On récupère la grille chargée
        // pour afficher ses informations.
        Grid grid =
                engine.getGrid();

        // Affiche un résumé de la simulation chargée.
        //
        // Exemple :
        //
        // Loaded from simulation.bin.
        // Grid: 20x20 (bounded)
        // Neighborhood: Moore
        // Tick: 42
        println(
                "Loaded from "
                        + filePath
                        + ". Grid: "
                        + grid.getWidth()
                        + "x"
                        + grid.getHeight()
                        + " ("
                        + (grid.isToricMode()
                        ? "toric"
                        : "bounded")
                        + ")"
                        + " | Neighborhood: "
                        + engine.getNeighborhood().getName()
                        + " | Tick: "
                        + engine.getTickCount());
    }



    /**
     * Réinitialise complètement la simulation.
     *
     * Cette méthode demande au moteur
     * de remettre la simulation à son état initial.
     */
    private void handleReset() {

        // Demande au moteur de simulation
        // d'effectuer une réinitialisation complète.
        //
        // À l'intérieur de engine.reset(),
        // le moteur va généralement :
        //
        // - vider la grille
        // - remettre le tick à 0
        // - arrêter la simulation si elle tournait
        // - prévenir les listeners (StatisticsService)
        //
        // Toute la logique de réinitialisation
        // est centralisée dans SimulationEngine.
        engine.reset();

        // Affiche un message de confirmation
        // pour informer l'utilisateur que
        // la réinitialisation a été effectuée.
        println("Engine reset.");
    }

    /**
     * Convertit un texte saisi par l'utilisateur
     * en valeur DiseaseState.
     *
     * Exemples :
     *
     * "S" -> DiseaseState.SUSCEPTIBLE
     * "I" -> DiseaseState.INFECTED
     * "R" -> DiseaseState.RECOVERED
     * "D" -> DiseaseState.DECEASED
     */
    private DiseaseState parseState(String token) {

        // Analyse le texte saisi.
        //
        // toLowerCase() permet d'accepter :
        //
        // "I"
        // "i"
        // "INFECTED"
        // "infected"
        //
        // sans distinction majuscule/minuscule.
        return switch (token.toLowerCase()) {

            // Si l'utilisateur tape :
            // "s" ou "susceptible"
            //
            // on retourne la valeur
            // DiseaseState.SUSCEPTIBLE
            case "s", "susceptible" ->
                    DiseaseState.SUSCEPTIBLE;

            // Si l'utilisateur tape :
            // "i" ou "infected"
            //
            // on retourne :
            // DiseaseState.INFECTED
            case "i", "infected" ->
                    DiseaseState.INFECTED;

            // Si l'utilisateur tape :
            // "r" ou "recovered"
            //
            // on retourne :
            // DiseaseState.RECOVERED
            case "r", "recovered" ->
                    DiseaseState.RECOVERED;

            // Si l'utilisateur tape :
            // "d" ou "deceased"
            //
            // on retourne :
            // DiseaseState.DECEASED
            case "d", "deceased" ->
                    DiseaseState.DECEASED;

            // Si aucune correspondance n'est trouvée,
            // on génère une erreur.
            //
            // Exemple :
            // add 5 3 toto
            default ->
                    throw new IllegalArgumentException(
                            "unknown state: " + token);
        };
    }

    /**
     * Convertit l'état d'une personne
     * en caractère d'affichage.
     *
     * Exemples :
     *
     * SUSCEPTIBLE -> 'S'
     * INFECTED -> 'I'
     * RECOVERED -> 'R'
     * DECEASED -> 'D'
     */
    private char symbolOf(Person person) {

        // On récupère l'état de la personne
        // puis on retourne le caractère associé.
        return switch (person.getState()) {

            // Personne saine mais pouvant être contaminée.
            case SUSCEPTIBLE -> 'S';

            // Personne actuellement infectée.
            case INFECTED -> 'I';

            // Personne guérie.
            case RECOVERED -> 'R';

            // Personne décédée.
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
