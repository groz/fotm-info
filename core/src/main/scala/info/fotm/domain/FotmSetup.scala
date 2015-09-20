package info.fotm.domain

final case class FotmSetup(specIds: Seq[Int], ratio: Double) {
  lazy val orderedSpecIds = specIds.sorted( CharacterOrderingFactory.specIdOrdering[Int](identity) )
}
