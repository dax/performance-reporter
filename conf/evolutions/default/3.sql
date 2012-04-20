# --- !Ups

ALTER TABLE run ADD values text NOT NULL;

# --- !Downs

ALTER TABLE run DROP COLUMN values;
