package info.fotm.util

import scala.util.Try

trait Persisted[S] { self =>
  def save(state: S): Try[Unit]
  def fetch(): Try[S]

  lazy val readonly = new Persisted[S] {
    override def save(state: S): Try[Unit] = Try{}
    override def fetch(): Try[S] = self.fetch()
  }
}
