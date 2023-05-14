package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.{ OneFrameCacheMiss, OneFrameLookupFailed }
import forex.services.rates.errors._
import forex.services.rates.models.OneFrameRateResponse
import scalacache.Mode
import scalacache.caffeine.CaffeineCache
import sttp.client3.circe._
import sttp.client3.{ SttpBackend, _ }
import sttp.model.Uri

class OneFrameLive[F[_]: Sync: Mode](sttpBackend: SttpBackend[F, Any],
                                     config: OneFrameConfig,
                                     ratesCache: CaffeineCache[Rate])
    extends Algebra[F]
    with LazyLogging {

  private val TOKEN_HEADER_NAME = "token"
  private val oneFrameUrl       = s"http://${config.host}:${config.port}/rates"

  private def ratePairsToQueryParams(pairs: List[Rate.Pair]): String =
    pairs.map(pair => s"pair=${pair.from}${pair.to}").mkString("&")

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    for {
      maybePair <- ratesCache.get(pair.toString)
    } yield
      maybePair.fold(
        Either.left[Error, Rate](OneFrameCacheMiss(s"Pair $pair not found in cache"))
      )(
        Either.right[Error, Rate](_)
      )

  override def fetchRates(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] =
    for {
      _ <- Sync[F].delay(logger.info("Start fetching currency rates"))
      ratesResp <- basicRequest
                    .header(TOKEN_HEADER_NAME, config.apiKey)
                    .get(
                      uri"$oneFrameUrl?${ratePairsToQueryParams(pairs)}"
                        .querySegmentsEncoding(Uri.QuerySegmentEncoding.Relaxed)
                    )
                    .response(asJson[List[OneFrameRateResponse]])
                    .send(sttpBackend)
      ratesOrError = ratesResp.body
        .map(rates => rates.map(_.toDomainRate()))
        .left
        .map(re => OneFrameLookupFailed(re.getMessage))
      _ <- ratesOrError match {
            case Left(err) => Sync[F].delay(logger.error(s"Error while trying to fetch currency rates: ${err.msg}"))
            case Right(rates) => {
              logger.info("Currency rates fetched successfully")
              rates.traverse(rate => ratesCache.put(rate.pair.toString)(rate))
            }
          }
    } yield ratesOrError

}
