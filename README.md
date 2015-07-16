## Test coverage:

[scoverage](https://github.com/scoverage/sbt-scoverage)

To run the tests with coverage enabled enter:

```
$ sbt clean coverage test
```

After the tests have finished you should then run

```
$ sbt coverageReport
```

By default, scoverage will generate reports for each project seperately. 
You can merge them into an aggregated report by invoking the following command after all tests are run.

```
$ sbt coverageAggregate
```

