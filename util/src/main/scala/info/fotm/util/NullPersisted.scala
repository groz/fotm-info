package info.fotm.util

import scala.util.{Failure, Try}

class NullPersisted[S] extends Persisted[S] {
  override def save(state: S): Try[Unit] = Try {}
  override def fetch(): Try[S] = Failure(new NoSuchElementException)
}

class MemoryPersisted[S] extends Persisted[S] {
  var stateOption: Option[S] = None
  override def save(state: S): Try[Unit] = Try { stateOption = Some(state) }
  override def fetch(): Try[S] = stateOption.fold(Failure(new NoSuchElementException): Try[S])(x => Try(x))
}
