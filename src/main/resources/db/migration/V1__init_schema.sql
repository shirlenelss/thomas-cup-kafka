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