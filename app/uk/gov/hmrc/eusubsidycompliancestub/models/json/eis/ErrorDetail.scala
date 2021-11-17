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

package uk.gov.hmrc.eusubsidycompliancestub.models.json.eis

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{CorrelationID, ErrorCode, Source, ErrorMessage}

case class ErrorDetail(
  errorCode: ErrorCode, // "403",
  errorMessage: ErrorMessage, //  "Invalid message : BEFORE TRANSFORMATION" or "Server Error"
  sourceFaultDetail: List[String], // TODO this doesn't look like the Json below
  source: Source = Source("EIS"),
  timestamp: LocalDateTime = LocalDateTime.now, //"2001-12-17T09:30:47Z",
  correlationId: CorrelationID = CorrelationID(UUID.randomUUID().toString) // "f058ebd6-02f7-4d3f-942e-904344e8cde5",
)


object ErrorDetail {
  val oddEisFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  implicit val errorDetailWrites: Writes[ErrorDetail] = new Writes[ErrorDetail] {

    override def writes(o: ErrorDetail): JsValue = Json.obj(
      "errorDetail" -> Json.obj(
        "timestamp" -> o.timestamp.format(oddEisFormat),
        "errorCode" -> o.errorCode,
        "errorMessage" -> o.errorMessage,
        "source" -> o.source,
        "correlationId" -> o.correlationId,
        "sourceFaultDetail" -> Json.obj(
          "detail" -> o.sourceFaultDetail
        )
      )
    )
  }
}



//{
//  "errorDetail": {
//    "timestamp": "2001-12-17T09:30:47Z",
//    "correlationId": "f058ebd6-02f7-4d3f-942e-904344e8cde5",
//    "errorCode": "403",
//    "errorMessage": "Invalid message : BEFORE TRANSFORMATION",
//    "source": "EIS",
//    "sourceFaultDetail": {
//      "detail": [
//        "object has missing required properties (['originatingSystem'])"
//      ]
//    }
//  }
//}