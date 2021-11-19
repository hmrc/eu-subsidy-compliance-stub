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

import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital.{EisBadResponseException, retrieveUndertakingEORIWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisParamName, EisParamValue, EisStatus}
import uk.gov.hmrc.eusubsidycompliancestub.services.JsonSchemaChecker

import scala.concurrent.Future

class UndertakingControllerSpec extends BaseControllerSpec {

  private val controller: UndertakingController =
    app.injector.instanceOf[UndertakingController]

  "Retrieve Undertaking" must {

    val okEori = EORI("GB123456789012345")
    val internalServerErrorEori = EORI("GB123456789012999")
    val notFoundEori = EORI("GB123456789012888")

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
      // TODO this next test should live on the BE
      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true
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
      JsonSchemaChecker[JsValue](
        contentAsJson(result),
        "errorDetailResponse"
      ).isSuccess mustEqual true
      status(result) mustEqual  play.api.http.Status.INTERNAL_SERVER_ERROR
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if EORI ends in 888 (not found)" in {
      val result: Future[Result] =
        controller.retrieve.apply(
          fakeRetrieveUndertakingPost(
            validRetrieveUndertakingBody(
              notFoundEori
            )
          )
        )
      val json = contentAsJson(result)
      (json \ "retrieveUndertakingResponse" \ "responseCommon" \ "status").as[String] mustEqual
        EisStatus.NOT_OK.toString
      (json \ "retrieveUndertakingResponse" \ "responseCommon" \ "returnParameters").as[List[Params]].head mustEqual
        Params(EisParamName.ERRORCODE, EisParamValue("107"))
      JsonSchemaChecker[JsValue](
        json,
        "retrieveUndertakingResponse"
      ).isSuccess mustEqual true
      status(result) mustEqual  play.api.http.Status.OK
      // TODO this next test should live on the BE
      intercept[EisBadResponseException] {
        Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      }
    }
  }
}
