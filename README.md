# labs_oop_A

## Database configuration
Database connection parameters are resolved in the following order:
1. Environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
2. `src/main/resources/database.properties` loaded from the classpath.
3. Built-in defaults (`jdbc:postgresql://localhost:5432/math_functions_db`, `postgres`/`postgres`).

If you run the application in Docker, provide the environment variables to override the bundled properties, for example:

```bash
docker run \
  -e DB_URL="jdbc:postgresql://db:5432/math_functions_db" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="postgres" \
  -p 8080:8080 your-image:tag
```

When running locally, adjust `src/main/resources/database.properties` to point to your PostgreSQL instance or rely on the defaults above.

## Running the web UI

Build the WAR with Maven (tests are not required to start the UI):

```bash
mvn -DskipTests clean package
```

Then choose one of the following options:

* **Run in Docker (recommended for local demos):**
    1. Build the image after the WAR is created: `docker build -t labs-oop-ui .`
    2. Start the container, passing your database settings if needed:

       ```bash
       docker run --rm -p 8080:8080 \
         -e DB_URL="jdbc:postgresql://localhost:5432/math_functions_db" \
         -e DB_USERNAME=postgres \
         -e DB_PASSWORD=postgres \
         labs-oop-ui
       ```

    3. Open the web interface at [http://localhost:8080](http://localhost:8080).

* **Deploy to an existing Tomcat 10 instance:**
    1. Copy `target/ROOT.war` into Tomcat's `webapps` directory.
    2. Start Tomcat and open [http://localhost:8080](http://localhost:8080).

The WAR is packaged as `ROOT.war`, so it is served from the root context (`/`).

### If you see a Jasper error saying "Не указана выходная папка"

Tomcat's default error pages rely on JSP compilation, which can fail with that
message when the container cannot write to its temporary directory. This
application ships a static error page (`/error/500.html`) to bypass Jasper. Make
sure you deploy a freshly built WAR and that Tomcat can write to `java.io.tmpdir`
or to the `workDir` configured for the context.