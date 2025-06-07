# thomas-cup-kafka

## Features & Tasks Implemented

- **.gitignore**: Added a suitable .gitignore for a Spring Java Maven project, ignoring IDE files, build output, logs, and OS-specific files.
- **Cleanup**: Removed previously committed files that are now ignored (e.g., .idea/, target/), and committed these changes.
- **Idempotency**: Implemented idempotency in the Kafka producer using a combination of match id and game number, ensuring each game in a match is tracked and updated independently.
- **Consumer Groups & Partitions**: Added a Kafka consumer with multiple consumer groups to demonstrate how messages are distributed across groups and partitions.
- **Badminton Rules Enforcement**: 
  - Scores must be between 0 and 30.
  - A match consists of the best of 3 games.
  - Games 1 and 2 are played to 21 points (cap at 30).
  - Game 3 is played to 15 points (cap at 30).
  - Each game starts at 0-0.
- **Validation**: Added validation logic in the MatchResult model to enforce the above badminton rules.
- **Tests**: Updated and added tests to:
  - Cover idempotency logic (including match id and game number).
  - Ensure correct logging for consumer groups and partitions.
  - Validate badminton scoring rules.
- **Jackson Compatibility**: Ensured the MatchResult model is compatible with JSON serialization/deserialization for Spring controllers.
- **Git Operations**: Committed and pushed all changes to the remote repository.
- **Database Consumer**: Added a dedicated consumer (MatchResultDbConsumer) that listens to two separate Kafka topics:
  - `new-game`: Inserts a new game into the PostgreSQL match_results table (with ON CONFLICT DO NOTHING).
  - `update-score`: Updates the score, winner, and matchDateTime for an existing game in the match_results table.
  - Uses PostgreSQL via Docker (see below for setup).
  - added swagger documentation for the API endpoints.

## How to Run

1. Build the project with Maven:
   ```sh
   mvn clean install
   ```
2. Run the Spring Boot application:
   ```sh
   mvn spring-boot:run
   ```
3. Run tests:
   ```sh
   mvn test
   ```

## PostgreSQL & Docker Setup

1. Add the following service to your `docker-compose.yml`:
   ```yaml
   postgres:
     image: postgres:15
     environment:
       POSTGRES_DB: thomas_cup
       POSTGRES_USER: thomas  # for now we can leave it here (fun project)
       POSTGRES_PASSWORD: thomas # until we move to a more secure setup 
     ports:
       - "5432:5432"
     volumes:
       - pgdata:/var/lib/postgresql/data
   volumes:
     pgdata:
   ```
2. Start PostgreSQL:
   ```sh
   docker compose up -d
   ```
3. Create the table:
   ```sql
   CREATE TABLE match_results (
     id VARCHAR NOT NULL,
     teamA VARCHAR,
     teamB VARCHAR,
     teamAScore INT,
     teamBScore INT,
     winner VARCHAR,
     matchDateTime TIMESTAMP,
     gameNumber INT,
     PRIMARY KEY (id, gameNumber)
   );
   ```
4. Configure your `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/thomas_cup
   spring.datasource.username=thomas
   spring.datasource.password=thomas
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.jpa.hibernate.ddl-auto=none
   ```

## Notes
- Ensure your Kafka topic `thomas-cup-matches` has multiple partitions to see consumer group/partition behavior.
- Use the API endpoint `/api/match-results` to send match results as JSON.
- The project enforces badminton match rules and idempotency at the producer level.
