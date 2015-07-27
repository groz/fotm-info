# Dev
 
[![Build Status](https://travis-ci.org/Groz/fotm-info.svg)](https://travis-ci.org/Groz/fotm-info)

## To do

[Preproduction preparation checklist](https://github.com/Groz/fotm-info/issues/7)

## Test coverage

The project uses [scoverage](https://github.com/scoverage/sbt-scoverage).

To run the tests with coverage enabled enter:

```
$ sbt clean coverage test
```

After the tests have finished you should then run:

```
$ sbt coverageReport
```

By default, scoverage will generate reports for each project separately. 
You can merge them into an aggregated report by invoking the following command after all tests are run:

```
$ sbt coverageAggregate
```
