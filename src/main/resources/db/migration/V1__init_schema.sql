-- Thomas Cup Kafka - Initial Schema Creation
-- This migration assumes database and user already exist
-- For production deployments, ensure thomas_cup_dev database and thomas_cup_user exist first

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