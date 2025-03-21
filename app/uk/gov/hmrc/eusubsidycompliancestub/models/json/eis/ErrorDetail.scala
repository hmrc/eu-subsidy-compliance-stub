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

package uk.gov.hmrc.eusubsidycompliancestub.models.json.eis

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{CorrelationID, ErrorCode, ErrorMessage, Source}

case class ErrorDetails(errorDetail: ErrorDetail)
object ErrorDetails {
  implicit val write: Writes[ErrorDetails] = Json.writes

  def apply(errorCode: ErrorCode, errorMessage: ErrorMessage, sourceFaultDetail: String): ErrorDetails =
    ErrorDetails(
      ErrorDetail(
        errorCode = ErrorCode(errorCode),
        errorMessage = ErrorMessage(errorMessage),
        sourceFaultDetail = SourceFaultDetail(List(sourceFaultDetail))
      )
    )
}

case class ErrorDetail(
  timestamp: LocalDateTime = LocalDateTime.now,
  correlationId: CorrelationID = CorrelationID(UUID.randomUUID().toString),
  errorCode: ErrorCode,
  errorMessage: ErrorMessage,
  source: Source = Source("EIS"),
  sourceFaultDetail: SourceFaultDetail
)

object ErrorDetail {
  implicit val oddEisFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  implicit val write: Writes[ErrorDetail] = Json.writes
}

case class SourceFaultDetail(detail: List[String])
object SourceFaultDetail {
  implicit val write: Writes[SourceFaultDetail] = Json.writes
}
