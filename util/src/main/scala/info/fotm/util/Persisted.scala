package info.fotm.util

trait Persisted[S] { self =>
  def save(state: S): Unit
  def fetch(): Option[S]

  lazy val readonly = new Persisted[S] {
    override def save(state: S): Unit = {}
    override def fetch(): Option[S] = self.fetch()
  }
}
