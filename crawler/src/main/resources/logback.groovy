import ch.qos.logback.classic.boolex.JaninoEventEvaluator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

import static ch.qos.logback.core.spi.FilterReply.ACCEPT
import static ch.qos.logback.core.spi.FilterReply.DENY

appender("STDOUT", ConsoleAppender) {
  filter(EvaluatorFilter) {
    evaluator(JaninoEventEvaluator) {
      expression = 'return logger.contains("netty") || logger.contains("AsyncCompletionHandler");'
    }
    onMatch = DENY
  }
  encoder(PatternLayoutEncoder) {
    pattern = "[%d{HH:mm:ss.SSS}] %-5level [%X{akkaSource}] %logger{36} - %msg%n"
  }
}

def rolloverPattern = { name -> "logs/archives/%d{yyyy-MM-dd_HH-mm}/${name}.txt" }

def createAppender =  { region, bracket ->
    def name = region + "-" + bracket
    appender(name, RollingFileAppender) {
        filter(EvaluatorFilter) {
            evaluator(JaninoEventEvaluator) {
                expression = 'return logger.contains("netty") || logger.contains("AsyncCompletionHandler");'
            }
            onMatch = DENY
        }
        filter(EvaluatorFilter) {
            evaluator(JaninoEventEvaluator) {
                expression = 'return ((String)mdc.get("akkaSource")).toUpperCase().contains("' + name.toUpperCase() + '");'
            }
            onMatch = ACCEPT
            onMismatch = DENY
        }
        file = "logs/${name}.txt"
        encoder(PatternLayoutEncoder) {
            pattern = "[%d{HH:mm:ss.SSS}] %-5level [%X{akkaSource}] %logger{36} - %msg%n"
        }
        rollingPolicy(TimeBasedRollingPolicy) {
            fileNamePattern = rolloverPattern(name)
            maxHistory = 10
        }
    }
    name
}

appender("COMBINED", RollingFileAppender) {
    filter(EvaluatorFilter) {
        evaluator(JaninoEventEvaluator) {
            expression = 'return logger.contains("netty") || logger.contains("AsyncCompletionHandler");'
        }
        onMatch = DENY
    }
    encoder(PatternLayoutEncoder) {
        pattern = "[%d{HH:mm:ss.SSS}] %-5level [%X{akkaSource}] %logger{36} - %msg%n"
    }
    file = "logs/combined.txt"
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = rolloverPattern("combined")
        maxHistory = 10
    }
}

def brackets = ["2v2", "3v3", "5v5", "rbg"]
def regions = ["us", "europe", "taiwan", "korea", "china"]

def appenders = ["STDOUT", "COMBINED"]

for (b in brackets) {
    for (r in regions) {
        appenders << createAppender(r, b)
    }
}

root(DEBUG, appenders)
