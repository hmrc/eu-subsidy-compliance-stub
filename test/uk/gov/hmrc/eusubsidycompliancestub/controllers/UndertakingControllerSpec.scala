/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.retrieveUndertakingEORIWrites
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EORI

import scala.concurrent.Future

class UndertakingControllerSpec extends BaseControllerSpec {

  private val controller: UndertakingController =
    app.injector.instanceOf[UndertakingController]

  "Retrieve Undertaking" must {

    val validRetrieveUndertakingBody: JsValue =
      Json.toJson(EORI("GB123456789012345"))(retrieveUndertakingEORIWrites)

    val invalidRetrieveUndertakingBody: JsValue = Json.obj("foo" -> "bar")

    "return an Undertaking for a valid request" in {
      val post = FakeRequest("POST", "//scp/retrieveundertaking/v1", fakeHeaders, validRetrieveUndertakingBody)
      val result: Future[Result] = controller.retrieve.apply(post)
//      println(Json.prettyPrint(validRetrieveUndertakingBody))
//      println(contentAsString(result))
      status(result) mustEqual  play.api.http.Status.OK
    }

    "return a 403 (weirdly) if the request payload is not valid" in {
      val post = FakeRequest("POST", "//scp/retrieveundertaking/v1", fakeHeaders, invalidRetrieveUndertakingBody)
      val result: Future[Result] = controller.retrieve.apply(post)
      status(result) mustEqual  play.api.http.Status.FORBIDDEN
    }

  }

}
