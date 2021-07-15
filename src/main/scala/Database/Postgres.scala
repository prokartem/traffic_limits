package Database

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats.effect._
import cats.implicits._
import doobie.postgres.sqlstate

class Postgres[F[_]: Async: ContextShift] {
  private val xa = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:postgres",
    "postgres",
    "password",
    Blocker.liftExecutionContext(ExecutionContexts.synchronous),
  )

  def migrate: F[Unit] = Migrations.migrate(xa)
  def insertNewLimit(limitName: String, limitValue: Long): F[Unit] =
    sql"""
        |INSERT INTO traffic_limits.limits_per_hour (limit_name, limit_value)
        |VALUES ($limitName, $limitValue);
         """.stripMargin.update.run.void.transact(xa)

  def getMaxLimit: F[Long] =
    sql"""
         |SELECT limit_value FROM traffic_limits.limits_per_hour 
         |WHERE limit_name = 'max' 
         |GROUP BY effective_date, limit_name
         |ORDER BY  max(effective_date) DESC
         |LIMIT 1;
         """.stripMargin.query[Long].unique.transact(xa)

  def getMinLimit: F[Long] =
    sql"""
         |SELECT limit_value FROM traffic_limits.limits_per_hour 
         |WHERE limit_name = 'min' 
         |GROUP BY effective_date, limit_name
         |ORDER BY  max(effective_date) DESC
         |LIMIT 1;
         """.stripMargin.query[Long].unique.transact(xa)
}
