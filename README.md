# forex-mtl - local proxy for getting Currency Exchange Rates

## Requirements
 - The service returns an exchange rate when provided with 2 supported currencies
 - The rate should not be older than 5 minutes
 - The service should support at least 10,000 successful requests per day with 1 API token (The One-Frame service supports a maximum of 1000 requests per day for any given authentication token.)

## Implementation notes

To satisfy requirements I decided to fetch all currency rates every 5 minutes and store them into in-memory cache. We support only 9 currencies, that forms 72 pairs, so there is no problem to store them inmemory. All get request will hit this cache. 
Timeout of 5 minutes gives us 1440/5=288 requests per day to One-Frame service. We can decrease timeout to 87 seconds (86400/87=993) and still be within the quota of 1000 requests per day.
So ONEFRAME_UPDATE_TIMEOUT value must be in the range from 300 to 87 seconds.

## How to build
```bash
sbt clean assembly docker
```

## How to run
```bash
docker compose up
```