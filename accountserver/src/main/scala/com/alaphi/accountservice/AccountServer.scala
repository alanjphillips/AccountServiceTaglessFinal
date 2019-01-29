package com.alaphi.accountservice

import cats.effect._
import cats.syntax.all._
import com.alaphi.accountservice.db.AccountInMemoryDatabase
import com.alaphi.accountservice.http.AccountApi
import com.alaphi.accountservice.program.AccountProgram
import com.alaphi.accountservice.repository.AccountInMemoryRepository
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

object AccountServer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = startServer[IO].as(ExitCode.Success)

  def startServer[F[_]: ConcurrentEffect](implicit ctx: ContextShift[F], timer: Timer[F]) : F[Unit] = {
    implicit val F = Sync[F]

    for {
      accountDB <- AccountInMemoryDatabase.createDB[F]
      accountRepository = new AccountInMemoryRepository[F](accountDB)
      accountProgram = new AccountProgram[F](accountRepository)
      accountApi = new AccountApi[F](accountProgram)
      server <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(accountApi.routes.orNotFound)
        .serve
        .compile
        .drain
    } yield server
  }

}






