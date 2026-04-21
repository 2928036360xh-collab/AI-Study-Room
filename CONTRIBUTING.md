# Contributing Guide

## Before you open a PR

- Make sure the project builds in Android Studio.
- Do not commit `local.properties`, signing files, or personal test data.
- Avoid adding large model files directly to the repository.
- Keep changes focused and avoid unrelated reformatting.

## Development expectations

- New business logic should use Kotlin.
- UI should follow the existing XML-based Android view approach.
- Keep Activity / Fragment logic thin when possible.
- Prefer updating existing patterns over introducing a new stack.

## Pull request checklist

- Describe the user-facing change.
- Mention any permissions, assets, or schema changes.
- Include screenshots for UI changes when applicable.
- Confirm that no private data or secrets are included.

## Privacy and assets

- Do not include student information, camera captures, exported reports, or personal datasets.
- If a model file is larger than typical Git hosting limits, use Git LFS or runtime download instead of committing it directly.