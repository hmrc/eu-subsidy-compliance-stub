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

package uk.gov.hmrc.eusubsidycompliancestub.models.json

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{Params, RequestCommon}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, IndustrySectorLimit, Sector, UndertakingName, UndertakingRef}

package object digital {

  implicit val undertakingFormat: Format[Undertaking] = new Format[Undertaking] {

    val requestCommon = RequestCommon(
      "acknowledgementReferenceTODO", // TODO
      "CreateNewUndertaking"
    )

    // provides json for EIS createUndertaking call
    override def writes(o: Undertaking): JsValue = {
      val lead: BusinessEntity =
        o.undertakingBusinessEntity match {
          case h :: Nil => h // TODO should test they are a lead, or maybe EIS will infer that :)?
          case _ => throw new IllegalStateException(s"unable to create undertaking with missing or multiple business entities")
        }

      val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
      Json.obj(
        "createUndertakingRequest" -> Json.obj(
          "requestCommon" -> requestCommon,
          "requestDetail" -> Json.obj(
            "undertakingName" -> o.name,
            "industrySector" -> o.industrySector,
            "businessEntity" -> Json.arr(
              Json.obj(
                "idType" -> "EORI",
                "id" -> lead.businessEntityIdentifier,
                "contacts" -> lead.contacts
              )
            ),
            "undertakingStartDate" -> dateFormatter.format(LocalDate.now)
          )
        )
      )

    }

    // provides Undertaking from EIS retrieveUndertaking response
    override def reads(retrieveUndertakingResponse: JsValue): JsResult[Undertaking] = {
      val responseCommon: JsLookupResult = retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseCommon"
      (responseCommon \ "status").as[String] match {
        case "NOT_OK" =>
          val processingDate = (responseCommon \ "processingDate").as[ZonedDateTime]
          val statusText = (responseCommon \ "statusText").asOpt[String]
          val returnParameters = (responseCommon \ "returnParameters").asOpt[List[Params]]
          throw new EisBadResponseException("NOT_OK", processingDate, statusText, returnParameters)
        case "OK" =>
          val responseDetail: JsLookupResult = retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseDetail"
          val undertakingRef: Option[UndertakingRef] = (responseDetail \ "undertakingReference").asOpt[UndertakingRef]
          val undertakingName: UndertakingName = (responseDetail \ "undertakingName").as[UndertakingName]
          val industrySector: Sector = (responseDetail \ "industrySector").as[Sector]
          val industrySectorLimit: IndustrySectorLimit = (responseDetail \ "industrySectorLimit").as[IndustrySectorLimit]
          val lastSubsidyUsageUpdt: LocalDate = (responseDetail \ "lastSubsidyUsageUpdt").as[LocalDate](new Reads[LocalDate] {
            override def reads(json: JsValue): JsResult[LocalDate] =
              JsSuccess(LocalDate.parse(json.as[String], eis.oddEisDateFormat))
          })
          val undertakingBusinessEntity: List[BusinessEntity] = (responseDetail \ "undertakingBusinessEntity").as[List[BusinessEntity]]
          JsSuccess(
            Undertaking(
              undertakingRef,
              undertakingName,
              industrySector,
              industrySectorLimit,
              lastSubsidyUsageUpdt,
              undertakingBusinessEntity
            )
          )
        case _ => JsError("unable to derive Error or Success from SCP04 response")
      }
    }
  }

  // provides json for EIS retrieveUndertaking call
  implicit val retrieveUndertakingEORIWrites: Writes[EORI] = new Writes[EORI] {
    val requestCommon = RequestCommon(
      "acknowledgementReference".padTo(32,'X'), // TODO find out what this ref is supposed to look like
      "RetrieveUndertaking"
    )

    override def writes(o: EORI): JsValue = Json.obj(
      "retrieveUndertakingRequest" -> Json.obj(
        "requestCommon" -> requestCommon,
        "requestDetail" -> Json.obj(
          "idType" -> "EORI",
          "idValue" -> o.toString
        )
      )
    )
  }


}
