package forex.services.rates

import forex.domain.{ Price, Rate, Timestamp }
import io.circe.Decoder

import java.time.{ Instant, ZoneOffset }

object models {

  case class OneFrameRateResponse(
      from: String,
      to: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      timestamp: Instant
  ) {
    def toDomainRate(): Rate =
      Rate(Rate.Pair(this.from, this.to), Price(this.price), Timestamp(this.timestamp.atOffset(ZoneOffset.UTC)))
  }

  object OneFrameRateResponse {
    implicit val decoder: Decoder[OneFrameRateResponse] = Decoder.forProduct6(
      "from",
      "to",
      "bid",
      "ask",
      "price",
      "time_stamp"
    )(OneFrameRateResponse.apply)
  }

}
