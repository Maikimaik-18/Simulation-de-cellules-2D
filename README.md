# Simulation de Cellules 2D

> Projet de fin d'études — ING1 Génie Informatique · CYTech · 2025-2026

Application graphique JavaFX de simulation cellulaire en plan 2D. Le thème
retenu est la **propagation d'une maladie** sur une population vivant sur une
grille. Les cellules (personnes) évoluent selon le temps, leur voisinage et
les interactions de l'utilisateur.

---

## Thème : propagation d'une maladie

La grille représente une **population** où chaque case est soit vide, soit
occupée par une **personne**. Une personne peut être dans l'un des 4 états :

| État          | Couleur | Signification                                        |
|---------------|---------|------------------------------------------------------|
| `SUSCEPTIBLE` | Vert    | Personne saine, peut être contaminée                 |
| `INFECTED`    | Rouge   | Personne malade, contamine ses voisins               |
| `RECOVERED`   | Bleu    | Personne guérie, immunisée à vie                     |
| `DECEASED`    | Gris    | Personne morte, ne participe plus à la simulation    |

À chaque tour de simulation, chaque personne :

1. **Vieillit** : si elle est malade, son compteur de jours d'infection augmente.
2. **Peut guérir** (probabilité de guérison) ou **mourir** (probabilité de mortalité).
3. **Se déplace** sur une case voisine vide (probabilité de mobilité).
4. **Contamine** ses voisins sains si elle est malade (probabilité de transmission).

Les 4 probabilités sont **réglables en temps réel** par l'utilisateur depuis
l'interface.

---

## Fonctionnalités

### Contrôle de la simulation
- Démarrer, mettre en pause, avancer pas à pas, réinitialiser
- Régler la vitesse en temps réel

### Configuration de la grille
- Choisir la taille de la grille
- Activer le mode **torique** (les bords se rejoignent)
- Choisir parmi **4 modes de voisinage** : orthogonal (4 cases),
  Moore (8 cases), euclidien (rayon), Manhattan (distance en pas)

### Gestion des personnes
- Ajouter une personne en cliquant sur la grille
- Supprimer une personne
- Remplir la grille aléatoirement

### Statistiques en direct
- Compteurs par état (sain, infecté, guéri, mort)
- Graphiques temps réel (courbes S/I/R/D au fil des tours)

### Sauvegarde / restauration
- Sauvegarder l'état complet d'une simulation dans un fichier
- Recharger une simulation depuis un fichier

---

## Équipe

Projet réalisé en binôme (dérogation validée par le directeur de projet) :

- **Moawad Maikel**
- **Boudraa Ryan**

### Outils de collaboration

| Besoin            | Outil utilisé                       |
|-------------------|-------------------------------------|
| Versionning       | GitHub                              |
| Communication     | WhatsApp + appels téléphoniques     |
| Suivi des tâches  | Page HTML de suivi mise en ligne    |
| Convention de code| Google Java Style (formatage auto)  |

---

## Prérequis

- **Java 21** ou supérieur ([Adoptium](https://adoptium.net/))
- **Maven 3.6+**

## Compilation & lancement

### Via Maven (depuis n'importe quel terminal)

```bash
# Compiler
mvn clean compile

# Lancer l'application JavaFX
mvn javafx:run
```

### Via les scripts fournis (depuis la racine du projet)

```bash
# Linux / macOS
./scripts/build.sh

# Windows
scripts\build.bat
```

---

## Structure du projet

```
.
├── README.md            Ce fichier
├── pom.xml              Configuration Maven (Java 21 + JavaFX 21)
├── .gitignore           Filtres Git
├── docs/
│   └── uml/             Diagrammes UML (PlantUML + PNG)
├── scripts/
│   ├── build.sh         Lancement depuis Linux / macOS
│   └── build.bat        Lancement depuis Windows
└── src/
    ├── main/
    │   ├── java/        Code source de l'application
    │   └── resources/   FXML, CSS, images
    └── test/
        └── java/        Tests unitaires (JUnit 5)
```

## Architecture logicielle

Le code est organisé en **5 packages** :

| Package         | Rôle                                                     |
|-----------------|----------------------------------------------------------|
| `ui`            | Interface JavaFX (App, MainController)                   |
| `model`         | Cœur de la simulation (SimulationEngine, Grid, Person)   |
| `neighborhood`  | 4 stratégies de voisinage interchangeables               |
| `stats`         | Calcul et historique des statistiques                    |
| `io`            | Sauvegarde et chargement de simulations                  |

### Design patterns utilisés

- **Strategy** : modes de voisinage interchangeables (`NeighborhoodStrategy`)
- **Observer** : notification automatique des statistiques (`SimulationListener`)
- **Service** : isolation des responsabilités transverses (`StatisticsService`, `SaveService`)
- **MVC** : séparation `ui` / `model` / contrôleur

---

## Documentation

- **Diagrammes UML** : [`docs/uml/`](docs/uml/)
  - `class-diagram.puml` + `.png` — diagramme de classes
  - `use-cases.puml` + `.png` — cas d'utilisation
  - `sequence-step.puml` + `.png` — séquence d'un tour de simulation
- **JavaDoc générée** : `docs/javadoc/` *(générée en fin de projet)*
- **Rapport final** : `docs/report.pdf` *(rendu final)*
