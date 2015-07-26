import scala.collection.immutable.Queue
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Try, Success}

import scala.concurrent.ExecutionContext.Implicits.global

type Set = (Int => Boolean)

def single(a: Int): Set = _ == a

def union(s1: Set, s2: Set): Set = x => s1(x) || s2(x)

def intersection(s1: Set, s2: Set): Set = x => s1(x) && s2(x)

def filter(s: Set, p: Int => Boolean): Set = x => s(x) && p(x)
//def filter = intersection _

val positives: Set = _ > 0

def myCollect[A](futures: Seq[Future[A]]): Future[Seq[A]] =
  Future.fold(futures)(Queue.empty[A]) { _ enqueue _ }

val a = Future { 1 }
val b = Future { 2 }
val c = Future { 3 }

a.onComplete((x: Try[Int]) => x)
Await.result(a, Duration.Zero)

scala.io.Source.fromURL("http://www.google.com").mkString

def countWords(text: String): Map[String, Int] =
  text.split("\\W+").groupBy(identity).map(kv => (kv._1, kv._2.size))

countWords("privet, menya privet zovut")

def loadFileAsync(name: String): Future[String] = Future {
  scala.io.Source.fromFile(name).mkString
}

def loadPageAsync(url: String): Future[String] = Future {
  scala.io.Source.fromURL(url).mkString
}

def loadFromDb(id: Int): Future[String] = Future {
  scala.io.Source.fromFile(id.toString).mkString
}


val filesToParse = Seq("first.txt", "second.txt", "third.txt")

val futures: Seq[Future[String]] = filesToParse.map(path => loadFileAsync(path))

val seed = Map.empty[String, Int].withDefaultValue(0)

val result = Future.fold(futures)(seed) { (dict, text) =>
  text.split("\\W+").foldLeft(dict) { (d, w) =>
    d.updated(w, d(w) + 1)
  }
}
