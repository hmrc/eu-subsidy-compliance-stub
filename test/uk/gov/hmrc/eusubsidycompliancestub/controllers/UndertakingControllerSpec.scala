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
import play.api.mvc.{Action, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital
import uk.gov.hmrc.eusubsidycompliancestub.models.json.digital.{EisBadResponseException, createUndertakingRequestWrites, retrieveUndertakingEORIWrites, undertakingFormat, updateUndertakingWrites}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.Params
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{DeclarationID, EORI, EisAmendmentType, EisParamName, EisParamValue, EisStatus, EisSubsidyAmendmentType, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, TaxType, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, CreateUndertakingRequest, HmrcSubsidy, NonHmrcSubsidy, Undertaking, UndertakingBusinessEntityUpdate, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances.arbContactDetails
import org.scalactic.Equality
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses.GetUndertakingBalanceApiResponse
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingrequest.GetUndertakingBalanceRequest
import uk.gov.hmrc.eusubsidycompliancestub.services.Store

import java.time.LocalDate
import scala.concurrent.Future

class UndertakingControllerSpec extends BaseControllerSpec {

  private val controller: UndertakingController =
    app.injector.instanceOf[UndertakingController]
  val internalServerErrorEori = EORI("GB123456789012999")
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
      Json.toJson(undertaking)(createUndertakingRequestWrites.writes)

    def validCreateUndertakingBody(undertaking: Undertaking): JsValue =
      Json.toJson(undertaking)(undertakingFormat.writes)

    def fakeCreateUndertakingPost(body: JsValue): FakeRequest[JsValue] =
      FakeRequest("POST", "/scp/createundertaking/v1", fakeHeaders, body)

    // the created/stored undertaking is not entirely equal to the one sent by digital
    implicit val eq = new Equality[Undertaking] {
      override def areEqual(a: Undertaking, b: Any): Boolean = b match {
        case u: Undertaking =>
          u.name == a.name &&
            u.industrySector == a.industrySector &&
            u.undertakingBusinessEntity == a.undertakingBusinessEntity
        case _ =>
          false
      }
    }

    "return 200 and an undertakingRef for a valid createUndertaking request" in {
      val leadEori = undertaking.undertakingBusinessEntity.filter(_.leadEORI == true).head.businessEntityIdentifier
      val createUndertakingRequest = CreateUndertakingRequest(
        name = undertaking.name,
        industrySector = undertaking.industrySector,
        businessEntity = undertaking.undertakingBusinessEntity
      )

      val result: Future[Result] = controller.create.apply(
        fakeCreateUndertakingPost(validCreateUndertakingRequestBody(createUndertakingRequest))
      )
      status(result) mustEqual play.api.http.Status.OK
      checkJson(contentAsJson(result), "createUndertakingResponse")

      val storedUndertakingOpt = Store.undertakings.retrieveByLeadEoriAndName(leadEori, createUndertakingRequest.name)
      val storedUndertaking = storedUndertakingOpt.get

      storedUndertaking.industrySector mustEqual createUndertakingRequest.industrySector
      storedUndertaking.name mustEqual createUndertakingRequest.name
      storedUndertaking.undertakingBusinessEntity mustEqual createUndertakingRequest.businessEntity

    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 101 " +
      "if EORI associated with another Undertaking in the Store" in {
        val eoriAssocUndertaking: Undertaking = undertakingWithEori(EORI("GB123456789012000"))
        Store.undertakings.put(eoriAssocUndertaking)
        notOkCreateUndertakingResponseCheck(eoriAssocUndertaking, "101")
        Store.clear()
      }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      val result: Future[Result] =
        controller.create.apply(
          fakeCreateUndertakingPost(Json.obj("foo" -> "bar"))
        )
      checkJson(contentAsJson(result), "errorDetailResponse")
      status(result) mustEqual play.api.http.Status.FORBIDDEN
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
      status(result) mustEqual play.api.http.Status.INTERNAL_SERVER_ERROR
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
    val invalidEORI = EORI("GB123456789012777")

    "return 200 and an Undertaking for a valid request" in {
      Store.undertakings.put(undertakingWithEori(okEori))
      val result: Future[Result] = testResponse[EORI](
        okEori,
        "retrieveUndertakingResponse",
        play.api.http.Status.OK
      )

      // TODO this next test should live on the BE
      val u: JsResult[Undertaking] = Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      u.isSuccess mustEqual true

      Store.clear()
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
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )

      // TODO this next test should live on the BE
      intercept[EisBadResponseException] {
        Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      }
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if not found in Store" in {
      val result: Future[Result] = testResponse[EORI](
        okEori,
        "retrieveUndertakingResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )

      // TODO this next test should live on the BE
      intercept[EisBadResponseException] {
        Json.fromJson[Undertaking](contentAsJson(result))(digital.undertakingFormat)
      }
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 055 if EORI is invalid" in {
      val result: Future[Result] = testResponse[EORI](
        invalidEORI,
        "retrieveUndertakingResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("055"), JsString("ID number missing or invalid"))
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
        undertaking.copy(reference = UndertakingRef("999")),
        "errorDetailResponse",
        play.api.http.Status.INTERNAL_SERVER_ERROR
      )(implicitly, writes, implicitly)
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if Undertaking.reference ends in 888 (duplicate acknowledgementRef)" in {
        testResponse[Undertaking](
          undertaking.copy(reference = UndertakingRef("888")),
          "updateUndertakingResponse",
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("116"), JsString(s"Invalid Undertaking ID $badId"))
          )
        )(implicitly, writes, implicitly)
      }

    "return 200 and a valid response for a successful amend" in {
      Store.undertakings.put(
        undertaking.copy(
          industrySector = Sector.agriculture,
          industrySectorLimit = IndustrySectorLimit(20000.00)
        )
      )

      // Changing the sector should also change the sector limit.
      val editedUndertaking = undertaking.copy(
        name = UndertakingName("EDITED NAME"),
        industrySector = Sector.other,
        industrySectorLimit = IndustrySectorLimit(200000.00)
      )

      testResponse[Undertaking](
        editedUndertaking,
        "updateUndertakingResponse",
        play.api.http.Status.OK
      )(implicitly, writes, implicitly)
      Store.undertakings.retrieve(editedUndertaking.reference) must contain(editedUndertaking)
      Store.clear()
    }

    "return 200 and a valid response for a successful disable" in {
      Store.undertakings.put(undertaking)
      testResponse[Undertaking](
        undertaking,
        "updateUndertakingResponse",
        play.api.http.Status.OK
      )(implicitly, updateUndertakingWrites(EisAmendmentType.D), implicitly)
      Store.undertakings.retrieve(undertaking.reference) mustEqual None
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
        play.api.http.Status.FORBIDDEN
      )
    }

    "return 500 if the undertakingRef ends in 999 " in {
      testResponse[UndertakingBusinessEntityUpdate](
        businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("999")),
        "errorDetailResponse",
        play.api.http.Status.INTERNAL_SERVER_ERROR
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 004 " +
      "if Undertaking.reference ends in 888 (duplicate acknowledgementRef)" in {
        testResponse[UndertakingBusinessEntityUpdate](
          businessEntityUpdates.copy(undertakingIdentifier = UndertakingRef("888")),
          "amendUndertakingMemberDataResponse",
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
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
          play.api.http.Status.OK,
          List(
            contentAsJson(_) \\ "status" mustEqual
              List(JsString("NOT_OK")),
            contentAsJson(_) \\ "paramValue" mustEqual
              List(JsString("110"), JsString(s"Subsidy Compliance address does not exist for EORI $eori"))
          )
        )
      }

    "return 200 and a valid response for a successful amend" in {
      val ref: UndertakingRef = businessEntityUpdates.undertakingIdentifier
      val u: Undertaking = undertaking.copy(reference = ref)
      Store.undertakings.put(u)
      testResponse[UndertakingBusinessEntityUpdate](
        businessEntityUpdates,
        "amendUndertakingMemberDataResponse",
        play.api.http.Status.OK
      )
      Store.clear()
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
    val subsidyAmount = SubsidyAmount(BigDecimal(100.00))
    val nonHmrcSubsidyAmount = SubsidyAmount(BigDecimal(500.00))
    val declarationId = DeclarationID("12345")
    val fixedDate = LocalDate.now().minusDays(20)

    val hmrcSubsidy = HmrcSubsidy(
      declarationID = declarationId,
      issueDate = Some(fixedDate),
      acceptanceDate = fixedDate,
      declarantEORI = okEori,
      consigneeEORI = okEori,
      taxType = Some(TaxType("1")),
      hmrcSubsidyAmtGBP = Some(subsidyAmount),
      hmrcSubsidyAmtEUR = Some(subsidyAmount),
      tradersOwnRefUCR = None
    )

    val nonHmrcSubsidy = NonHmrcSubsidy(
      subsidyUsageTransactionId = Some(SubsidyRef("AB12345")),
      allocationDate = LocalDate.of(2022, 1, 1),
      submissionDate = fixedDate,
      publicAuthority = Some("Local Authority"),
      traderReference = None,
      nonHMRCSubsidyAmtEUR = nonHmrcSubsidyAmount,
      businessEntityIdentifier = Some(okEori),
      amendmentType = Some(EisSubsidyAmendmentType("1"))
    )

    val undertakingSubsidies = UndertakingSubsidies(
      undertakingIdentifier = undertakingRef,
      nonHMRCSubsidyTotalEUR = subsidyAmount,
      nonHMRCSubsidyTotalGBP = subsidyAmount,
      hmrcSubsidyTotalEUR = subsidyAmount,
      hmrcSubsidyTotalGBP = subsidyAmount,
      nonHMRCSubsidyUsage = List(nonHmrcSubsidy),
      hmrcSubsidyUsage = List(hmrcSubsidy)
    )

    val undertaking = Undertaking(
      reference = undertakingRef,
      name = UndertakingName("TestUndertaking"),
      industrySector = Sector.agriculture,
      industrySectorLimit = industrySectorLimit,
      lastSubsidyUsageUpdt = Some(LocalDate.of(2021, 1, 18)),
      undertakingBusinessEntity = List(
        BusinessEntity(
          okEori,
          leadEORI = true,
          arbContactDetails.arbitrary.sample
        )
      )
    )

    "return 200 and the undertaking balance for a valid request" in {
      Store.undertakings.put(undertaking)
      Store.subsidies.put(undertakingSubsidies)

      val result: Future[Result] = testResponse[GetUndertakingBalanceRequest](
        okRequest,
        "getUndertakingBalanceResponse",
        play.api.http.Status.OK
      )

      // TODO this next test should live on the BE
      val u: JsResult[GetUndertakingBalanceApiResponse] =
        Json.fromJson[GetUndertakingBalanceApiResponse](contentAsJson(result))
      u.isSuccess mustEqual true
      val response = u.get
      val undertakingBalance = response.getUndertakingBalanceResponse.get
      undertakingBalance.undertakingIdentifier mustEqual undertakingRef
      undertakingBalance.industrySectorLimit mustEqual industrySectorLimit
      undertakingBalance.conversionRate mustEqual SubsidyAmount(1.2)
      undertakingBalance.availableBalanceEUR mustEqual SubsidyAmount(19800.00)
      undertakingBalance.availableBalanceGBP mustEqual SubsidyAmount(16500.00)
      undertakingBalance.nationalCapBalanceEUR mustEqual industrySectorLimit

      Store.clear()
    }

    "return 403 (as per EIS spec) and a valid errorDetailResponse if the request payload is not valid" in {
      testResponse[JsValue](
        Json.obj("foo" -> "bar"),
        "errorDetailResponse",
        play.api.http.Status.FORBIDDEN
      )
    }

    "return 200 but with NOT_OK responseCommon.status and ERRORCODE 107 if EORI not found" in {
      val result: Future[Result] = testResponse[GetUndertakingBalanceRequest](
        notFoundEoriRequest,
        "getUndertakingBalanceResponse",
        play.api.http.Status.OK,
        List(
          contentAsJson(_) \\ "status" mustEqual List(JsString("NOT_OK")),
          contentAsJson(_) \\ "paramValue" mustEqual
            List(JsString("107"), JsString("Undertaking reference in the API not Subscribed in ETMP"))
        )
      )
    }
  }
}
