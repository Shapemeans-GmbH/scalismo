/*
 * Copyright University of Basel, Graphics and Vision Research Group
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

package scalismo.common

import scalismo.ScalismoTestSuite
import scalismo.geometry._
import scalismo.image.{DiscreteImage, DiscreteImageDomain1D, DiscreteImageDomain2D, DiscreteImageDomain3D}
import scalismo.common.interpolation.{LinearImageInterpolator, LinearImageInterpolator3D}

class LinearInterpolatorTest extends ScalismoTestSuite {

  describe("A Linear Interpolator") {

    it("returns correct value at grid points.") {

      val dom = DiscreteImageDomain3D(Point(0, 0, 0), EuclideanVector(1, 1, 1), IntVector(2, 2, 2))
      val img = DiscreteImage(dom, (p: Point[_3D]) => p.x)
      val img_interpolated = img.interpolate(LinearImageInterpolator[_3D, Double]())
      val id = img.domain.pointSet.pointId(IntVector(1, 1, 1))
      val point = img.domain.pointSet.point(id)

      img(id) shouldBe img_interpolated(point)

    }

    it("correctly interpolates in 3D") {

      val dom = DiscreteImageDomain3D(Point(0, 0, 0), EuclideanVector(1, 1, 1), IntVector(2, 2, 2))
      val img = DiscreteImage(dom, (p: Point[_3D]) => p.x)
      val img_interpolated = img.interpolate(LinearImageInterpolator[_3D, Double]())
      val point = Point(0.5, 0.5, 0.5)

      img_interpolated(point) shouldBe 0.5

    }

    it("correctly interpolates in 2D") {

      val dom = DiscreteImageDomain2D(Point(0, 0), EuclideanVector(1, 1), IntVector(2, 2))
      val img = DiscreteImage(dom, (p: Point[_2D]) => p.x)
      val img_interpolated = img.interpolate(LinearImageInterpolator[_2D, Double]())
      val point = Point(0.5, 0.5)

      img_interpolated(point) shouldBe 0.5

    }

    it("correctly interpolates in 1D") {

      val dom = DiscreteImageDomain1D(Point(0), EuclideanVector(1), IntVector(2))
      val img = DiscreteImage(dom, (p: Point[_1D]) => p.x)
      val img_interpolated = img.interpolate(LinearImageInterpolator[_1D, Double]())
      val point = Point(0.5)

      img_interpolated(point) shouldBe 0.5

    }

    it("can be evaluated everywhere where the original image is defined") {
      val originalDomain =
        DiscreteImageDomain3D(Point3D(0, 0, 0), EuclideanVector3D(0.1, 0.1, 0.1), IntVector3D(10, 10, 10))
      val img = DiscreteImage(originalDomain, (p: Point[_3D]) => p.x)
      val fineDomain = DiscreteImageDomain3D(originalDomain.boundingBox, IntVector3D(100, 100, 100))

      val interpolatedImg = img.interpolate(LinearImageInterpolator3D[Double]())
      for (pt <- fineDomain.pointSet.points) {
        interpolatedImg.isDefinedAt(pt) shouldBe (true)
      }

      try {
        interpolatedImg.discretize(fineDomain, 0)
      } catch {
        case ex: Exception => fail("Should not throw Exception", ex)
      }
    }

  }

}
