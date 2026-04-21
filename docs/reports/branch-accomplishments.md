# BMDExpress-3 — Branch Accomplishments Report

**Branches:** `experiment-metadata` and `bmdx-core`
**Base:** `main` (upstream Sciome)
**Report date:** 2026-04-17
**Author/operator:** Dan Svoboda (dlsvob@gmail.com)

---

## Executive summary

Two branches, stacked.

1. **`experiment-metadata`** — 18 commits on top of `main`.
   Adds a full experimental-metadata system to BMDExpress-3: auto-parsing from
   file headers, controlled vocabularies (externalized to YAML), a rules-engine
   validator (easy-rules), editing dialogs (single + batch), and propagation of
   the 8 metadata columns through every downstream analysis data type. Also
   introduces a visual GUI test runner built on TestFX.

2. **`bmdx-core`** — 1 committed commit on top of `experiment-metadata`, plus
   substantial uncommitted work. The committed work strips the JavaFX GUI,
   analysis engines (BMD modeling, prefilters, category analysis), and
   associated CLI runners — keeping only the data model, metadata system, and
   .bm2 I/O. The uncommitted work layers in a **domain-neutral data model**
   (Endpoint / EndpointResponse interfaces) so the core can hold apical tox
   study data and categorical observations alongside genomics.

Throughout, much of the work was planned and executed in Claude Code sessions
spanning Dec 2025 → Apr 2026. The conversation log (excerpted below) is the
connective tissue between commits.

---

## Part 1 — `experiment-metadata` branch

### Commit timeline

```
e99db41  2025-12-01  Add experimental metadata capture with controlled vocabularies
6a8ecb3  2025-12-??  Add ability to edit experiment metadata for existing experiments
ced2a53  2025-12-02  Add experiment metadata editing and display
25b0624  2025-12-??  Refactor metadata to support In Vivo/In Vitro hierarchy and enforce data integrity
c98b57e  2025-12-??  Add expanded experiment metadata fields with validation
db21925  2025-12-??  Update README.md
0aa38ac  2025-12-??  Merge remote-tracking branch 'origin/main' into experiment-metadata
c57351e  2025-12-??  Merge branch 'experiment-metadata' of auerbachs/BMDExpress-3
256daba  2025-12-??  Refactor experiment metadata code for DRY and maintainability
f567e2d  2025-12-??  Refactor: Collapse ExperimentDescription hierarchy into unified class
7055204  2025-12-??  Refactor: Extract status bar formatting to ExperimentDescription
024eba5  2025-12-??  Externalize vocabularies to YAML configuration
f43e8f0  2025-12-??  Fix: Handle null experiments from validation failures
0857ef4  2025-12-??  Restore platform chooser dialog for expression data import
82847ef  2025-12-12  Add convenience methods for ExperimentDescription access
f0e7e1c  2026-02-26  Add visual GUI test runner with TestFX
995d99c  2026-02-??  Add experiment metadata validation rules and description dialogs
0767a73  2026-02-??  Propagate ExperimentDescription metadata columns through entire pipeline
```

**Diff summary vs `main`:**
- **54 files changed, 5,900 insertions, 50 deletions**
- 18 net-new classes (model, rules, dialogs, guitest infra)
- 1 new YAML configuration (`vocabulary.yml`)

### Conversation glue — how it was built

The Claude Code transcripts in `~/.claude/projects/-home-svobodadl-Dev-Projects-BMDExpress-3/`
show how this branch evolved:

- **`c1e98253.jsonl`** (5.5MB, Mar 3) — starts with "what is the name of the
  active branch?" and "how is it different from main?" The user's framing was:
  *"review this additional code. is it any good? does it follow the coding
  pattern and practice of main?"* This became the driver for the DRY /
  hierarchy-collapse / vocabulary-externalization commits below.

- **`4965bbe9.jsonl`** (2.0MB, Feb 26) — plan-driven: *"Implement the following
  plan: Fix Misalignments with Main Branch Patterns."* This corresponds to the
  `995d99c` / `f567e2d` / `024eba5` sequence.

- **`7cd3506f.jsonl`** (2.6MB, Feb 26) — *"Implement the following plan: Visual
  GUI Test Runner with TestFX"* → commit `f0e7e1c`.

- **`9d30fa37.jsonl`** (4.5MB, Mar 3) — *"Implement the following plan:
  Integrate ExperimentDescription as a First-Class Part of the Object Model"*
  → commit `0767a73` (metadata-column propagation).

- **`af484666.jsonl`** (1.5MB, Mar 3) — *"Implement the following plan: Make
  Expression Data Import Pipeline Robust for Metadata"* and *"branch the
  repo. llm-metadata"* — spawning the (now separate) `llm-metadata` branch.

### 1.1 The metadata model

**Key file:** `src/main/java/com/sciome/bmdexpress2/mvp/model/info/ExperimentDescription.java`

Final unified class after the hierarchy collapse (commit `f567e2d`). The
*.java file went from three classes (`ExperimentDescriptionBase`,
`InVivoExperimentDescription`, `InVitroExperimentDescription`) with routes
of administration (`OralRoute`, `InhalationRoute`, `IntravenousRoute`,
`TransdermalRoute`) to one unified `ExperimentDescription` using plain
fields with validation rules enforcing the in-vivo vs in-vitro constraints.

