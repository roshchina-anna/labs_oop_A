CREATE TABLE functions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    expression TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);