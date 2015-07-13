package controllers

import info.fotm.aether.LadderStorageActor
import models._

import javax.inject._
import akka.actor._
import play.api._
import play.api.mvc._

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  lazy val ladderStorageActor = system.actorOf(Props[LadderStorageActor], "ladder-storage-actor")

  def index = Action {
    implicit val euRegion = Region("EU")
    val sf: Realm = Realm("Soulflayer", "sf")
    val raz: Realm = Realm("Razuvious", "raz")(Region("KR"))

    val id = info.fotm.domain.CharacterId("", "")

    val groz = Character("Groz", CharacterClass(5, Some(270)), sf, Stats(0, 0, 0, 0))
    val srez = Character("Srez", CharacterClass(3, Some(62)), raz, Stats(0, 0, 0, 0))
    val dond = Character("Donder", CharacterClass(4, Some(71)), raz, Stats(0, 0, 0, 0))

    val team1 = Team(List(groz, srez, dond), Stats(0, 1, 0, 1))(Threes)

    Ok(views.html.index("Hullo", List(team1), Nil))
  }

}
