/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.newcore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ConnectionSpec extends AnyFlatSpec with should.Matchers {

  behavior of "Connection"

  it should "UndirectedConnection" in {
    val connection = new UndirectedConnection[Int](1, 2)
    connection.connection(0) shouldBe None
    connection.connection(1) shouldBe Some(2)
    connection.connection(2) shouldBe Some(1)
  }

  it should "DirectedConnection" in {
    val connection = new DirectedConnection[Int](1, 2)
    connection.connection(0) shouldBe None
    connection.connection(1) shouldBe Some(2)
    connection.connection(2) shouldBe None
  }

}
