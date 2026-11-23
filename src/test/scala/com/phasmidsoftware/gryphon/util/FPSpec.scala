package com.phasmidsoftware.gryphon.util

import com.phasmidsoftware.gryphon.util.FP.{partition, sequence}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Using.Releasable
import scala.util.{Failure, Success, Try}

class FPSpec extends AnyFlatSpec with Matchers {

  behavior of "FP"

  it should "sequence 0" in {
    val target: Seq[Option[Int]] = Nil
    sequence(target) shouldBe None
  }

  it should "sequence 1" in {
    val target: Seq[Option[Int]] = Seq(Some(1), None, Some(2))
    sequence(target) shouldBe Some(Seq(1, 2))
  }

  it should "sequence 2" in {
    val target: Seq[Option[Int]] = Seq(None, Some(2), Some(1))
    sequence(target) shouldBe Some(Seq(2, 1))
  }

  it should "sequence 3" in {
    val target: Seq[Option[Int]] = Seq(None)
    sequence(target) shouldBe None
  }

  private val boom = new Exception("boom")
  private val bang = new Exception("bang")

  behavior of "FP.partition"

  // The Seq overload is reached only indirectly, from GraphParser:64, so nothing asserted
  // the partitioning itself. Note that both overloads preserve the original order within
  // each side; callers that report failures in input order depend on that.
  it should "split a Seq into successes then failures, preserving order" in {
    val xys: Seq[Try[Int]] = Seq(Success(1), Failure(boom), Success(2), Failure(bang))
    val (successes, failures) = partition(xys)
    successes shouldBe Seq(Success(1), Success(2))
    failures shouldBe Seq(Failure(boom), Failure(bang))
  }

  it should "split an empty Seq into two empty Seqs" in {
    val (successes, failures) = partition(Seq.empty[Try[Int]])
    successes shouldBe Nil
    failures shouldBe Nil
  }

  it should "put everything on the right when a Seq is all failures" in {
    val xys: Seq[Try[Int]] = Seq(Failure(boom), Failure(bang))
    val (successes, failures) = partition(xys)
    successes shouldBe Nil
    failures shouldBe xys
  }

  it should "put everything on the left when a Seq is all successes" in {
    val xys: Seq[Try[Int]] = Seq(Success(1), Success(2))
    val (successes, failures) = partition(xys)
    successes shouldBe xys
    failures shouldBe Nil
  }

  // Pins which overload a Seq argument selects: the Iterator overload sits immediately
  // beside the Seq one, and an argument reaching the wrong one would still compile.
  it should "split an Iterator into successes and failures" in {
    val (successes, failures) = partition(Iterator(Success(1), Failure(boom), Success(2)))
    successes.toSeq shouldBe Seq(Success(1), Success(2))
    failures.toSeq shouldBe Seq(Failure(boom))
  }

  /**
   * A resource that records whether it was released, so the tests below can assert the
   * release actually happened rather than merely that the result was the right shape.
   */
  private class TestResource {
    var released: Boolean = false
  }

  private given Releasable[TestResource] with {
    def release(resource: TestResource): Unit = resource.released = true
  }

  behavior of "TryUsing.tryIt"

  it should "flatten the result and release the resource" in {
    val resource = new TestResource
    TryUsing.tryIt(Success(resource))(_ => Success(42)) shouldBe Success(42)
    resource.released shouldBe true
  }

  // The point of the wrapper: a Failure from f must not leak the resource.
  it should "release the resource when f returns a Failure" in {
    val resource = new TestResource
    TryUsing.tryIt(Success(resource))(_ => Failure(boom)) shouldBe Failure(boom)
    resource.released shouldBe true
  }

  it should "release the resource when f throws" in {
    val resource = new TestResource
    TryUsing.tryIt[TestResource, Int](Success(resource))(_ => throw boom) shouldBe Failure(boom)
    resource.released shouldBe true
  }

  it should "propagate a failed resource without calling f" in {
    var called = false
    val result = TryUsing.tryIt[TestResource, Int](Failure(boom)) { _ =>
      called = true
      Success(1)
    }
    result shouldBe Failure(boom)
    called shouldBe false
  }

  // ry is by-name, so a caller passing an expensive or effectful expression needs to know
  // it is evaluated once and not once per use inside the for-comprehension.
  it should "evaluate the by-name resource exactly once" in {
    var evaluations = 0
    val resource = new TestResource
    val result = TryUsing.tryIt({
      evaluations += 1
      Success(resource)
    })(_ => Success(7))
    result shouldBe Success(7)
    evaluations shouldBe 1
  }

  behavior of "TryUsing.trial"

  // trial has no caller in src/main — see #97. These pin its behaviour while that decision
  // is open, so that removing it later is a deliberate act rather than a silent one.
  it should "apply f and release the resource" in {
    val resource = new TestResource
    TryUsing.trial(Success(resource))(_ => 42) shouldBe Success(42)
    resource.released shouldBe true
  }

  it should "release the resource when f throws" in {
    val resource = new TestResource
    TryUsing.trial[TestResource, Int](Success(resource))(_ => throw boom) shouldBe Failure(boom)
    resource.released shouldBe true
  }

  it should "propagate a failed resource without calling f" in {
    var called = false
    val result = TryUsing.trial[TestResource, Int](Failure(boom)) { _ =>
      called = true
      1
    }
    result shouldBe Failure(boom)
    called shouldBe false
  }

  it should "evaluate the by-name resource exactly once" in {
    var evaluations = 0
    val resource = new TestResource
    val result = TryUsing.trial({
      evaluations += 1
      Success(resource)
    })(_ => 7)
    result shouldBe Success(7)
    evaluations shouldBe 1
  }

  behavior of "TryUsing.apply"

  it should "flatten the result and release the resource" in {
    val resource = new TestResource
    TryUsing(resource)(_ => Success(42)) shouldBe Success(42)
    resource.released shouldBe true
  }

  it should "release the resource when f returns a Failure" in {
    val resource = new TestResource
    TryUsing(resource)(_ => Failure(boom)) shouldBe Failure(boom)
    resource.released shouldBe true
  }
}
