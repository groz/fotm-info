import info.fotm.util.ObservableStream

val sink = new ObservableStream[Int] {
  def put(x: Int) = super.publish(x)
}

val othersink = new ObservableStream[Int] {
  def put(x: Int) = super.publish(x)
}

for (x <- sink) {
  println(s"Sink: $x")
}

for (y <- othersink) {
  println(s"Other sink: $y")
}

val combinedSink = for {
  x <- sink
  if x % 2 == 0
  y <- othersink
} yield (x, y)

for (xy <- combinedSink) {
  println(s"Combined sink: $xy")
}

sink.put(1) // Sink: 1
sink.put(2) // Sink: 2
sink.put(3) // Sink: 3
othersink.put(10) // Other sink: 30, Combined sink: (2, 10)
