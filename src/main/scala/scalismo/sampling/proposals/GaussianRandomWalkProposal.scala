/*
 * Copyright 2016 University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scalismo.sampling.proposals

import breeze.linalg.DenseVector
import breeze.stats.stddev
import scalismo.common.Vectorizer
import scalismo.sampling.{MHProposalGenerator, MHSample, ProposalGenerator, SampleLens, TransitionProbability}

/**
 * Classical Random Walk proposal, where the current state is perturbed using a step, which is generated
 * by an isotropic gaussian with the given standard deviation.
 */
class GaussianRandomWalkProposal(stddev: Double, val tag: String)(implicit
                                                                  rng: scalismo.utils.Random)
    extends MHProposalGenerator[DenseVector[Double]] {
  self =>

  implicit val randBasis: breeze.stats.distributions.RandBasis = rng.breezeRandBasis
  private val gaussianDist = breeze.stats.distributions.Gaussian(0, stddev)

  override def propose(sample: MHSample[DenseVector[Double]]): MHSample[DenseVector[Double]] = {
    val partAsVec: DenseVector[Double] = sample.parameters
    val perturbation: DenseVector[Double] = DenseVector.rand(partAsVec.length, gaussianDist)
    sample.copy(parameters = partAsVec + perturbation, generatedBy = tag)
  }

  override def logTransitionProbability(from: MHSample[DenseVector[Double]],
                                        to: MHSample[DenseVector[Double]]): Double = {

    if (from.parameters.length != to.parameters.length) {
      Double.NegativeInfinity
    } else {

      val dim = from.parameters.length
      val logProbs = for (i <- 0 until dim) yield {
        val residual = to.parameters(i) - from.parameters(i)
        gaussianDist.logPdf(residual)
      }
      logProbs.sum
    }
  }

  /**
   * Create a new GaussianRandomWalkProposal, which only updates the coefficients in an given range.
   */
  def partial(range: Range): GaussianRandomWalkProposal = {
    new GaussianRandomWalkProposal(stddev, tag) {
      override def propose(sample: MHSample[DenseVector[Double]]): MHSample[DenseVector[Double]] = {
        val partialSample = sample.copy(parameters = sample.parameters(range).copy)
        val partialNew = self.propose(partialSample).parameters
        val newFull = sample.parameters.copy
        newFull(range) := partialNew
        sample.copy(parameters = newFull, generatedBy = tag)
      }

      override def logTransitionProbability(from: MHSample[DenseVector[Double]],
                                            to: MHSample[DenseVector[Double]]): Double = {

        // This proposal generator can only change proposal for the given parameter range
        // If anything else should be changed, there is 0 probability to reach that state
        // To check that, we patch the parameters in the to vector with those from the from vector.
        // If they are the same after that copy things are fine. If they should be different, there
        // is zero probability of transitioning there
        val patchedToParameters = to.parameters.copy
        patchedToParameters(range) := from.parameters(range)
        if (patchedToParameters != from.parameters) {
          Double.NegativeInfinity
        } else {
          val fromPartial = from.copy(parameters = from.parameters(range).copy)
          val toPartial = to.copy(parameters = to.parameters(range).copy)
          self.logTransitionProbability(fromPartial, toPartial)
        }
      }
    }

  }
}

object GaussianRandomWalkProposal {
  def apply(stddev: Double, tag: String)(implicit rng: scalismo.utils.Random): GaussianRandomWalkProposal =
    new GaussianRandomWalkProposal(stddev, tag)
}

class MHIdentityProposal[A]() extends MHProposalGenerator[A] {

  /** rate of transition from to (log value) */
  override def logTransitionProbability(from: MHSample[A], to: MHSample[A]): Double = 0

  /** draw a sample from this proposal distribution, may depend on current state */
  override def propose(current: MHSample[A]): MHSample[A] = current.copy(generatedBy = "ident")
}

object MHIdentityProposal {
  def forType[A] = new MHIdentityProposal[A]()
}
