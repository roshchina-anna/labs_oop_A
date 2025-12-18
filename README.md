# labs_oop_A

## Database configuration
Database connection parameters are resolved in the following order:
1. Environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
2. `src/main/resources/database.properties` loaded from the classpath.
3. Built-in defaults (`jdbc:postgresql://localhost:5432/math_functions_db`, `postgres`/`postgres`).

If you run the application in Docker, provide the environment variables to override the bundled properties, for example:

```bash
docker run \
  -e DB_URL="jdbc:postgresql://db:5432/functions_db" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="postgres" \
  -p 8080:8080 your-image:tag
```

When running locally, adjust `src/main/resources/database.properties` to point to your PostgreSQL instance or rely on the defaults above.