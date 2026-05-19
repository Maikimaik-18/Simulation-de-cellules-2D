# UML Diagrams

This folder contains the UML diagrams of the Cell Simulation 2D project,
written in [PlantUML](https://plantuml.com) so they can be versioned in Git
and regenerated automatically.

## Files

| File                    | Purpose                                                 |
|-------------------------|---------------------------------------------------------|
| `class-diagram.puml`    | Class diagram of the entire application                 |
| `use-cases.puml`        | Use case diagram (User perspective)                     |
| `sequence-step.puml`    | Sequence diagram of one simulation tick (bonus diagram) |

## How to generate the PNG / SVG images

You need either Java + the PlantUML JAR, or the IntelliJ plugin.

### Option 1 — Command line (recommended for CI)

1. Download `plantuml.jar` from <https://plantuml.com/download>
2. Place it at the root of the project (or anywhere on your `$PATH`)
3. Generate all diagrams:

```bash
java -jar plantuml.jar docs/uml/*.puml
```

This produces `class-diagram.png`, `use-cases.png` and `sequence-step.png`
next to the `.puml` source files.

For SVG output (sharper, scalable, recommended for the final report):

```bash
java -jar plantuml.jar -tsvg docs/uml/*.puml
```

### Option 2 — IntelliJ IDEA plugin

1. `File → Settings → Plugins → Marketplace`
2. Search for **PlantUML Integration** and install it
3. Open any `.puml` file → live preview appears in a side panel
4. Right-click in the preview → `Save Diagram` to export PNG/SVG

### Option 3 — Online (quick check)

Paste the content of a `.puml` file at <https://www.plantuml.com/plantuml/uml/>

## Convention

- **Always edit the `.puml` source** — never edit the PNG directly.
- After editing, regenerate the PNG and commit both files.
- The PNG is what we ship in the final report; the `.puml` is what we maintain.
