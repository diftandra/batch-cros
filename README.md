# CROS Batch (CrosbatchApplication)

Monthly Spring Batch job that loads the CROS text files into PostgreSQL, then moves each loaded file to a backup folder.
Spring Boot 4.1, Spring Batch 6, Java 17+, Spring Data JPA (Hibernate 7), PostgreSQL.

## Job design

Job `crosMonthlyJob`, job parameters `period` (e.g. `112025`) and `run.time`. One flow per file, in the order of
`crosbatch.files`:

```
<TYPE>.prepare   tasklet  file in input?  no + in backup -> SKIPPED (next file)
                                          no + not in backup -> FAILED
                                          SNAPSHOT: delete rows of this period
<TYPE>.load      chunk    FlatFileItemReader -> CrosItemProcessor -> JpaItemWriter / UpsertItemWriter
<TYPE>.finalize  tasklet  fail if the file had no rows; UPSERT: is_active=false for keys not in the file
<TYPE>.backup    tasklet  move file to <backup-dir>/<period>/<name>_<timestamp>.txt
```

The job metadata (BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION, BATCH_STEP_EXECUTION, ...) is stored in PostgreSQL.

## Project structure

```
src/main/java/id/co/bca/crosbatch
├── CrosbatchApplication.java       main: CLI mode runs the job once and exits; API mode stays up
├── api/                            REST controller, API key filter, async task executor (API mode only)
├── batch/
│   ├── CrosJobService              start / find / list / recover, shared by CLI and API
│   ├── JobSummaryListener          per-step summary in the log when the job ends
│   ├── CrosJobConfig               Job + flows + steps
│   ├── CrosFileItemReader          FlatFileItemReader ("~|~", header row = column names)
│   ├── CrosItemProcessor           Row -> entity, sets periode / is_active, filters blank lines
│   ├── UpsertItemWriter            composite-key match: update or insert
│   ├── PrepareFileTasklet          skip / missing check, snapshot delete
│   ├── FinalizeFileTasklet         empty-file check, deactivate
│   ├── BackupFileTasklet           move file to backup
│   ├── ProgressLogListener         progress line every N rows
│   ├── PeriodDetector              period from TGL_DATA of the input files
│   └── CrosFileLocator             period -> file name, periode date
├── config/CrosBatchProperties      all settings (crosbatch.* in application.yml)
├── entity/                         JPA entities, one table per file, UUID id
├── file/                           CrosLineMapper, Row, CrosFileDefinitions (column mapping + keys)
└── service/FileBackupService
```

## Load rules

| File | Mode | Composite key |
|---|---|---|
| CROS_MASTER_DEBITUR | UPSERT | cust_ln_curr_key, kategori_debitur, kode_kanwil, kode_kcu, kategori_kredit |
| CROS_MASTER_PINJAMAN | UPSERT | cust_ln_curr_key, kode_kanwil, kode_kcu, no_rekening_pinjaman, no_pinjaman |
| CROS_MASTER_KOMITMEN | UPSERT | cust_ln_curr_key, kode_kanwil, kode_kcu, no_rekening_pinjaman, no_komitmen |
| CROS_JAMINAN_DEBITUR | UPSERT | cust_ln_curr_key, coll_curr_key |
| CROS_NPL, CROS_PEMEGANG_SAHAM, CROS_REGION_DIM, CROS_BRANCH_DIM, CROS_RELASI_JAMINAN_FASILITAS | SNAPSHOT | – |

The keys were extended after checking the 112025 files; with the original keys 28,544 rows shared a key with
another row and were merged. With these keys only 1 row in PINJAMAN and 1 in KOMITMEN is a duplicate in the source
(logged as `duplicateKeysInFile`, last row kept).

- **UPSERT**: key found -> row updated with the new values, `periode` = current period, `is_active = true`.
  Key not found -> inserted. Then every active row that this job execution did not load (its key was not in the
  file) is set to `is_active = false`.
- **SNAPSHOT**: rows of the current period are deleted, then the file is inserted. Older periods are kept.
- `periode` is the first day of the period month (112025 -> `2025-11-01`).
- Dates: `dd-MM-yy` (`30-11-25`) and `dd-MMM-yy` (`25-OCT-16`, used in BRANCH_DIM); two-digit years map to
  1950–2049 (`18-06-86` -> 1986-06-18). Configurable with `crosbatch.date-patterns`.

## Input files and period

The files arrive without period in the name: `CROS_MASTER_DEBITUR.txt`, `CROS_NPL.txt`, ... in the input folder
(pattern `{prefix}.txt`, setting `CROSBATCH_FILE_NAME_PATTERN`; use `{prefix}_{period}.txt` if names include it).

A scheduled run needs no parameter. The period is chosen like this:

1. `--crosbatch.period=MMyyyy` / API body `{"period":"112025"}` if given. It must match TGL_DATA of the files.
2. Otherwise from `TGL_DATA` of the input files (`30-11-25` -> 112025).
3. Otherwise (input folder has no CROS files) the previous month, so a run after everything was loaded reports
   the files as already processed.

The run stops with an error when the files belong to different months, when a row's TGL_DATA is outside the
period, or when the only input files have no TGL_DATA (REGION_DIM, BRANCH_DIM, NPL); then give the period explicitly.

## Rerun behaviour

- Any error stops the job with status FAILED and exit code 1. The log shows the file, line number and value.
- A file is moved to backup only after its database work is committed. Running the same period again skips files
  already in backup and loads every file still in the input folder again from the first line
  (SNAPSHOT deletes the period first, UPSERT matches the same keys), so partial data from the failed run is replaced.
