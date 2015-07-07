import ch.qos.logback.classic.boolex.JaninoEventEvaluator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.filter.EvaluatorFilter
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

def createAppender =  { region, bracket ->
    def name = region + "-" + bracket
    appender(name, FileAppender) {
        filter(EvaluatorFilter) {
            evaluator(JaninoEventEvaluator) {
                expression = 'return logger.contains("netty") || logger.contains("AsyncCompletionHandler");'
            }
            onMatch = DENY
        }
        filter(EvaluatorFilter) {
            evaluator(JaninoEventEvaluator) {
                expression = 'return ((String)mdc.get("akkaSource")).contains("US-2v2");'
            }
            onMatch = ACCEPT
            onMismatch = DENY
        }
        append = false
        file = "logs/${name}.txt"
        encoder(PatternLayoutEncoder) {
            pattern = "[%d{HH:mm:ss.SSS}] %-5level [%X{akkaSource}] %logger{36} - %msg%n"
        }
    }
}

createAppender("us", "2v2")

root(DEBUG, ["STDOUT", "us-2v2"])
