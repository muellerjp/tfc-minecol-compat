# Contributing

## Getting started

1. Fork the repository and create a branch from `main`.
2. Make sure both **MineColonies** and **TerraFirmaCraft** jars are available as
   local Maven artifacts or fill in their coordinates in `build.gradle` before
   compiling.
3. Open the project in IntelliJ IDEA and import the Gradle project.

## Pull requests

- Keep the scope of a PR small — one fix or one feature per PR.
- Describe **why** the change is needed in the PR description, not just what it does.
- If you're fixing a bug, reference the issue number in the PR description
  (`Closes #N`).

## Code style

- Follow the existing Java formatting — 4-space indentation, opening braces on
  the same line.
- No trailing whitespace, LF line endings.
- Do not add comments that only restate what the code already says.

## Reporting bugs

Please use the GitHub issue tracker. Include:
- Mod versions for MineColonies, TFC, and this compat mod.
- A short description of the unexpected behaviour.
- Relevant log excerpts (crash report or `latest.log`).
