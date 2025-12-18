CREATE TABLE points (
    function_id INTEGER NOT NULL,
    x_value DOUBLE PRECISION NOT NULL,
    y_value DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (function_id, x_value),
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE
);
