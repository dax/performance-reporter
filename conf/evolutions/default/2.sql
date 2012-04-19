# --- !Ups

CREATE SEQUENCE system_id_seq;
CREATE TABLE system (
  id integer NOT NULL DEFAULT nextval('system_id_seq'),
  label varchar(255)
);

ALTER TABLE run ADD system_id integer NOT NULL;
ALTER TABLE run ADD FOREIGN KEY(system_id) REFERENCES system(id) ON DELETE CASCADE;

# --- !Downs

DROP TABLE system;
DROP SEQUENCE system_id_seq;