Core fields (abridged):

```java
public class ExperimentDescription implements Serializable
{
    // Common
    private TestArticleIdentifier testArticle;
    private String subjectType;           // "in vivo" | "in vitro"
    private String provider;
    private String platform;
    private String articleType;
    private String articleRoute;          // oral | inhaled | transdermal | ...
    private String articleVehicle;        // corn oil | feed | water | aerosol | gas
    private String administrationMeans;   // gavage (oral only)
    private String studyDuration;
    private String inVivoDuration;

    // In-vivo fields
    private String species;
    private String strain;
    private String sex;
    private String organ;

    // In-vitro field
    private String cellLine;
```

The class delegates all vocabulary lookups to `VocabularyConfig` (YAML-backed
singleton) rather than hard-coding them:

```java
public static List<String> getSpeciesVocabulary() {
    return VocabularyConfig.getInstance().getSpecies();
}
public static List<String> getOrganVocabulary() {
    return VocabularyConfig.getInstance().getOrgans();
}
public static List<String> getStrainsForSpecies(String species) {
    return VocabularyConfig.getInstance().getStrainsForSpecies(species);
}
// ... plus ARTICLE_ROUTE_VOCABULARY, SEX_VOCABULARY, STRAINS_BY_SPECIES,
// IN_VIVO_DURATION_VOCABULARY, STUDY_DURATION_VOCABULARY, ARTICLE_TYPE_VOCABULARY
```

Commit `7055204` moved status-bar formatting out of `MainPresenter` and into
the model itself:

```java
// ExperimentDescription.java
public String getStatusBarString() {
    StringBuilder sb = new StringBuilder();
    if (testArticle != null && testArticle.hasIdentifier()) { ... }
    if (species != null && !species.isEmpty())
        sb.append(" | Species: ").append(species);
    // ... strain, sex, organ, cellLine
    return sb.toString();
}
```

### 1.2 Vocabulary externalization (`024eba5`)

**New file:** `src/main/resources/vocabulary.yml`

Before this commit, species / organ / strain lists were hard-coded arrays
inside `ExperimentDescription.java`. After, they live in YAML:

```yaml
# vocabulary.yml (excerpts)
providers:
  - Affymetrix
  - Agilent
  - BioSpyder
  - RefSeq
  - Ensembl
  - Clinical Endpoint
  - Generic

subjectTypes:
  - in vivo
  - in vitro

articleRoutes:
  - oral
  - inhaled
  - transdermal

articleVehicles:
  - corn oil
  - feed
  - water
  - aerosol
  - gas

administrationMeans:
  - gavage

species:
  - rat
  - mouse
  - human
  - rabbit
  - dog
  - monkey
  - zebrafish
  - guinea pig
  - hamster
  - pig

sexes:
  - male
  - female
  - both
  - mixed
  - NA

organs: [adrenal, blood, bone, brain, colon, heart, intestine, kidney,
         liver, lung, ovary, pancreas, skin, spleen, stomach, testis,
         thymus, thyroid, uterus]

strains:
  rat:
    - Sprague-Dawley
    - Wistar
    - Fischer 344
  mouse:
    - C57BL/6
    - BALB/c
    - DBA/2
    ...
```

**New file:** `src/main/java/com/sciome/bmdexpress2/mvp/model/info/VocabularyConfig.java`

Singleton with SnakeYAML parsing and memoized getters. Wires up
`module-info.java` with `requires org.yaml.snakeyaml;` and `opens` of the
`info` package for reflective access.

### 1.3 Validation rules engine (`995d99c`)

**New subpackage:** `src/main/java/com/sciome/bmdexpress2/mvp/model/info/rules/`

Uses **Easy Rules** (`org.jeasy.rules:easy-rules-core:4.1.0`). Each rule is
a pojo registered with a central validator:

```java
// MetadataValidator.java (excerpt)
public class MetadataValidator extends ValidatorBase<MetadataFacts>
{
    private static final MetadataValidator INSTANCE = new MetadataValidator();

    @Override
    protected void registerRules(Rules rules) {
        // Subject Type rules
        rules.register(new SubjectTypeRules.InVitroDurationRule());
        rules.register(new SubjectTypeRules.InVivoDurationRule());
        rules.register(new SubjectTypeRules.InVitroRequiresCellLine());
        rules.register(new SubjectTypeRules.InVivoRequiresSpecies());
        rules.register(new SubjectTypeRules.InVivoRequiresStrain());
        rules.register(new SubjectTypeRules.InVivoRequiresSex());
        rules.register(new SubjectTypeRules.InVivoRequiresOrgan());
        rules.register(new SubjectTypeRules.InVitroCannotHaveInVivoFields());

        // Article Route rules (oral/inhaled/transdermal constraints)
        rules.register(new ArticleRouteRules.AdminMeansOnlyForOral());
        rules.register(new ArticleRouteRules.OralRequiresAdminMeans());
        rules.register(new ArticleRouteRules.TransdermalNoVehicle());
        rules.register(new ArticleRouteRules.EMFNoVehicle());
        rules.register(new ArticleRouteRules.OralVehicleValidation());
        rules.register(new ArticleRouteRules.InhaledVehicleValidation());

        // Species/Strain consistency rules
        rules.register(new SpeciesStrainRules.StrainMatchesSpecies());

        // Platform/Provider rules
        rules.register(new PlatformRules.ProviderMatchesPlatform());
    }
}
```

