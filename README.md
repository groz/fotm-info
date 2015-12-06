## General info

Project http://fotm.info has been stopped as of 12/05/2015.

This repository contains almost everything that is needed to get it up and running.

See notes below for full building/deployment if you want to host it yourself.

## How to checkout

Project uses git submodules. To checkout:

```
$ git clone https://github.com/Groz/fotm-info 
$ cd fotm-info 
$ git submodule init 
$ git submodule update
```

## Build

Download [Simple Build Tool](http://www.scala-sbt.org/download.html).

```$ sbt compile```

## Run

Project follows simple three-tiered architecture pattern and consists of mongo storage and *portal* and *crawler* subprojects.

To run locally:
- spin up [mongodb](https://www.mongodb.org/)
- obtain a key from [dev.battle.net](dev.battle.net) key to access battle.net api
- create *core/src/main/resources/secrets.conf* (do not check it into git) file and put the connection string and dev key there:
```
prod-env.mongodb.uri = "mongodb://user:password@host:30500/fotmdb"
bnet-api-key = "h2cgpv58h78j3ec9mwg477ees9mqq8vq"
```
- ```$ sbt "crawler/run"``` starts crawler
- ```$ sbt "portal/run"``` starts web portal

## Deploy

It can run in any environment, but already has useful scripts for one click build/test/deploy to [AWS](aws.amazon.com). Just create the instances with [create_instance](scripts/aws/create_instance.sh) script and run full deployment with [deploy.sh](scripts/aws/deploy.sh).

Enjoy.
