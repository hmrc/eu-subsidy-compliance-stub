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

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, LocalDateTime}

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, EisStatus, EisStatusString, IndustrySectorLimit, Sector, UndertakingName, UndertakingRef}

package object eis {

  val clock = Clock.systemUTC()
  val formatter = DateTimeFormatter.ISO_INSTANT

  def receiptDate: String = {
    val instant = Instant.now(clock)
    val withoutNanos = instant.minusNanos(instant.getNano)
    formatter.format(withoutNanos)
  }

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
    // TODO handle responseCommon
    override def reads(retrieveUndertakingResponse: JsValue): JsResult[Undertaking] = {
      val json: JsLookupResult = retrieveUndertakingResponse \ "retrieveUndertakingResponse" \ "responseDetail"
      val undertakingRef: Option[UndertakingRef] = (json \ "undertakingReference").asOpt[UndertakingRef]
      val undertakingName: UndertakingName = (json \ "undertakingName").as[UndertakingName]
      val industrySector: Sector = (json \ "industrySector").as[Sector]
      val industrySectorLimit: IndustrySectorLimit = (json \ "industrySectorLimit").as[IndustrySectorLimit]
      val lastSubsidyUsageUpdt: LocalDate = (json \ "lastSubsidyUsageUpdt").as[LocalDate]
      val undertakingBusinessEntity: List[BusinessEntity] = (json \ "undertakingBusinessEntity").as[List[BusinessEntity]]
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

  // provides response for EIS retrieveUndertaking call
  implicit val eisRetrieveUndertakingResponse: Writes[Undertaking] = new Writes[Undertaking] {
    val oddEisDateFormat = DateTimeFormatter.ofPattern("YYYY/MM-dd")
    override def writes(o: Undertaking): JsValue = Json.obj(
      "retrieveUndertakingResponse" -> Json.obj(
        "responseCommon" -> ResponseCommon(EisStatus.OK, EisStatusString("ok"), LocalDateTime.now, List.empty[Params]),
        "responseDetail" -> Json.obj(
          "undertakingReference" ->  o.reference,
          "undertakingName" -> o.name,
          "industrySector" -> o.industrySector,
          "industrySectorLimit" -> o.industrySectorLimit,
          "lastSubsidyUsageUpdt" -> o.lastSubsidyUsageUpdt.format(oddEisDateFormat),
          "undertakingBusinessEntity" -> o.undertakingBusinessEntity
        )
      )
    )
  }

  val retUndResp: JsValue = Json.parse(
    """
      |{
      |  "retrieveUndertakingResponse": {
      |    "responseCommon": {
      |      "status": "OK",
      |      "processingDate": "3446-92-08T17:31:33Z",
      |      "statusText": "ABCDEFGHIJKLMNOPQRSTUVWXY",
      |      "returnParameters": []
      |    },
      |    "responseDetail": {
      |      "undertakingReference": "ABCDE",
      |      "undertakingName": "ABCDEFGHIJKLMNOPQRSTUV",
      |      "industrySector": "0",
      |      "industrySectorLimit": 511.5,
      |      "lastSubsidyUsageUpdt": "2136/08-03",
      |      "undertakingBusinessEntity": [
      |        {
      |          "businessEntityIdentifier": "GB123456789012",
      |          "leadEORI": true,
      |          "address": {
      |            "addressLine1": "ABCDE",
      |            "countryCode": "AB",
      |            "addressLine2": "ABCDEFGHIJKLMNOPQRSTUVWXYZAB",
      |            "addressLine3": "ABCDEF",
      |            "postcode": "ABCDEF"
      |          },
      |          "contacts": {
      |            "phone": "ABCDEFGHIJKLMNOPQRSTUV",
      |            "mobile": "ABCDE"
      |          }
      |        }
      |      ]
      |    }
      |  }
      |}
    """.stripMargin)


}
