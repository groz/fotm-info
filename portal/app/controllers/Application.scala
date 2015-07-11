package controllers

import models._
import play.api._
import play.api.mvc._

object Application extends Controller {

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
