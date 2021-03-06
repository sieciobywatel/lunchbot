package actors

import actors.EaterActor._
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import commands.{Choose, Pay}
import org.scalatest.{FlatSpecLike, MustMatchers}

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActorSpec extends TestKit(ActorSystem("EaterActorSpec")) with FlatSpecLike with MustMatchers {

  it should "process food selection" in {

    val eater = "some_eater"

    val eaterActor = TestFSMRef(new EaterActor(eater))

    eaterActor.stateName mustBe Joined
    eaterActor.stateData mustBe Empty


    val food1 = "some_food"
    val food2 = "some_other_food"

    // choosing food

    eaterActor ! Choose(eater, food1)

    eaterActor.stateName mustBe FoodChosen
    eaterActor.stateData mustBe FoodData(food1)

    // changing food choice

    eaterActor ! Choose(eater, food2)

    eaterActor.stateName mustBe FoodChosen
    eaterActor.stateData mustBe FoodData(food2)

  }

  it should "process payment" in {

    val eater = "some_eater"

    val eaterActor = TestFSMRef(new EaterActor(eater))

    eaterActor.stateName mustBe Joined
    eaterActor.stateData mustBe Empty


    val food = "some_food"

    // trying to pay without choosing food

    eaterActor ! Pay(eater)

    eaterActor.stateName mustBe Joined
    eaterActor.stateData mustBe Empty

    // choosing food

    eaterActor ! Choose(eater, food)

    eaterActor.stateName mustBe FoodChosen
    eaterActor.stateData mustBe FoodData(food)

    // successfully paying

    eaterActor ! Pay(eater)

    eaterActor.stateName mustBe Paid
    eaterActor.stateData mustBe FoodData(food)

    // second pay should have no effect

    eaterActor ! Pay(eater)

    eaterActor.stateName mustBe Paid
    eaterActor.stateData mustBe FoodData(food)

    // choosing after payment shoud have no effect

    eaterActor ! Choose(eater, food)

    eaterActor.stateName mustBe Paid
    eaterActor.stateData mustBe FoodData(food)

  }

}
