# --- !Ups

ALTER TABLE run DROP COLUMN values;

CREATE SEQUENCE metric_id_seq;
CREATE TABLE metric (
  id integer NOT NULL DEFAULT nextval('metric_id_seq'),
  label varchar(255),
  system_id integer NOT NULL
);
ALTER TABLE metric ADD FOREIGN KEY(system_id) REFERENCES system(id) ON DELETE CASCADE;

CREATE SEQUENCE metric_value_id_seq;
CREATE TABLE metric_value (
  id integer NOT NULL DEFAULT nextval('metric_value_id_seq'),
  datetime datetime NOT NULL,
  value integer NOT NULL,
  metric_id integer NOT NULL,
  run_id integer NOT NULL
);
ALTER TABLE metric_value ADD FOREIGN KEY(metric_id) REFERENCES metric(id) ON DELETE CASCADE;
ALTER TABLE metric_value ADD FOREIGN KEY(run_id) REFERENCES run(id) ON DELETE CASCADE;

# --- !Downs

ALTER TABLE metric_value DROP FOREIGN KEY(run_id);
ALTER TABLE metric_value DROP FOREIGN KEY(metric_id);
DROP TABLE metric_value;
DROP SEQUENCE metric_value_id_seq;

ALTER TABLE metric DROP FOREIGN KEY(system_id);
DROP TABLE metric;
DROP SEQUENCE metric_id_seq;

ALTER TABLE run ADD values text NOT NULL;
