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

import cats.implicits._
import play.api.libs.json._
import play.api.mvc.{Action, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital.{EisBadResponseException, retrieveUndertakingEORIWrites, undertakingFormat, updateUndertakingWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisAmendmentType, EisParamName, EisParamValue, EisStatus, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances.arbContactDetails

import scala.concurrent.Future

class UndertakingControllerSpec extends BaseControllerSpec {

  private val controller: UndertakingController =
    app.injector.instanceOf[UndertakingController]
  val internalServerErrorEori = EORI("GB123456789012999")
  val undertaking: Undertaking = TestInstances.arbUndertakingForCreate.arbitrary.sample.get

  "Create Undertaking" must {

    def validCreateUndertakingBody(undertaking: Undertaking): JsValue = {
      Json.toJson(undertaking)(undertakingFormat.writes)
    }

    def fakeCreateUndertakingPost(body: JsValue): FakeRequest[JsValue] =
      FakeRequest("POST", "/scp/createundertaking/v1", fakeHeaders, body)

    def undertakingWithEori(eori: EORI) =
      undertaking.copy(undertakingBusinessEntity =
        List(
          BusinessEntity(
            eori,
            leadEORI = true,
            arbContactDetails.arbitrary.sample.get.some
          )
        )
      )

    "return 200 and an undertakingRef for a valid createUndertaking request" in {
       val result: Future[Result] = controller.create.apply(
          fakeCreateUndertakingPost(validCreateUndertakingBody(undertaking))
        )
      checkJson(contentAsJson(result), "createUndertakingResponse")
      status(result) mustEqual  play.api.http.Status.OK
    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      val result: Future[Result] =
        controller.create.apply(
          fakeCreateUndertakingPost(Json.obj("foo" -> "bar"))
        )
      checkJson(contentAsJson(result), "errorDetailResponse")
      status(result) mustEqual  play.api.http.Status.FORBIDDEN
    }

    "return 500 if the BusinessEntity.EORI ends in 999 " in {
      val duffUndertaking: Undertaking = undertakingWithEori(internalServerErrorEori)
      val result: Future[Result] =
        controller.create.apply(
          fakeCreateUndertakingPost(
            validCreateUndertakingBody(
              duffUndertaking
            )
          )
        )
      checkJson(contentAsJson(result), "errorDetailResponse")
      status(result) mustEqual  play.api.http.Status.INTERNAL_SERVER_ERROR
    }


    def notOkCreateUndertakingResponseCheck(undertaking: Undertaking, responseCode: String) = {
      val result: Future[Result] = {
        controller.create.apply(
          fakeCreateUndertakingPost(
            validCreateUndertakingBody(
              undertaking
            )
          )
        )
      }
      val json = contentAsJson(result)
      (json \ "createUndertakingResponse" \ "responseCommon" \ "status").as[String] mustEqual
        EisStatus.NOT_OK.toString

      (json \ "createUndertakingResponse" \ "responseCommon" \ "returnParameters").as[List[Params]].head mustEqual
        Params(EisParamName.ERRORCODE, EisParamValue(responseCode))
      checkJson(contentAsJson(result), "createUndertakingResponse")
      status(result) mustEqual play.api.http.Status.OK
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if BusinessEntity.EORI ends in 888 (duplicate acknowledgementRef)" in {
      val duffUndertaking: Undertaking = undertakingWithEori(EORI("GB123456789012888"))

      notOkCreateUndertakingResponseCheck(duffUndertaking, "004")
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 101 " +
      "if BusinessEntity.EORI ends in 777 (EORI associated with another Undertaking)" in {
      val eoriAssocUndertaking: Undertaking = undertakingWithEori(EORI("GB123456789012777"))

      notOkCreateUndertakingResponseCheck(eoriAssocUndertaking, "101")
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 102 if " +
      "BusinessEntity.EORI ends in 666 (invalid EORI number)" in {
      val invalidEoriUndertaking: Undertaking = undertakingWithEori(EORI("GB123456789012666"))

      notOkCreateUndertakingResponseCheck(invalidEoriUndertaking, "102")
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 113 if " +
      "BusinessEntity.EORI ends in 555 (Postcode missing for the address)" in {
      val postcodeMissingUndertaking: Undertaking = undertakingWithEori(EORI("GB123456789012555"))

      notOkCreateUndertakingResponseCheck(postcodeMissingUndertaking, "113")
    }
  }

  "Retrieve Undertaking" must {

    implicit val path: String = "/scp/retrieveundertaking/v1"
    implicit val action: Action[JsValue] = controller.retrieve
    val okEori = EORI("GB123456789012345")
    val notFoundEori = EORI("GB123456789012888")

    "return 200 and an Undertaking for a valid request" in {
      val result: Future[Result] = testResponse[EORI](
        okEori,
        "retrieveUndertakingResponse",
        play.api.http.Status.OK
      )

      // TODO this next test should live on the BE
      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true
    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        play.api.http.Status.FORBIDDEN
      )
    }

    "return 500 if the EORI ends in 999 " in {
      testResponse[EORI](
        internalServerErrorEori,
        "errorDetailResponse",
        play.api.http.Status.INTERNAL_SERVER_ERROR
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if EORI ends in 888 (not found)" in {
      val result: Future[Result] = testResponse[EORI](
        notFoundEori,
        "retrieveUndertakingResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_)\\"status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_)\\"paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )

      // TODO this next test should live on the BE
      intercept[EisBadResponseException] {
        Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      }
    }
  }

  "amend Undertaking" must {

    implicit val path: String = "/scp/updateundertaking/v1"
    implicit val action: Action[JsValue] = controller.update
    implicit val writes: Writes[Undertaking] = updateUndertakingWrites(EisAmendmentType.A)

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        play.api.http.Status.FORBIDDEN
      )
    }

    "return 500 if the undertakingRef ends in 999 " in {
      testResponse[Undertaking](
        undertaking.copy(reference = Some(UndertakingRef("999"))),
        "errorDetailResponse",
        play.api.http.Status.INTERNAL_SERVER_ERROR
      )(implicitly, writes, implicitly)
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if Undertaking.reference ends in 888 (duplicate acknowledgementRef)" in {
      testResponse[Undertaking](
        undertaking.copy(reference = Some(UndertakingRef("888"))),
        "updateUndertakingResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_)\\"status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_)\\"paramValue" mustEqual
            List(JsString("004"), JsString("Duplicate submission acknowledgment reference"))
        )
      )(implicitly, writes, implicitly)
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 116 " +
      "if Undertaking.reference ends in 777 (bad acknowledgementRef)" in {
      val badId = "777"
      testResponse[Undertaking](
        undertaking.copy(reference = Some(UndertakingRef(badId))),
        "updateUndertakingResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_)\\"status" mustEqual
            List(JsString("NOT_OK")),
          contentAsJson(_)\\"paramValue" mustEqual
            List(JsString("116"), JsString(s"Invalid Undertaking ID $badId"))
        )
      )(implicitly, writes, implicitly)
    }

    "return 200 and a valid response for a successful amend" in {
      testResponse[Undertaking](
        undertaking,
        "updateUndertakingResponse",
        play.api.http.Status.OK,
      )(implicitly, writes, implicitly)
    }

    "return 200 and a valid response for a successful disable" in {
      testResponse[Undertaking](
        undertaking,
        "updateUndertakingResponse",
        play.api.http.Status.OK,
      )(implicitly, updateUndertakingWrites(EisAmendmentType.D), implicitly)
    }

  }
}