Usage is a fluent facts builder:

```java
MetadataFacts facts = new MetadataFacts()
    .subjectType("in vivo")
    .species("rat")
    .strain("C57BL/6")          // mouse strain — rule will flag this
    .articleRoute("oral")
    .administrationMeans("gavage");

MetadataValidator.validateMetadata(facts);

if (facts.hasErrors()) {
    for (String error : facts.getErrors()) {
        // "Strain 'C57BL/6' does not belong to species 'rat'"
    }
}
```

### 1.4 Parsing metadata from file headers

**File:** `src/main/java/com/sciome/bmdexpress2/util/ExperimentDescriptionParser.java`
**Size:** 779 lines (biggest single new file in the branch)

Pipeline: read the header block of an expression data file (`# key: value`
style), tokenize, map each key to a setter via a keyword dictionary, validate
against `MetadataValidator`, return an `ExperimentDescription` + a list of
issues.

Representative keyword dictionary:

```java
private static final String[] TEST_ARTICLE_KEYS =
    {"test article", "testarticle", "chemical", "compound", "article"};
private static final String[] SPECIES_KEYS =
    {"species", "organism"};
private static final String[] STRAIN_KEYS =
    {"strain"};
private static final String[] SEX_KEYS =
    {"sex", "gender"};
private static final String[] ORGAN_KEYS =
    {"organ", "tissue"};
// ... ARTICLE_ROUTE_KEYS, ARTICLE_VEHICLE_KEYS, PLATFORM_KEYS, PROVIDER_KEYS,
//     IN_VIVO_DURATION_KEYS, STUDY_DURATION_KEYS, ARTICLE_TYPE_KEYS, etc.

private static final Set<String> SEX_KEYWORDS = new HashSet<>(Arrays.asList(
    "male", "female", "m", "f", "both", "mixed", "na"));
```

Each field is parsed with `parseVocabularyField()` which validates against
the YAML vocabulary before calling the setter. Unrecognized values produce
an issue (not a failure).

### 1.5 UI: metadata dialogs

Two dialogs, both `Dialog<...>` subclasses:

**Single experiment dialog** —
`src/main/java/com/sciome/bmdexpress2/mvp/view/mainstage/ExperimentDescriptionDialog.java`
(~270 lines).

Layout: `GridPane` with rows for Test Article, Subject Type, Species,
Strain, Sex, Organ, Cell Line. Editable `ComboBox`es for controlled-vocab
fields; `TextField` for free-text. Subject-Type selector toggles which
field group is visible. OK button disabled until required fields are
filled.

**Batch dialog** —
`src/main/java/com/sciome/bmdexpress2/mvp/view/mainstage/BatchExperimentDescriptionDialog.java`
(~317 lines original). Scrollable `VBox` of per-experiment `GridPane`s.
Returns `Map<DoseResponseExperiment, ExperimentDescription>`.

Both go through the validator on OK and present issues in a blocking
alert if any rule fails.

Wired into `ProjectNavigationView` via right-click context menu:

```java
// ProjectNavigationView.java
MenuItem editMetadata = new MenuItem("Edit Description");
editMetadata.setOnAction(e -> presenter.editExperimentMetadata(selected));

MenuItem batchEdit = new MenuItem("Batch Edit Metadata");
batchEdit.setOnAction(e -> presenter.batchEditMetadata(multi));
```

And wired into the import flow (`ProjectNavigationPresenter.onLoadExperiement`):

```java
// Collect experiments whose metadata is incomplete, then prompt for each.
List<DoseResponseExperiment> needMetadata = new ArrayList<>();
for (DoseResponseExperiment exp : experiments) {
    ExperimentDescription desc = exp.getExperimentDescription();
    if (desc == null || !isMetadataComplete(desc)) {
        needMetadata.add(exp);
    }
}
for (DoseResponseExperiment exp : needMetadata) {
    ExperimentDescription updated = getView()
        .showExperimentDescriptionDialog(exp.getExperimentDescription(), exp.getName());
    if (updated != null) exp.setExperimentDescription(updated);
}
```

### 1.6 Metadata columns across the entire pipeline (`0767a73`)

**Scope:** 17 files, 894 insertions.

This commit makes the 8 scientifically meaningful metadata columns — Test
Article, CASRN, Species, Strain, Sex, Organ, Cell Line, Subject Type —
accessible on every result type. Two new methods on `ExperimentDescription`:

```java
public List<String> getColumnHeaders() {
    return Arrays.asList(
        COL_TEST_ARTICLE, COL_CASRN, COL_SPECIES, COL_STRAIN,
        COL_SEX, COL_ORGAN, COL_CELL_LINE, COL_SUBJECT_TYPE);
}

public List<Object> getColumnValues() {
    // TestArticleIdentifier fields can themselves be null even when
    // the testArticle object exists, so we double-check both levels.
    String testArticleName = (testArticle != null) ? testArticle.getName() : null;
    String casrn = (testArticle != null) ? testArticle.getCasrn() : null;
    return Arrays.asList(
        nullToEmpty(testArticleName), nullToEmpty(casrn),
        nullToEmpty(species), nullToEmpty(strain),
        nullToEmpty(sex), nullToEmpty(organ),
        nullToEmpty(cellLine), nullToEmpty(subjectType));
}
```

