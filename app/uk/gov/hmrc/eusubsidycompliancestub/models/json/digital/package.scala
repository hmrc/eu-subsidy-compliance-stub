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

package uk.gov.hmrc.eusubsidycompliancestub.models.json

import cats.implicits._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{Params, RequestCommon}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisAmendmentType, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, CreateUndertakingRequest, Undertaking}

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

package object digital {

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  implicit val undertakingFormat: Format[Undertaking] = new Format[Undertaking] {
    val requestCommon: RequestCommon = RequestCommon("CreateNewUndertaking")
    // provides json for EIS createUndertaking call
    override def writes(o: Undertaking): JsValue = {
      val lead: BusinessEntity =
        o.undertakingBusinessEntity match {
          case h :: Nil => h
          case _ =>
            throw new IllegalStateException(s"unable to create undertaking with missing or multiple business entities")
        }

      Json.obj(
        "createUndertakingRequest" -> Json.obj(
          "requestCommon" -> requestCommon,
          "requestDetail" -> Json.obj(
            "undertakingName" -> o.name,
            "industrySector" -> o.industrySector,
            "businessEntity" ->
              Json.obj(
                "idType" -> "EORI",
                "idValue" -> JsString(lead.businessEntityIdentifier),
                "contacts" -> lead.contacts
              ),
            "undertakingStartDate" -> dateFormatter.format(LocalDate.now)
          )
        )
      )

    }

    // provides Undertaking from EIS retrieveUndertaking response
    override def reads(retrieveUndertakingResponse: JsValue): JsResult[Undertaking] = {
      val responseCommon: JsLookupResult =
        retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case "NOT_OK" =>
          val processingDate = (responseCommon \ "processingDate").as[ZonedDateTime]
          val statusText = (responseCommon \ "statusText").asOpt[String]
          val returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]].getOrElse(Nil)
          JsError(s"""Bad Response:
                     |  processingDate: $processingDate
                     |  statusText: $statusText
                     |  returnParameters: ${returnParameters.mkString("\n", "\n    -", "")}""".stripMargin)
        case "OK" =>
          val responseDetail: JsLookupResult =
            retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseDetail"
          val undertakingRef: String = (responseDetail \ "undertakingReference").as[String]
          val undertakingName: UndertakingName = (responseDetail \ "undertakingName").as[UndertakingName]
          val industrySector: Sector = (responseDetail \ "industrySector").as[Sector]
          val industrySectorLimit: IndustrySectorLimit =
            (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit]
          val undertakingStatus: Int = (responseDetail \ "undertakingStatus").as[Int]
          val lastSubsidyUsageUpdt: LocalDate =
            (responseDetail \ "lastSubsidyUsageUpdt").as[LocalDate]((json: JsValue) => {
              Either
                .catchNonFatal(LocalDate.parse(json.as[String], eis.oddEisDateFormat))
                .fold(
                  t => JsError(JsPath(List(KeyPathNode("lastSubsidyUsageUpdt"))), t.getMessage),
                  s => JsSuccess(s)
                )
            })
          val undertakingBusinessEntity: List[BusinessEntity] =
            (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
          JsSuccess(
            Undertaking(
              UndertakingRef(undertakingRef),
              undertakingName,
              industrySector,
              industrySectorLimit,
              lastSubsidyUsageUpdt.some,
              undertakingStatus.some,
              undertakingBusinessEntity
            )
          )
        case _ => JsError("unable to derive Error or Success from SCP04 response")
      }
    }
  }

  // provides json for EIS retrieveUndertaking call
  implicit val createUndertakingRequestWrites: Writes[CreateUndertakingRequest] = new Writes[CreateUndertakingRequest] {

    override def writes(createUndertakingRequest: CreateUndertakingRequest): JsValue = {
      val lead: BusinessEntity =
        createUndertakingRequest.businessEntity match {
          case h :: Nil => h
          case _ =>
            throw new IllegalStateException(s"unable to create undertaking with missing or multiple business entities")
        }

      Json.obj(
        "createUndertakingRequest" -> Json.obj(
          "requestCommon" -> RequestCommon("CreateNewUndertaking"),
          "requestDetail" -> Json.obj(
            "undertakingName" -> createUndertakingRequest.name,
            "industrySector" -> createUndertakingRequest.industrySector,
            "businessEntity" ->
              Json.obj(
                "idType" -> "EORI",
                "idValue" -> JsString(lead.businessEntityIdentifier),
                "contacts" -> lead.contacts
              ),
            "undertakingStartDate" -> dateFormatter.format(LocalDate.now)
          )
        )
      )
    }
  }

  // provides json for EIS retrieveUndertaking call
  implicit val retrieveUndertakingEORIWrites: Writes[EORI] = new Writes[EORI] {

    override def writes(o: EORI): JsValue = Json.obj(
      "retrieveUndertakingRequest" -> Json.obj(
        "requestCommon" -> RequestCommon("RetrieveUndertaking"),
        "requestDetail" -> Json.obj(
          "idType" -> "EORI",
          "idValue" -> o.toString
        )
      )
    )
  }

  // provides json for EIS updateUndertaking call
  def updateUndertakingWrites(
    amendmentType: EisAmendmentType = EisAmendmentType.A
  ): Writes[Undertaking] = {
    val amendUndertakingWrites: Writes[Undertaking] = new Writes[Undertaking] {

      override def writes(o: Undertaking): JsValue =
        Json.obj(
          "updateUndertakingRequest" -> Json.obj(
            "requestCommon" -> RequestCommon("UpdateUndertaking"),
            "requestDetail" -> Json.obj(
              "amendmentType" -> amendmentType,
              "undertakingId" -> o.reference,
              "undertakingName" -> o.name,
              "industrySector" -> o.industrySector,
              "disablementStartDate" -> dateFormatter.format(LocalDate.now)
            )
          )
        )
    }
    amendUndertakingWrites
  }
}
