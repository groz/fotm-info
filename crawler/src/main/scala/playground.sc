import info.fotm.crawler.ObservableStream

class S extends ObservableStream[Int] {
}

val s = new S()

s.subscribe(println)
s.publish(1)
s.publish(2)
s.publish(3)
val x: List[Int]