Then the same columns are injected into every result row via the
`BMDExpressAnalysisRow.getRow()` override in:
- `BMDResult.java` (+ `ProbeStatResult.java`)
- `OneWayANOVAResult.java` / `OneWayANOVAResults.java`
- `WilliamsTrendResult.java` / `WilliamsTrendResults.java`
- `OriogenResult.java` / `OriogenResults.java`
- `CurveFitPrefilterResult.java` / `CurveFitPrefilterResults.java`
- `CategoryAnalysisResult.java` / `CategoryAnalysisResults.java`

Each results class gained a method like:

```java
// BMDResult.java
public List<String> getColumnHeader() {
    List<String> header = new ArrayList<>();
    header.addAll(doseResponseExperiment.getExperimentDescription().getColumnHeaders());
    header.addAll(...existing columns...);
    return header;
}
```

Commit `82847ef` added the upward traversal so downstream result types can
reach the description:

```java
// CategoryAnalysisResults.java — traverses BMDResult → DoseResponseExperiment
public ExperimentDescription getExperimentDescription() {
    if (bmdResults == null || bmdResults.isEmpty()) return null;
    return bmdResults.get(0).getExperimentDescription();
}

// BMDResult.java — traverses DoseResponseExperiment
public ExperimentDescription getExperimentDescription() {
    return (doseResponseExperiment != null)
            ? doseResponseExperiment.getExperimentDescription() : null;
}

// PrefilterResults.java — default interface method
default ExperimentDescription getExperimentDescription() {
    DoseResponseExperiment dre = getDoseResponseExperiment();
    return (dre != null) ? dre.getExperimentDescription() : null;
}
```

### 1.7 Visual GUI test runner (`f0e7e1c`)

**New subpackage:** `src/test/java/com/sciome/bmdexpress2/guitest/`

Built on **TestFX** (added to `pom.xml`: `org.testfx:testfx-core:4.0.16-alpha`,
`testfx-junit5:4.0.16-alpha`). The user's prompt in the Feb 26 transcript:

> *"do we have a way to run gui tests visually? i.e. i want to have a list
> of workflows, and the user actions that comprise them, and be able to
> step through them so that i can watch."*

Result: a YAML-configured workflow engine with visual step-through.

Components:
- **`WorkflowDefinition`** — root type, holds name / description / setup / steps
- **`StepDefinition`** — one user action: `click`, `type`, `select`, `assert`
- **`SetupDefinition`** — per-workflow preconditions (files to create, etc.)
- **`WorkflowLoader`** — SnakeYAML parser for `src/test/resources/workflows/*.yaml`
- **`ActionExecutor`** — dispatches each step to the TestFX `FxRobot`
- **`ControlPanel`** — JavaFX side-panel UI: list of workflows, step list,
  Play / Pause / Step-Next buttons
- **`VisualTestRunner`** — main entrypoint; launches the target app plus the
  control panel and binds them together

Sample workflow:

```yaml
# src/test/resources/workflows/import-expression-data.yaml
name: Import Expression Data
description: Import a sample expression file and confirm metadata prompt appears
setup:
  - createFile: testdata/sample_expression.txt
steps:
  - click: "#menuFile"
  - click: "#menuImportExpression"
  - type: "#filePathField" "testdata/sample_expression.txt"
  - click: "#btnImport"
  - assert: "Experiment Description dialog appears"
  - type: "#testArticleField" "PFOA"
  - select: "#speciesField" "rat"
  - click: "OK"
```

Menu wire-up in `MenuBarView`:

```java
MenuItem visualTestRunner = new MenuItem("Visual Test Runner...");
visualTestRunner.setOnAction(e -> new VisualTestRunner().start(new Stage()));
```

### 1.8 Bm2 deserialization test (`0767a73`)

**File:** `src/test/java/com/sciome/bmdexpress2/model/Bm2DeserializationTest.java` (361 lines)

Guards against Jackson deserialization regressions. Walks a fixture `.bm2`
file, deserializes it, and asserts every expected field on every analysis
type round-trips. Particularly important after the object-model refactors
in `f567e2d` and `025b0624`.

---

## Part 2 — `bmdx-core` branch

### 2.1 Committed state: the strip (`caebfe1`)

**Single commit.** Message:

> Strip BMDExpress 3 to headless bmdx-core library (132 sources, 288KB)
>
> Remove GUI (JavaFX views, charts, dialogs), analysis engines (BMD
> modeling, prefilters, category analysis), and their CLI runners.
> Keep the full data model (88 classes for .bm2 deserialization),
> native CLI I/O commands (combine, export, query, delete), metadata
> system (ExperimentDescription, vocabulary, parser), and utilities
> (ExperimentFileUtil, ProjectUtilities, annotation).
>
> Add sync-from-upstream.sh for merging parent branch model changes
> with automatic missing-class restoration and compile verification.

**Diff stat vs `experiment-metadata`:** 501 files changed, **238 insertions,
93,674 deletions**.

#### What was removed

