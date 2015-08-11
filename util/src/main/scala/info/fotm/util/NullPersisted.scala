package info.fotm.util

class NullPersisted[S] extends Persisted[S] {
  override def save(state: S): Unit = {}
  override def fetch(): Option[S] = None
}

class MemoryPersisted[S] extends Persisted[S] {
  var stateOption: Option[S] = None
  override def save(state: S): Unit = { stateOption = Some(state) }
  override def fetch(): Option[S] = stateOption
}
