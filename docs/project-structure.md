# Project Structure

## Core directories

`app/src/main/java/com/example/end_side/`
- Main Android application code.
- `data/`: Room entities, DAO, repository, and persistence models.
- `engine/`: active AI analysis pipeline and analyzers. The current implementation uses ML Kit.
- `service/`: foreground/background study monitoring services.
- `ui/`: activities, fragments, view models, and custom views.
- `utils/`: shared helper utilities.

`app/src/main/res/`
- XML layouts, drawables, themes, colors, strings, and other Android resources.

`app/src/main/assets/`
- Runtime assets still used by the app, such as local help content.

`app/src/main/jniLibs/`
- Native libraries that are still part of the active build.

`app/archive/paddle-lite/`
- Archived Paddle Lite implementation.
- This folder is intentionally outside active source sets so it does not participate in build or packaging.
- Suitable to delete as a whole when preparing an open-source version.

`gradle/`
- Version catalog, wrapper config, and Gradle infrastructure.

`.github/`
- Copilot and repository automation instructions.

## Current AI implementation split

`app/src/main/java/com/example/end_side/engine/StudyAnalysisPipeline.kt`
- Active on-device analysis entry point.
- Uses ML Kit pose and face detection.

`app/archive/paddle-lite/`
- Legacy Paddle Lite implementation retained only for reference.