| Package | What | Why |
| --- | --- | --- |
| `commandline/` (analysis runners) | `ANOVARunner`, `BMDAnalysisRunner`, `CategoryAnalysisRunner`, `CurveFitPrefilterRunner`, `NonParametricAnalysisRunner`, `OriogenRunner`, `WilliamsTrendRunner`, `AnalyzeRunner` (1,335 lines alone) | Analysis-engine entry points |
| `commandline/config/**` | `BMDSConfig`, `CategoryConfig`, `IVIVEConfig`, `NonParametricConfig`, `PrefilterConfig`, etc. | Configuration for removed engines |
| `mvp/presenter/**` | `BMDAnalysisPresenter`, `CategorizationPresenter`, `MainPresenter`, `MenuBarPresenter`, `ProjectNavigationPresenter` (1,108 lines) and all prefilter / visualization / dataview presenters | JavaFX GUI layer |
| `mvp/view/**` | `BMDAnalysisView`, `CategorizationView`, `AnnotationUpdateView`, plus the single + batch description dialogs, curve fit view | JavaFX GUI layer |
| `mvp/view/mainstage/` | `ProjectNavigationView`, MenuBarView, dialogs | JavaFX GUI layer |
| `guitest/**` + `workflows/**` + `testdata/**` | Entire visual test runner + sample data | Depended on the removed views |
| `src/main/resources/tissues/*` | `Adipose`, `Adrenal Gland`, `Bone Marrow`, `Brain`, `Colon`, `Heart`, `Kidney`, `Liver`, `Lung`, `Ovary`, `Pancreas`, `Skin`, `Spleen`, `Stomach`, `Testis`, `Thymus`, `Thyroid`, `Uterus`, `tissues.txt` (18 files) | Tissue-specific enrichment tables used only by the removed category-analysis engine |
| `mvp/model/Bm2DeserializationTest` | Legacy test | Moved to `src/test/` in uncommitted work |

#### What was kept

- **Entire data model** (88 classes): `BMDExpressAnalysisDataSet`, `BMDProject`,
  `DoseResponseExperiment`, `BMDResult`, `OneWayANOVAResults`,
  `WilliamsTrendResults`, `OriogenResults`, `CurveFitPrefilterResults`,
  `CategoryAnalysisResults`, `ProbeResponse`, `Probe`, `Treatment`,
  `ChipInfo`, `AnalysisInfo`, the full stat result hierarchy, etc.
- **Metadata system**: `ExperimentDescription`, `VocabularyConfig`,
  `TestArticleIdentifier`, rules/ subpackage, `ExperimentDescriptionParser`.
- **.bm2 I/O**: `ExperimentFileUtil`, `ProjectUtilities`, annotation utilities.
- **CLI I/O commands**: `combine`, `export`, `query`, `delete` (the pieces
  in `BMDExpressCommandLine` that read/write `.bm2` without running analyses).
- **vocabulary.yml**.

#### What was added

**`scripts/sync-from-upstream.sh`** (151 lines) — the one net-new file
beyond the stripping. Because bmdx-core is a curated subset, any future
data-model change in `experiment-metadata` needs to be re-synced. This
script does it safely:

```bash
# Sync strategy (abridged from the script):
#   1. Merge the upstream branch (experiment-metadata) into bmdx-core,
#      preserving our deletions via `-X ours` merge strategy on any
#      previously-removed files.
#   2. Try to compile.  If clean, done.
#   3. If compile fails with "cannot find symbol" errors, parse the
#      missing class names from the Maven output, check them out
#      directly from the upstream branch, retry compilation.
#   4. Loop up to MAX_ROUNDS=5 times to cover transitive dependencies
#      (class A needs class B needs class C).
#   5. If still failing after 5 rounds, print remaining errors for
#      manual resolution.

CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "bmdx-core" ]; then
    echo "ERROR: Must be on the bmdx-core branch (currently on '$CURRENT_BRANCH')"
    exit 1
fi
```

### 2.2 Uncommitted work-in-progress

`git status` at time of report (files modified vs last commit):

```
M  src/main/java/com/sciome/bmdexpress2/mvp/model/DoseResponseExperiment.java        (+74 lines)
M  src/main/java/com/sciome/bmdexpress2/mvp/model/IStatModelProcessable.java         (+39)
M  src/main/java/com/sciome/bmdexpress2/mvp/model/info/ExperimentDescription.java    (+53)
M  src/main/java/com/sciome/bmdexpress2/mvp/model/info/VocabularyConfig.java         (+21)
M  src/main/java/com/sciome/bmdexpress2/mvp/model/probe/Probe.java                   (+10)
M  src/main/java/com/sciome/bmdexpress2/mvp/model/probe/ProbeResponse.java           (+35)
M  src/main/java/com/sciome/bmdexpress2/util/ExperimentDescriptionParser.java        (+8)
M  src/main/resources/vocabulary.yml                                                 (+26)
?? src/main/java/com/sciome/bmdexpress2/mvp/model/probe/Endpoint.java                (new)
?? src/main/java/com/sciome/bmdexpress2/mvp/model/probe/EndpointResponse.java        (new)
?? src/test/                                                                         (new, moved from src/main/java)
?? docs/                                                                             (new)
```

Total: **252 insertions, 14 deletions** + 2 new interface files.

Theme: **domain-neutral data model**. The `.bm2` format and the surrounding
object graph were originally genomics-only (Probe, ProbeResponse, ChipInfo).
The uncommitted work generalizes these into a domain-agnostic pair of
interfaces while keeping all existing `Probe`/`ProbeResponse` code working.

