/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancestub.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.Play.materializer
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.eusubsidycompliancestub.BaseSpec
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.services.Store
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances.arbUndertakings
class StoreControllerSpec extends BaseSpec with BeforeAndAfterEach {
  private val controller = app.injector.instanceOf[StoreController]

  "StoreController" must {
    "clear store" in {
      forAll { undertakings: List[Undertaking] =>
        undertakings.foreach(Store.undertakings.put)
        val response = controller.clearStore(FakeRequest())
        status(response) mustBe 200
        contentAsString(response) mustBe "\"cleared: true\""
        Store.isEmpty mustBe true
      }
    }

    "show store" in {
      val undertakings = arbUndertakings.arbitrary.sample.get
      Store.clear()
      undertakings.foreach(Store.undertakings.put)
      val response = controller.show(FakeRequest())
      status(response) mustBe 200
      val responseContent = Json.parse(contentAsString(response)).as[List[Undertaking]]
      responseContent.sortBy(_.reference) mustBe undertakings.sortBy(_.reference)
    }
  }

  override def beforeEach(): Unit = {
    Store.clear()
  }
}
