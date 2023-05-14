package forex.programs.rates

import forex.domain.Rate
import forex.programs.rates.errors._

import scala.concurrent.duration.FiniteDuration

trait Algebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[Error Either Rate]
  def updateRatesPeriodically(duration: FiniteDuration): F[Unit]
}
