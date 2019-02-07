[![Build Status](https://travis-ci.org/navicore/akka-eventhubs-example.svg?branch=master)](https://travis-ci.org/navicore/akka-eventhubs-example)

Akka Eventhubs Example
---

Akka Streams Azure Eventhubs Source ~~and Sink~~

set up persistence with docker redis:

```
docker run -p 6379:6379 --name some-redis -d redis
export REDIS_HOST=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`
```
