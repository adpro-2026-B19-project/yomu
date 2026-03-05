# Staging Change Notes

## Purpose
This file records what was added on `staging` so team members can quickly sync environment, run the app, and understand current base behavior.

## Infrastructure And Runtime
- Added root `.dockerignore` to keep build context small.
- Added root `DOCKERFILE` for multi-stage Java build and runtime image.
- Added root `compose.yml` for local container orchestration.
- Added Spring docker profile: `src/main/resources/application-docker.properties`.
- Set docker profile to use H2 file database at `/app/data/yomu-db`.

## Local App Configuration
- Updated default datasource URL in `src/main/resources/application.properties` to:
  - `jdbc:h2:file:./data/yomu-db-v2;DB_CLOSE_ON_EXIT=FALSE`
- This avoids old schema conflicts from previous local H2 files.

## Team Workflow Docs
- Added `README.md` with commands for:
  - local test/run
  - docker compose up/logs/down/down -v

## Web Entry Route
- Landing page template is now `src/main/resources/templates/landingPage.html`.
- Base route `/` is mapped in `TemplateController` to `landingPage`.
- Landing page navigation links:
  - Login button -> `/auth/login`
  - Register button -> `/auth/register`

## Branch Sync Expectation
- After staging updates, merge `staging` into all `auth/*` branches and push each branch.

## Commit Discipline
- For auth and template work, make one focused fix per commit.
- Avoid bundling unrelated refactors or bugfixes into a single commit.
