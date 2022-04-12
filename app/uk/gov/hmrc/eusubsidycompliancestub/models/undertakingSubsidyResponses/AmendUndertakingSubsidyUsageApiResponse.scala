/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancestub.models.undertakingSubsidyResponses

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ResponseCommon
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisStatus, EisStatusString, UndertakingRef}

import java.time.LocalDateTime

case class AmendUndertakingSubsidyUsageApiResponse(
  amendUndertakingSubsidyUsageResponse: AmendUndertakingSubsidyUsageResponse
)

object AmendUndertakingSubsidyUsageApiResponse {
  implicit val writes: Writes[AmendUndertakingSubsidyUsageApiResponse] = Json.writes
  def apply(errorCode: String, errorText: String): AmendUndertakingSubsidyUsageApiResponse =
    AmendUndertakingSubsidyUsageApiResponse(
      AmendUndertakingSubsidyUsageResponse(
        responseCommon = ResponseCommon(
          errorCode,
          errorText
        ),
        None
      )
    )

  def apply(undertakingRef: UndertakingRef): AmendUndertakingSubsidyUsageApiResponse =
    AmendUndertakingSubsidyUsageApiResponse(
      AmendUndertakingSubsidyUsageResponse(
        responseCommon = ResponseCommon(
          EisStatus.OK,
          EisStatusString("Success"),
          LocalDateTime.now,
          None
        ),
        ResponseDetail(undertakingRef).some
      )
    )
}
case class AmendUndertakingSubsidyUsageResponse(responseCommon: ResponseCommon, responseDetail: Option[ResponseDetail])

object AmendUndertakingSubsidyUsageResponse {
  implicit val writes: Writes[AmendUndertakingSubsidyUsageResponse] = Json.writes
}

case class ResponseDetail(undertakingIdentifier: UndertakingRef)
object ResponseDetail {
  implicit val writes: Writes[ResponseDetail] = Json.writes
}
