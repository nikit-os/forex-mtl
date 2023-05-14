package forex.programs.rates

import cats.data.EitherT
import cats.effect.{ Concurrent, Timer }
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.Currency._
import forex.domain._
import forex.programs.rates.errors._
import forex.services.RatesService

import scala.concurrent.duration.FiniteDuration

class Program[F[_]: Concurrent: Timer](
    ratesService: RatesService[F]
) extends Algebra[F]
    with LazyLogging {

  private val allRates = List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)
  private val allRatePairs = for {
    cur1 <- allRates
    cur2 <- allRates
    ratePair = Rate.Pair(cur1, cur2) if cur1 != cur2
  } yield ratePair

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] =
    EitherT(ratesService.get(Rate.Pair(request.from, request.to))).leftMap(toProgramError).value

  def updateRatesPeriodically(duration: FiniteDuration): F[Unit] = {
    def updateRatesJob(): F[Unit] =
      for {
        _ <- ratesService.fetchRates(allRatePairs)
        _ <- Timer[F].sleep(duration)
        _ <- updateRatesJob()
      } yield ()

    updateRatesJob().handleErrorWith { e =>
      {
        logger.error("Cache update failed", e)
        updateRatesPeriodically(duration)
      }
    }
  }
}

object Program {

  def apply[F[_]: Concurrent: Timer](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}
