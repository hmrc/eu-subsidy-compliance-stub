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

import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EORI

package object eis {

  implicit val undertakingWrites: Writes[Undertaking] = new Writes[Undertaking] {
    override def writes(o: Undertaking): JsValue = ???
  }

  implicit val eoriWrites: Writes[EORI] = new Writes[EORI] {
    val receiptDate = "some date"
    val acknowledgementReference = "some ref"
    override def writes(o: EORI): JsValue = Json.parse(
      s"""
         | {
         |   "retrieveUndertakingRequest": {
         |    "requestCommon": {
         |      "originatingSystem": "MDTP",
         |      "receiptDate": "$receiptDate",
         |      "acknowledgementReference": "$acknowledgementReference",
         |      "messageTypes": {
         |        "messageType": "RetrieveUndertaking"
         |      },
         |      "requestParameters": [
         |        {
         |          "paramName": "REGIME",
         |          "paramValue": "ESC"
         |        }
         |      ]
         |    },
         |    "requestDetail": {
         |      "idType": "EORI",
         |      "idValue": "$o"
         |    }
         |  }
         | }
      """.stripMargin)
  }
}
