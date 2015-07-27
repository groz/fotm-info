package info.fotm.util

trait Persisted[S] {
  def save(state: S): Unit
  def fetch(): Option[S]
}