#### 2.2.1 New interfaces

**`src/main/java/com/sciome/bmdexpress2/mvp/model/probe/Endpoint.java`**

```java
package com.sciome.bmdexpress2.mvp.model.probe;

/**
 * Domain-neutral interface for a measured quantity in a dose-response experiment.
 *
 * In genomics, an endpoint is a gene probe (microarray spot, S1500+ target, etc.).
 * In apical toxicology, an endpoint is a clinical measurement (ALT, body weight,
 * liver weight, etc.).  This interface abstracts over both — any object that has
 * a string identifier can serve as an endpoint.
 */
public interface Endpoint
{
    /**
     * Returns the unique identifier for this endpoint.
     * For genomics: the probe set ID (e.g., "ACAA1A_7954").
     * For apical data: the endpoint name (e.g., "Alanine aminotransferase").
     */
    String getId();
    void setId(String id);
}
```

**`src/main/java/com/sciome/bmdexpress2/mvp/model/probe/EndpointResponse.java`**

```java
package com.sciome.bmdexpress2.mvp.model.probe;

import java.util.List;

/**
 * Domain-neutral interface for a single endpoint's dose-response data.
 *
 * Each EndpointResponse pairs an {@link Endpoint} (what was measured) with
 * a list of response values (one per treatment/animal in the experiment).
 */
public interface EndpointResponse
{
    Endpoint getEndpoint();
    List<Float> getResponses();
    void setResponses(List<Float> responses);
}
```

#### 2.2.2 `Probe` implements `Endpoint`

```java
// Probe.java — diff
-public class Probe implements Serializable
+public class Probe implements Serializable, Endpoint
```

`Probe` already had `getId()` / `setId(String)` so no further changes
were needed — the interface is a pure marker + contract addition.

#### 2.2.3 `ProbeResponse` implements `EndpointResponse`

```java
// ProbeResponse.java — diff
-public class ProbeResponse extends BMDExpressAnalysisRow implements Serializable
+public class ProbeResponse extends BMDExpressAnalysisRow implements Serializable, EndpointResponse

 @Deprecated(forRemoval = false)
 public Probe getProbe() { return probe; }

+@Override
+@JsonIgnore
+public Endpoint getEndpoint() { return probe; }
```

The `@Deprecated(forRemoval = false)` pattern signals *"prefer the new API
but we're not removing this"* — lets genomics code keep using `getProbe()`
without IDE warnings promising deletion.

#### 2.2.4 `DoseResponseExperiment` gets endpoint aliases

```java
// DoseResponseExperiment.java — diff
+@JsonAlias("endpointResponses")
 private List<ProbeResponse> probeResponses;

+@Deprecated(forRemoval = false)
 public List<ProbeResponse> getProbeResponses() { return probeResponses; }

+@Deprecated(forRemoval = false)
 public void setProbeResponses(List<ProbeResponse> probeResponses) {
     this.probeResponses = probeResponses;
 }

+/**
+ * Domain-neutral alias for {@link #getProbeResponses()}.
+ *
+ * Returns the dose-response matrix as a list of {@link EndpointResponse}
+ * objects.  Each entry is one measured endpoint (gene probe, clinical
+ * measurement, organ weight, etc.) with its response values across all
+ * treatments.
+ */
+@JsonIgnore
+public List<? extends EndpointResponse> getEndpointResponses() {
+    return probeResponses;
+}
+
+@JsonIgnore
+public void setEndpointResponses(List<ProbeResponse> endpointResponses) {
+    this.probeResponses = endpointResponses;
+}
```

Key detail: **`@JsonAlias("endpointResponses")`** on the backing field.
This means Jackson can deserialize `.bm2` files that use *either* the
legacy `"probeResponses"` key *or* the new `"endpointResponses"` key,
letting new domain-neutral writers emit the new name without breaking
old readers.

Similar changes for `ChipInfo getChip()` → `getPlatform()` aliases.

#### 2.2.5 `IStatModelProcessable` — default method for the alias

```java
// IStatModelProcessable.java — diff
+/**
+ * Contract for any dataset that can be sent to BMD statistical model fitting.
+ * Provides both genomics-named and domain-neutral method names for compatibility.
+ */
 public interface IStatModelProcessable
 {
     DoseResponseExperiment getProcessableDoseResponseExperiment();

+    @Deprecated(forRemoval = false)
     List<ProbeResponse> getProcessableProbeResponses();

+    /**
+     * @JsonIgnore because this returns the same list as getProcessableProbeResponses() —
+     * Jackson would serialize it as redundant @ref integer arrays, cluttering the JSON tree.
+     */
+    @JsonIgnore
+    default List<? extends EndpointResponse> getProcessableEndpointResponses() {
+        return getProcessableProbeResponses();
+    }

     String getParentDataSetName();
     String getDataSetName();
 }
```

All existing implementers (`DoseResponseExperiment`, `OneWayANOVAResults`,
`WilliamsTrendResults`, `OriogenResults`, `CurveFitPrefilterResults`,
`BMDResult`) get the new default for free — no edits needed.

#### 2.2.6 `dataType` field on `ExperimentDescription`

New field for data-completeness classification:

