# --- !Ups

CREATE SEQUENCE run_id_seq;
CREATE TABLE run (
  id integer NOT NULL DEFAULT  nextval('run_id_seq'),
  label varchar(255)
);

# --- !Downs

DROP TABLE run;
DROP SEQUENCE run_id_seq;
