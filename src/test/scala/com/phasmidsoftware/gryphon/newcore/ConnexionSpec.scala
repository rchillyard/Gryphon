/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ConnexionSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Connexion"

  it should "UndirectedConnexion" in {
    val connexion = new UndirectedConnexion[Int](1, 2)
    connexion.connexion(0) shouldBe None
    connexion.connexion(1) shouldBe Some(2)
    connexion.connexion(2) shouldBe Some(1)
  }

  it should "DirectedConnexion" in {
    val connexion = new DirectedConnexion[Int](1, 2)
    connexion.connexion(0) shouldBe None
    connexion.connexion(1) shouldBe Some(2)
    connexion.connexion(2) shouldBe None
  }

}