```java
// ExperimentDescription.java — diff
+// Data completeness classification: "tox_study" (raw, may have gaps),
+// "inferred" (gap-filled for BMD modeling), or "gene_expression".
+// Nullable for backward compatibility: old .bm2 files won't have this field.
+private String dataType;

+public static List<String> getDataTypeVocabulary() {
+    return VocabularyConfig.getInstance().getDataTypes();
+}

+public String getDataType() { return dataType; }
+public void setDataType(String dataType) { this.dataType = dataType; }
```

And in `vocabulary.yml`:

```yaml
# Data completeness classification.  Describes whether a file/experiment
# contains raw tox study data (may have gaps from dead/excluded animals)
# or gap-filled data suitable for BMD modeling.
#   tox_study       — actual experimental values, may have missing data
#   inferred        — gap-filled (dose-group averages substituted for missing)
#   gene_expression — transcriptomics data (always complete, different pipeline)
dataTypes:
  - tox_study
  - inferred
  - gene_expression
```

#### 2.2.7 vocabulary.yml — apical & observations support

Providers list grew:

```yaml
 providers:
+  # Genomics providers — commercial entities or reference databases
   - Affymetrix
   - Agilent
   - BioSpyder
   - RefSeq
   - Ensembl
-  - Clinical Endpoint
+  # Apical provider — in vivo tox study numeric dose-response endpoints
+  - Apical
+  # Observations provider — categorical event data (clinical observations)
+  - Observations
   - Generic
```

Platforms list grew:

```yaml
 platforms:
   ...
-  # Clinical endpoints
+  # Apical platforms — each is a class of in vivo tox study numeric endpoints
+  - Body Weight
+  - Organ Weight
   - Clinical Chemistry
   - Hematology
-  - Organ Weight
+  - Hormones
+  - Tissue Concentration
+  # Observations platforms — categorical event data
+  - Clinical
   - Generic
```

#### 2.2.8 Jackson @JsonIgnore audit on `ExperimentDescription`

Multiple computed / formatting methods now carry `@JsonIgnore` to prevent
Jackson from serializing them as fake properties:

```java
 @JsonIgnore
 public List<String> getColumnHeaders() { ... }

 @JsonIgnore
 public List<Object> getColumnValues() { ... }

 @JsonIgnore
 public String getExperimentType() { ... }

 @JsonIgnore
 public boolean isInVivo() { ... }

 @JsonIgnore
 public boolean isInVitro() { ... }

 @JsonIgnore
 public String getStatusBarString() { ... }

 @JsonIgnore
 public String getFormattedString() { ... }
```

This cleans up the `.bm2` JSON output — no more `"columnHeaders": [...]`,
`"inVivo": true`, etc. polluting the serialized model.

#### 2.2.9 `ExperimentDescriptionParser` — data type parsing

```java
 private static final String[] PROVIDER_KEYS = {"provider"};
+private static final String[] DATA_TYPE_KEYS = {"data type", "datatype", "data_type"};

 ...

+// Parse data type classification — tox_study, inferred, or gene_expression.
+// Validated against the dataTypes vocabulary from vocabulary.yml.
+String dataType = parseVocabularyField(metadata, issues, "Data Type",
+    ExperimentDescription.getDataTypeVocabulary(),
+    desc::setDataType,
+    DATA_TYPE_KEYS);
```

#### 2.2.10 Tests

`src/test/java/com/sciome/bmdexpress2/model/Bm2DeserializationTest.java`
moved from `src/main/java/...` (where it was awkwardly sitting) to
`src/test/java/...` — the standard Maven location.

#### 2.2.11 Docs directory

`docs/domain-agnostic-refactoring.html` (not detailed here) — design notes
for the Endpoint/EndpointResponse refactor, written earlier in this session.

### 2.3 Conversation glue — `bmdx-core`

The current conversation (`9f7fce81.jsonl`) began with an LLM-metadata
deduction plan and branched into UI work on the dialogs. During that session
the user asked to verify branch state, leading to this report.

Earlier Claude conversations that touched `bmdx-core` themes:

- **`3e7848b3.jsonl`** — *"i want to develop a new data type for bmd express.
  use the rlm-code mcp service to research the code's object model"*.
  This is the research phase that informed the apical / observations
  extensions now landing in the uncommitted diff.

- **`af484666.jsonl`** — *"branch the repo. llm-metadata"* and *"use llm to
  deduce metadata from combination of filename, category analysis names"* —
  spawned the separate `llm-metadata` branch where the LLM deduction logic
  was originally prototyped.

- **`9d30fa37.jsonl`** — integrated `ExperimentDescription` into the object
  model. Its work is in the `experiment-metadata` branch (commit `0767a73`)
  but the refactors it set up are what made the bmdx-core strip feasible —
  the metadata system was self-contained enough to carry forward without
  the analysis engines.

---

## Part 3 — Dependency additions

Tracked via `pom.xml` across the two branches.

### In `experiment-metadata`:

| Dep | Version | Purpose | Commit |
| --- | --- | --- | --- |
| `org.yaml:snakeyaml` | 2.0 | Load `vocabulary.yml` | `024eba5` |
| `org.jeasy:easy-rules-core` | 4.1.0 | Metadata validation engine | `995d99c` |
| `org.jeasy:easy-rules-support` | 4.1.0 | YAML-defined rules support | `995d99c` |
| `org.testfx:testfx-core` | 4.0.16-alpha | Visual GUI test runner | `f0e7e1c` |
| `org.testfx:testfx-junit5` | 4.0.16-alpha | JUnit 5 bindings | `f0e7e1c` |
| (SLF4J / logback) | existing | Validator logging | `995d99c` |

