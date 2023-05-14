package forex

import scala.concurrent.ExecutionContext

import cats.effect._
import cats.implicits._
import forex.config._
import org.http4s.server.blaze.BlazeServerBuilder
import scalacache.CatsEffect.modes.async

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer: ContextShift] {

  def stream(ec: ExecutionContext): F[Unit] =
    for {
      config <- Config.load("app")
      _ <- Resources
            .make[F]()
            .use(resources => {
              val module = new Module[F](config, resources)
              for {
                _ <- Concurrent[F].start(module.ratesProgram.updateRatesPeriodically(config.oneFrame.updateTimeout))
                _ <- BlazeServerBuilder[F](ec)
                      .bindHttp(config.http.port, config.http.host)
                      .withHttpApp(module.httpApp)
                      .serve
                      .compile
                      .drain
              } yield ()
            })
    } yield ()

}
