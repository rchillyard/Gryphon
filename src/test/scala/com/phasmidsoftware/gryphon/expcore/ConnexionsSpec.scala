/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.expcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.util.Random

class ConnexionsSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Connexions"

  implicit val random: Random = new Random()

  it should "empty" in {
    val property: String = "Hello"
    val target: Connexions[Int, Connexion[Int], String] = Connexions.empty(property)
    target.connexions.isEmpty shouldBe true
    target.property shouldBe property
  }

  it should "$plus" in {
    val property: String = "Hello"
    val target: Connexions[Int, Connexion[Int], String] = Connexions.empty(property)
    val t: Connexion[Int] = DirectedConnexionCase(0, 1)
    val updated = target + t
    val connexions = updated.connexions
    connexions.isEmpty shouldBe false
    updated.property shouldBe property
    val iterator = connexions.iterator
    iterator.hasNext shouldBe true
    iterator.next() shouldBe t
    iterator.hasNext shouldBe false
  }

  it should "unit" in {
    val property: String = "Hello"
    val target: Connexions[Int, Connexion[Int], String] = Connexions.empty(property)
    val t1: Connexion[Int] = DirectedConnexionCase(0, 1)
    val t2: Connexion[Int] = DirectedConnexionCase(1, 2)
    val bag = Bag.create(t1, t2)
    val twoBagger: Connexions[Int, Connexion[Int], String] = target.unit(bag, "Goodbye")
    twoBagger.property shouldBe "Goodbye"
    twoBagger.connexions shouldBe bag
  }
}