### In `bmdx-core`:

- No new deps. The strip commit actually keeps the full `pom.xml` intact
  — the library still needs JavaFX for the UI model classes (ObservableList
  etc. are used pervasively), Jackson, SnakeYAML, easy-rules.

---

## Part 4 — How the branches relate

```
main  ─────────────────────────────────────────────────────────────►
              │
              │ (branch)
              ▼
experiment-metadata  ─┬── metadata model ────────────────►
                      │
                      │    +vocabulary.yml
                      │    +validation rules
                      │    +dialogs
                      │    +column propagation
                      │    +TestFX runner
                      │
                      └── (branch)
                          │
                          ▼
                     bmdx-core  ──┬── strip GUI/engines (committed)
                                  │
                                  ├── Endpoint/EndpointResponse (WIP)
                                  ├── dataType vocabulary (WIP)
                                  ├── apical/observations platforms (WIP)
                                  └── @JsonIgnore audit (WIP)
```

`bmdx-core` is **strictly downstream** of `experiment-metadata`. The
sync-from-upstream.sh script codifies the intent that any further additions
to `experiment-metadata` should flow down into `bmdx-core` with minimal
friction.

Other related branches (not in scope for this report):
- **`llm-metadata`** — LLM-based metadata deduction, spawned from
  `experiment-metadata` (commit `af484666` conversation).
- **`origin/pvalue_passthru`, `origin/duckdb`, `origin/zscore_newtoxicr`**,
  etc. — unrelated upstream feature branches.

---

## Part 5 — Statistics

### `experiment-metadata` vs `main`

| Category | Files | Insertions | Deletions |
| --- | --- | --- | --- |
| Model / metadata | 18 | ~3,200 | ~20 |
| UI dialogs | 3 | ~700 | ~10 |
| Parser | 1 | 779 | 0 |
| Rules engine | 9 | ~1,200 | 0 |
| Vocabulary YAML | 1 | 151 | 0 |
| TestFX runner | 11 | ~1,000 | 0 |
| pom.xml + module-info | 2 | ~50 | 0 |
| Tests | 2 | ~380 | 0 |
| **Total** | **54** | **~5,900** | **~50** |

### `bmdx-core` vs `experiment-metadata`

| Category | Files | Insertions | Deletions |
| --- | --- | --- | --- |
| Committed (strip) | 501 | 238 | 93,674 |
| Uncommitted (domain-neutral refactor + apical vocab + dataType) | 11 | ~252 | ~14 |
| **Total (commit + WIP)** | **~512** | **~490** | **~93,688** |

---

## Part 6 — Outstanding items

On `bmdx-core`, these items are still uncommitted:
1. `Endpoint.java` / `EndpointResponse.java` — new interfaces
2. `Probe implements Endpoint`, `ProbeResponse implements EndpointResponse`
3. `getEndpointResponses()` aliases on `DoseResponseExperiment` + `@JsonAlias`
4. `IStatModelProcessable.getProcessableEndpointResponses()` default method
5. `dataType` field on `ExperimentDescription`
6. `vocabulary.yml` apical + observations + dataTypes extensions
7. `@JsonIgnore` audit on `ExperimentDescription` computed methods
8. `ExperimentDescriptionParser` — data type key parsing
9. Move `Bm2DeserializationTest` from `src/main/` to `src/test/`
10. `docs/domain-agnostic-refactoring.html`

These form a coherent next commit: *"Introduce domain-neutral Endpoint
interfaces and apical-data vocabulary support"*.

---

## Appendix — Report methodology

This report was generated by inspecting:
- `git log --oneline main..experiment-metadata` (18 commits)
- `git log --oneline experiment-metadata..bmdx-core` (1 commit)
- `git log --stat` for per-file byte-level impact per commit
- `git diff --stat main...experiment-metadata` and
  `git diff --stat experiment-metadata...bmdx-core`
- `git status` and `git diff HEAD` for uncommitted bmdx-core WIP
- Source reads of the key files cited above
- Claude Code transcripts in
  `~/.claude/projects/-home-svobodadl-Dev-Projects-BMDExpress-3/*.jsonl`
  (8 transcripts, ~21MB total) for user-intent context

The 8 transcripts map roughly to the branch arc:

| Transcript | Date | Primary theme |
| --- | --- | --- |
| `c1e98253` (5.5MB) | Mar 3 | Branch review, pattern alignment |
| `4965bbe9` (2.0MB) | Feb 26 | Fix misalignments with main |
| `7cd3506f` (2.6MB) | Feb 26 | TestFX visual runner plan |
| `3e7848b3` (1.8MB) | Mar 3 | New data type research (apical) |
| `9d30fa37` (4.5MB) | Mar 3 | First-class ExperimentDescription integration |
| `af484666` (1.5MB) | Mar 3 | Import robustness + `llm-metadata` branch |
| `9f7fce81` (3.9MB) | Apr 17 | LLM metadata deduction UI (current session) |
| `5d856aac` (1KB)   | Mar 3 | (empty transcript) |
