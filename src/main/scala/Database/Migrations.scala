package Database

import cats.effect.Sync
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

object Migrations {
  private val migration: Update0 =
    sql"""
         CREATE SCHEMA IF NOT EXISTS traffic_limits;
         CREATE TABLE IF NOT EXISTS traffic_limits.limits_per_hour(
            effective_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            limit_name TEXT NOT NULL,
            limit_value BIGINT NOT NULL,
            PRIMARY KEY (effective_date, limit_name)
         );
         INSERT INTO traffic_limits.limits_per_hour (limit_name, limit_value)
            SELECT * FROM (VALUES ('min', 1024), ('max', 1073741824)) AS x
            WHERE NOT EXISTS (
               SELECT * FROM traffic_limits.limits_per_hour);
         """.update
  def migrate[F[_]: Sync](xa: Aux[F, Unit]): F[Unit] =
    migration.run.void.transact(xa)
}
