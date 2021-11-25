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
import java.time._

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.types._

package object eis {

  val clock: Clock = Clock.systemUTC()
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
  val oddEisDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM-dd")

  def receiptDate: String = {
    val instant = Instant.now(clock)
    val withoutNanos = instant.minusNanos(instant.getNano)
    formatter.format(withoutNanos)
  }

  implicit class RichLocalDateTime(in: LocalDateTime) {
    def eisFormat: String =
      formatter.format(in.toInstant(ZoneOffset.UTC).minusNanos(in.getNano))
  }

  // provides response for EIS retrieveUndertaking call
  implicit val eisRetrieveUndertakingResponse: Writes[Undertaking] = new Writes[Undertaking] {

    override def writes(o: Undertaking): JsValue = Json.obj(
      "retrieveUndertakingResponse" -> Json.obj(
        "responseCommon" ->
          ResponseCommon(
            EisStatus.OK,
            EisStatusString("ok"),
            LocalDateTime.now,
            None
          ),
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

  // formatter for the response from EIS when creating the Undertaking
  implicit val eisCreateUndertakingResponse: Writes[UndertakingRef] = new Writes[UndertakingRef] {
    override def writes(undertakingRef: UndertakingRef): JsValue = {
      Json.obj(
        "createUndertakingResponse" -> Json.obj(
          "responseCommon" ->
            ResponseCommon(
              EisStatus.OK,
              EisStatusString("String"),
              LocalDateTime.now,
              None
            ),
          "responseDetail" -> Json.obj(
            "undertakingReference" -> undertakingRef
          )
        )
      )
    }
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
