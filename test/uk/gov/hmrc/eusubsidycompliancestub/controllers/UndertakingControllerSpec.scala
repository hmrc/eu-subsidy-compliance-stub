/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.{Action, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital.{createUndertakingRequestWrites, retrieveUndertakingEORIWrites, undertakingFormat, updateUndertakingWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisAmendmentType, EisParamName, EisParamValue, EisStatus, IndustrySectorLimit, Sector, SubsidyAmount, UndertakingName, UndertakingRef, UndertakingStatus}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, CreateUndertakingRequest, Undertaking, UndertakingBusinessEntityUpdate}
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances.arbContactDetails
import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses.{GetUndertakingBalanceApiResponse, UndertakingBalance}
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingrequest.GetUndertakingBalanceRequest
import uk.gov.hmrc.eusubsidycompliancestub.services.EscService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import uk.gov.hmrc.eusubsidycompliancestub.BaseSpec
import play.api.http.{Status => HttpStatus}

import scala.concurrent.{ExecutionContext, Future}

class UndertakingControllerSpec extends BaseSpec {

  private val mockEscService = mock[EscService]
  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val controller: UndertakingController = new UndertakingController(
    escService = mockEscService,
    cc = app.injector.instanceOf[ControllerComponents],
    authAndEnvAction = app.injector.instanceOf[AuthAndEnvAction]
  )(appConfig = appConfig, ec = ExecutionContext.global)

  val internalServerErrorEori: EORI = EORI("GB123456789012999")
  val undertaking: Undertaking = TestInstances.arbUndertakingForCreate.arbitrary.sample.get
  val businessEntityUpdates: UndertakingBusinessEntityUpdate =
    TestInstances.arbUndertakingBusinessEntityUpdate.arbitrary.sample.get

  def undertakingWithEori(eori: EORI): Undertaking =
    undertaking.copy(undertakingBusinessEntity =
      List(
        BusinessEntity(
          eori,
          leadEORI = true,
          arbContactDetails.arbitrary.sample.get.some
        )
      )
    )

