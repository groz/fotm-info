package info.fotm.util

class NullPersisted[S] extends Persisted[S] {
  override def save(state: S): Unit = {}
  override def fetch(): Option[S] = None
}
