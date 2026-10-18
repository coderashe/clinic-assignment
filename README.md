# Community Clinic Appointment & Queue Management System

Spring Boot 3.5 / Java 25 / MySQL 8 / Thymeleaf + Bootstrap 5.

## Run
1. Install JDK 25 and MySQL 8. MySQL must be reachable; the `clinic` database is created automatically.
2. Set credentials (defaults: root/root): `export DB_USER=... DB_PASSWORD=... [DB_HOST=localhost DB_NAME=clinic]`
3. `mvn spring-boot:run` then open http://localhost:8080
4. Hibernate creates the tables on first start (`ddl-auto: update`).

## Demo accounts (seeded on an empty DB; password from `clinic.seed.password`, default `Clinic#2026`)
| user | role |
|---|---|
| reception | RECEPTIONIST |
| dr.moyo, dr.ncube | CLINICIAN |
| manager | MANAGER |

Change the password and set `clinic.seed.enabled=false` before real use.

## Design notes
- **No double booking:** each booking/reschedule/check-in/call-next first takes a `PESSIMISTIC_WRITE` lock on the clinician row, then runs the overlap query (`start < N_end AND end > N_start`, also `PESSIMISTIC_WRITE`). Concurrent requests for one clinician are serialised; `@Version` on all entities is a second safety net.
- **Slots:** durations, clinic hours and default length are in `application.yml` (`clinic.slot.*`). Start times must sit on a slot boundary (e.g. 30-min slots start at :00/:30) and in the future.
- **Queue:** order = priority tier, then check-in time. EWT = (waiting patients ahead *in the same tier*) x (clinician's mean historical visit length; `clinic.queue.default-visit-minutes` until history exists). A `@Scheduled` sweep every 60s fans out one virtual thread per clinician queue, guarded by a `ReentrantLock`.
- **Field-level security:** clinicians/managers only ever see masked National ID and phone; address is receptionist-only.
- **Notifications:** `MockSms/MockEmailNotificationService` log on virtual threads after the booking transaction commits (phone numbers are masked in logs).