  "Create Undertaking" must {

    def validCreateUndertakingRequestBody(undertaking: CreateUndertakingRequest): JsValue =
      Json.toJson(undertaking)(createUndertakingRequestWrites)

    def validCreateUndertakingBody(undertaking: Undertaking): JsValue =
      Json.toJson(undertaking)(undertakingFormat.writes(_))

    def fakeCreateUndertakingPost(body: JsValue): FakeRequest[JsValue] =
      FakeRequest("POST", "/scp/createundertaking/v1", fakeHeaders, body)

    "return 200 and an undertakingRef for a valid createUndertaking request" in {
      val createUndertakingRequest = CreateUndertakingRequest(
        name = undertaking.name,
        industrySector = undertaking.industrySector,
        businessEntity = undertaking.undertakingBusinessEntity
      )

      when(mockEscService.retrieveUndertaking(any())).thenReturn(Future.successful(None))
      when(mockEscService.createUndertaking(any(), any())).thenReturn(Future.successful(UndertakingRef("some-ref")))

      val result: Future[Result] = controller.create.apply(
        fakeCreateUndertakingPost(validCreateUndertakingRequestBody(createUndertakingRequest))
      )
      status(result) mustEqual HttpStatus.OK
      checkJson(contentAsJson(result), "createUndertakingResponse")

      verify(mockEscService, times(1)).retrieveUndertaking(any())
      verify(mockEscService, times(1)).createUndertaking(any(), any())
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 101 if EORI associated with another Undertaking" in {
      val eori = EORI("GB123456789012000")
      val eoriAssocUndertaking: Undertaking = undertakingWithEori(eori)
      when(mockEscService.retrieveUndertaking(eori)) thenReturn Future.successful(Some(eoriAssocUndertaking))
      notOkCreateUndertakingResponseCheck(eoriAssocUndertaking, "101")
    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      val result = controller.create.apply(
        fakeCreateUndertakingPost(Json.obj("foo" -> "bar"))
      )
      checkJson(contentAsJson(result), "errorDetailResponse")
      status(result) mustEqual HttpStatus.FORBIDDEN
    }

    "return 500 if the BusinessEntity.EORI ends in 999 " in {
      val duffUndertaking: Undertaking = undertakingWithEori(internalServerErrorEori)
      when(mockEscService.retrieveUndertaking(internalServerErrorEori)) thenReturn
        Future.successful(Some(duffUndertaking))
      val result = controller.create.apply(
        fakeCreateUndertakingPost(
          validCreateUndertakingBody(
            duffUndertaking
          )
        )
      )
      checkJson(contentAsJson(result), "errorDetailResponse")
      status(result) mustEqual HttpStatus.INTERNAL_SERVER_ERROR
    }

    def notOkCreateUndertakingResponseCheck(undertaking: Undertaking, responseCode: String) = {
      val result: Future[Result] =
        controller.create.apply(
          fakeCreateUndertakingPost(
            validCreateUndertakingBody(
              undertaking
            )
          )
        )
      val json = contentAsJson(result)
      (json \ "createUndertakingResponse" \ "responseCommon" \ "status").as[String] mustEqual
        EisStatus.NOT_OK.toString

      (json \ "createUndertakingResponse" \ "responseCommon" \ "returnParameters").as[List[Params]].head mustEqual
        Params(EisParamName.ERRORCODE, EisParamValue(responseCode))
      checkJson(contentAsJson(result), "createUndertakingResponse")
      status(result) mustEqual HttpStatus.OK
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
    val invalidEORI = EORI("GB123456789012777")

    "return 200 and an Undertaking for a valid request" in {
      when(mockEscService.retrieveUndertaking(any()))
        .thenReturn(Future.successful(Some(undertakingWithEori(okEori))))

      val result: Future[Result] = testResponse[EORI](
        okEori,
        "retrieveUndertakingResponse",
        HttpStatus.OK
      )

      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true
    }

    "return 200 and an Undertaking with status 'suspendedAutomated' when eori ends with 511" in {
      val eoriNumber = EORI("GB123456789012511")

      when(mockEscService.retrieveUndertaking(any()))
        .thenReturn(Future.successful(Some(undertakingWithEori(eoriNumber))))
      val result: Future[Result] = testResponse[EORI](
        eoriNumber,
        "retrieveUndertakingResponse",
        HttpStatus.OK
      )

      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true
      u.get.undertakingStatus mustEqual Some(UndertakingStatus.suspendedAutomated.id)
    }

    "return 200 and an Undertaking with status 'suspendedManual' when eori ends with 316" in {
      val eoriNumber = EORI("GB123456789012316")
      when(mockEscService.retrieveUndertaking(any()))
        .thenReturn(Future.successful(Some(undertakingWithEori(eoriNumber))))
      val result: Future[Result] = testResponse[EORI](
        eoriNumber,
        "retrieveUndertakingResponse",
        HttpStatus.OK
      )

      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true
      u.get.undertakingStatus mustEqual Some(UndertakingStatus.suspendedManual.id)
    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        HttpStatus.FORBIDDEN
      )
    }

    "return 500 if the EORI ends in 999 " in {
      testResponse[EORI](
        internalServerErrorEori,
        "errorDetailResponse",
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if EORI ends in 888 (not found)" in {
      val result: Future[Result] = testResponse[EORI](
        notFoundEori,
        "retrieveUndertakingResponse",
        HttpStatus.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )

      val undertakingResult = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      undertakingResult.isError mustBe true
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if not found" in {
      when(mockEscService.retrieveUndertaking(any())).thenReturn(Future.successful(None))
      val result: Future[Result] = testResponse[EORI](
        okEori,
        "retrieveUndertakingResponse",
        HttpStatus.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )

      val undertakingResult = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      undertakingResult.isError mustBe true
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 055 if EORI is invalid" in {
      val result: Future[Result] = testResponse[EORI](
        invalidEORI,
        "retrieveUndertakingResponse",
        HttpStatus.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("055"), JsString("ID number missing or invalid"))
        )
      )

      val undertakingResult = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      undertakingResult.isError mustBe true
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
        HttpStatus.FORBIDDEN
      )
    }

    "return 500 if the undertakingRef ends in 999 " in {
      testResponse[Undertaking](
        undertaking.copy(reference = UndertakingRef("999")),
        "errorDetailResponse",
        HttpStatus.INTERNAL_SERVER_ERROR
      )(implicitly, writes, implicitly)
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if Undertaking.reference ends in 888 (duplicate acknowledgementRef)" in {
        testResponse[Undertaking](
          undertaking.copy(reference = UndertakingRef("888")),
          "updateUndertakingResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("004"), JsString("Duplicate submission acknowledgment reference"))
          )
        )(implicitly, writes, implicitly)
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 116 " +
      "if Undertaking.reference ends in 777 (bad acknowledgementRef)" in {
        val badId = "777"
        testResponse[Undertaking](
          undertaking.copy(reference = UndertakingRef(badId)),
          "updateUndertakingResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("116"), JsString(s"Invalid Undertaking ID $badId"))
          )
        )(implicitly, writes, implicitly)
      }

    "return 200 and a valid response for a successful amend" in {
      when(mockEscService.updateUndertaking(any(), any(), any())(any())).thenReturn(Future.successful(()))

      // Changing the sector should also change the sector limit.
      val editedUndertaking = undertaking.copy(
        name = UndertakingName("EDITED NAME"),
        industrySector = Sector.other,
        industrySectorLimit = IndustrySectorLimit(200000.00)
      )

      testResponse[Undertaking](
        editedUndertaking,
        "updateUndertakingResponse",
        HttpStatus.OK
      )(implicitly, writes, implicitly)
      verify(mockEscService, times(1)).updateUndertaking(any(), any(), any())(any())
    }

    "return 200 and a valid response for a successful disable" in {
      when(mockEscService.updateUndertaking(any(), any(), any())(any())).thenReturn(Future.successful(()))
      testResponse[Undertaking](
        undertaking,
        "updateUndertakingResponse",
        HttpStatus.OK
      )(implicitly, updateUndertakingWrites(EisAmendmentType.D), implicitly)
      verify(mockEscService, times(2)).updateUndertaking(any(), any(), any())(any())
    }

  }

  "amend Undertaking.undertakingBusinessEntity" must {

    implicit val path: String = "/scp/amendundertakingmemberdata/v1 "
    implicit val action: Action[JsValue] = controller.amendUndertakingMemberData

    val eori = businessEntityUpdates.businessEntityUpdates.head.businessEntity.businessEntityIdentifier

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        HttpStatus.FORBIDDEN
      )
    }

    "return 500 if the undertakingRef ends in 999 " in {
      testResponse[UndertakingBusinessEntityUpdate](
        businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("999")),
        "errorDetailResponse",
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if Undertaking.reference ends in 888 (duplicate acknowledgementRef)" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("888")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("004"), JsString("Duplicate submission acknowledgment reference"))
          )
        )
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 106 " +
      "if Undertaking.reference ends in 777" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("777")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("106"), JsString(s"EORI not Subscribed in ETMP $eori"))
          )
        )
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 " +
      "if Undertaking.reference ends in 666" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("666")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
          )
        )
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 108 " +
      "if Undertaking.reference ends in 555" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("555")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("108"), JsString(s"Relationship with another undertaking exist for EORI $eori"))
          )
        )
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 109 " +
      "if Undertaking.reference ends in 444" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("444")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("109"), JsString(s"Relationship does not exist for EORI $eori"))
          )
        )
      }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 110 " +
      "if Undertaking.reference ends in 333" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("333")),
          "amendUndertakingMemberDataResponse",
          HttpStatus.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("110"), JsString(s"Subsidy Compliance address does not exist for EORI $eori"))
          )
        )
      }

    "return 200 and a valid response for a successful amend" in {
      when(mockEscService.updateUndertakingBusinessEntities(any(), any())).thenReturn(Future.successful(()))
      testResponse[UndertakingBusinessEntityUpdate](
        businessEntityUpdates,
        "amendUndertakingMemberDataResponse",
        HttpStatus.OK
      )
    }
  }

  "Get undertaking balance" must {

    implicit val path: String = "/scp/getsamundertakingbalance/v1"
    implicit val action: Action[JsValue] = controller.getUndertakingBalance
    val okEori = EORI("GB123456789012345")
    val okRequest = GetUndertakingBalanceRequest(eori = Some(okEori))
    val notFoundEori = EORI("GB123456789012888")
    val notFoundEoriRequest = GetUndertakingBalanceRequest(eori = Some(notFoundEori))
    val industrySectorLimit = IndustrySectorLimit(20000)
    val undertakingRef = UndertakingRef("UR123456")

    val balance = UndertakingBalance(
      undertakingIdentifier = undertakingRef,
      nonHMRCSubsidyAllocationEUR = None,
      hmrcSubsidyAllocationEUR = None,
      industrySectorLimit = industrySectorLimit,
      totalEUR = SubsidyAmount(19800.00),
      totalGBP = SubsidyAmount(16500.00),
      conversionRate = SubsidyAmount(1.2)
    )

    "return 200 and the undertaking balance for a valid request" in {
      when(mockEscService.getUndertakingBalance(any())).thenReturn(Future.successful(Some(balance)))

      val result: Future[Result] = testResponse[GetUndertakingBalanceRequest](
        okRequest,
        "getUndertakingBalanceResponse",
        HttpStatus.OK
      )

      val u: JsResult[GetUndertakingBalanceApiResponse] =
        Json.fromJson[GetUndertakingBalanceApiResponse](contentAsJson(result))
      u.isSuccess mustEqual true
      val response = u.get
      val undertakingBalance = response.getUndertakingBalanceResponse.get
      undertakingBalance.undertakingIdentifier mustEqual undertakingRef
      undertakingBalance.industrySectorLimit mustEqual industrySectorLimit
      undertakingBalance.conversionRate mustEqual SubsidyAmount(1.2)
      undertakingBalance.availableBalanceEUR mustEqual SubsidyAmount(200.00)
      undertakingBalance.availableBalanceGBP mustEqual SubsidyAmount(166.67)
      undertakingBalance.nationalCapBalanceEUR mustEqual industrySectorLimit

    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        HttpStatus.FORBIDDEN
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if EORI not found" in {
      when(mockEscService.getUndertakingBalance(any())).thenReturn(Future.successful(None))
      testResponse[GetUndertakingBalanceRequest](
        notFoundEoriRequest,
        "getUndertakingBalanceResponse",
        HttpStatus.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 500 Undertaking doesn't exist if eori ends with 111908" in {

      val eoriNumber = EORI("GB123456789111908")
      val getUndertakingBalanceRequest = GetUndertakingBalanceRequest(eori = Some(eoriNumber))

      testResponse[GetUndertakingBalanceRequest](
        getUndertakingBalanceRequest,
        "getUndertakingBalanceResponse",
        HttpStatus.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("500"), JsString("Undertaking doesn't exist"))
        )
      )
    }
  }
}
