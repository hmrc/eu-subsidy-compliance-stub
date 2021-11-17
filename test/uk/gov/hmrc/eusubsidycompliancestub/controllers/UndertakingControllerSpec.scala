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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{eisRetrieveUndertakingResponse, retrieveUndertakingEORIWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancestub.services.{EisService, JsonSchemaChecker}

import scala.concurrent.Future

class UndertakingControllerSpec extends BaseControllerSpec {

  private val controller: UndertakingController =
    app.injector.instanceOf[UndertakingController]

  "Retrieve Undertaking" must {

    val okEori = EORI("GB123456789012345")
    val internalServerErrorEori = EORI("GB123456789012999")
    val notFoundEori = EORI("GB123456789012888") // TODO remember this returns a 107 in the ResponseCommon

    def validRetrieveUndertakingBody(eori: EORI): JsValue =
      Json.toJson(eori)(retrieveUndertakingEORIWrites)

    def fakeRetrieveUndertakingPost(body: JsValue): FakeRequest[JsValue] =
      FakeRequest("POST", "/scp/retrieveundertaking/v1", fakeHeaders, body)

    "return an Undertaking for a valid request" in {
      val result: Future[Result] =
        controller.retrieve.apply(
          fakeRetrieveUndertakingPost(validRetrieveUndertakingBody(okEori))
        )
      JsonSchemaChecker[JsValue](
        contentAsJson(result),
        "retrieveUndertakingResponse"
      ).isSuccess mustEqual true
      status(result) mustEqual  play.api.http.Status.OK
    }

    "return 403 (weirdly) if the request payload is not valid" in {
      val result: Future[Result] =
        controller.retrieve.apply(
          fakeRetrieveUndertakingPost(Json.obj("foo" -> "bar"))
        )
      JsonSchemaChecker[JsValue](
        contentAsJson(result),
        "errorDetailResponse"
      ).isSuccess mustEqual true
      status(result) mustEqual  play.api.http.Status.FORBIDDEN
    }

    "return 500 if the EORI ends in 999 " in {
      val result: Future[Result] =
        controller.retrieve.apply(
          fakeRetrieveUndertakingPost(
            validRetrieveUndertakingBody(
              internalServerErrorEori
            )
          )
        )
      status(result) mustEqual  play.api.http.Status.INTERNAL_SERVER_ERROR
    }

  }

}
