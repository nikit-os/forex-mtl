package forex

import cats.effect.{ Concurrent, ContextShift, Resource, Sync }
import forex.domain.Rate
import scalacache.caffeine.CaffeineCache
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

case class Resources[F[_]](
    sttpBackend: SttpBackend[F, Any],
    ratesCache: CaffeineCache[Rate]
)

object Resources {
  def make[F[_]: ContextShift: Concurrent](): Resource[F, Resources[F]] =
    for {
      sttpBackend <- AsyncHttpClientCatsBackend.resource[F]()
      ratesCache <- Resource.eval(Sync[F].delay(CaffeineCache[Rate]))
    } yield Resources(sttpBackend, ratesCache)
}
