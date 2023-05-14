package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.OneFrameConfig
import forex.domain.Rate
import interpreters._
import scalacache.Mode
import scalacache.caffeine.CaffeineCache
import sttp.client3.SttpBackend

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync: Mode](sttpBackend: SttpBackend[F, Any],
                             config: OneFrameConfig,
                             ratesCache: CaffeineCache[Rate]): Algebra[F] =
    new OneFrameLive[F](sttpBackend, config, ratesCache)
}