- Each launch is a new job instance, so a period can be run as often as needed.
- If a run was killed (execution still marked STARTED), the next run stops with a message. When you are sure no other
  run is active, start once with `--crosbatch.recover-stale-executions=true`.

## Build

```bash
./mvnw clean package          # -> target/crosbatch.jar
```

## Run

Create the database once (tables, indexes and Spring Batch tables are created on first run):

```sql
CREATE DATABASE cros;
```

```bash
export CROSBATCH_DB_URL='jdbc:postgresql://dbhost:5432/cros?reWriteBatchedInserts=true'
export CROSBATCH_DB_USERNAME=cros
export CROSBATCH_DB_PASSWORD='***'
export CROSBATCH_INPUT_DIR=/data/cros/input
export CROSBATCH_BACKUP_DIR=/data/cros/backup
export CROSBATCH_LOG_FILE=/data/cros/logs/crosbatch.log

java -Xmx1g -jar crosbatch.jar                          # period from TGL_DATA (scheduled run)
java -Xmx1g -jar crosbatch.jar --crosbatch.period=112025  # explicit period (MMyyyy)
```

Any value in `application.yml` can be overridden by an environment variable, a `--crosbatch.xxx=` argument,
or an external file: `--spring.config.additional-location=/opt/crosbatch/application.yml`.

| Property | Env var | Default |
|---|---|---|
| crosbatch.period | CROSBATCH_PERIOD | empty = from TGL_DATA, see "Input files and period" |
| crosbatch.file-name-pattern | CROSBATCH_FILE_NAME_PATTERN | `{prefix}.txt` |
| crosbatch.input-dir | CROSBATCH_INPUT_DIR | ./data/input |
| crosbatch.backup-dir | CROSBATCH_BACKUP_DIR | ./data/backup |
| crosbatch.charset | CROSBATCH_CHARSET | UTF-8 |
| crosbatch.batch-size (chunk size) | CROSBATCH_BATCH_SIZE | 1000 |
| crosbatch.files[n].enabled | – | true |
| crosbatch.recover-stale-executions | – | false |
| spring.jpa.hibernate.ddl-auto | CROSBATCH_DDL_AUTO | update |
| spring.batch.jdbc.initialize-schema | CROSBATCH_BATCH_SCHEMA_INIT | always |
| logging.file.name | CROSBATCH_LOG_FILE | ./logs/crosbatch.log (daily rolling, gzip, 90 days) |

## API mode

The same jar can run as a small web service that starts the job on request:

```bash
export CROSBATCH_API_KEY='a-long-random-secret'   # required, the app will not start without it
java -Xmx1g -jar crosbatch.jar --spring.profiles.active=api
```

Settings in `application-api.yml`: port `CROSBATCH_API_PORT` (8080), listen address `CROSBATCH_API_ADDRESS`
(`127.0.0.1` by default; use `0.0.0.0` to accept calls from other machines). Every call needs the `X-API-KEY` header.

| Method | Path | Result |
|---|---|---|
| POST | `/api/cros-batch/executions` body `{"period":"112025"}` (body optional = period from TGL_DATA) | 202 + execution, job runs in the background |
| GET | `/api/cros-batch/executions/{id}` | status, per-step counts, failure messages |
| GET | `/api/cros-batch/executions?limit=20` | latest executions, newest first |
| POST | `/api/cros-batch/executions/{id}/recover` | marks a run left STARTED by a killed process as FAILED |

Errors: 400 bad period / missing input folder, 401 missing or wrong key, 404 unknown id,
409 a run is still in progress (or the run to recover is alive in this process).

```bash
K='X-API-KEY: a-long-random-secret'
curl -X POST localhost:8080/api/cros-batch/executions -H "$K" -H 'Content-Type: application/json' -d '{"period":"112025"}'
curl localhost:8080/api/cros-batch/executions/1 -H "$K"
```

Only one run at a time is allowed. The check uses the Spring Batch tables, so it also blocks an API run while a
CLI run is active (and the other way round).

### Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`. Click **Authorize**, enter the API key, then use "Try it out".
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Switch both off with `CROSBATCH_SWAGGER_ENABLED=false` (for example in production).

### Health check

No API key needed, so load balancers and monitoring can call these:

| URL | Meaning |
|---|---|
| `/actuator/health` | Overall status and the status of each component (`db`, `diskSpace`, `crosBatch`, ...) |
| `/actuator/health/liveness` | The process is alive. Use for restart decisions |
| `/actuator/health/readiness` | Ready to run a job: database reachable, input folder readable, backup folder writable |

`UP` returns HTTP 200, `DOWN` returns 503. A failed last run does **not** make the service DOWN; it is shown as a
detail. Details (folder paths, last execution, database type) are hidden by default; set
`CROSBATCH_HEALTH_SHOW_DETAILS=always` to show them. Only the `health` actuator endpoint is exposed.

## Schedule monthly (cron example, 1st of the month 02:00)

```cron
0 2 1 * * . /opt/crosbatch/env.sh && java -Xmx1g -jar /opt/crosbatch/crosbatch.jar >> /data/cros/logs/cron.out 2>&1
```

## Try it with the sample files

Sample files are not in the repository because they can contain customer data (`samples/` is in `.gitignore`).
Put your own files in `samples/input`, named like `CROS_MASTER_DEBITUR.txt`, with a header line and
`~|~` as the delimiter, then:

```bash
mkdir -p data/input && cp samples/input/* data/input/
java -jar target/crosbatch.jar
```
