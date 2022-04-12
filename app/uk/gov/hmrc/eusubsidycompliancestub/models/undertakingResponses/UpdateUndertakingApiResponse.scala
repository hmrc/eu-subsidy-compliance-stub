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

package uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ResponseCommon
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisStatus, EisStatusString, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingSubsidyResponses.ResponseDetail

import java.time.LocalDateTime

case class UpdateUndertakingApiResponse(updateUndertakingResponse: UpdateUndertakingResponse)

object UpdateUndertakingApiResponse {
  implicit val writes: Writes[UpdateUndertakingApiResponse] = Json.writes

  def apply(errorCode: String, errorText: String): UpdateUndertakingApiResponse = UpdateUndertakingApiResponse(
    UpdateUndertakingResponse(
      responseCommon = ResponseCommon(
        errorCode,
        errorText
      ),
      None
    )
  )

  def apply(undertakingRef: UndertakingRef): UpdateUndertakingApiResponse = UpdateUndertakingApiResponse(
    UpdateUndertakingResponse(
      responseCommon = ResponseCommon(
        EisStatus.OK,
        EisStatusString("ok"),
        LocalDateTime.now,
        None
      ),
      UpdateUndertakingResponseDetail(undertakingRef).some
    )
  )
}

case class UpdateUndertakingResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[UpdateUndertakingResponseDetail]
)

object UpdateUndertakingResponse {
  implicit val writes: Writes[UpdateUndertakingResponse] = Json.writes
}

case class UpdateUndertakingResponseDetail(undertakingReference: UndertakingRef)
object UpdateUndertakingResponseDetail {
  implicit val writes: Writes[UpdateUndertakingResponseDetail] = Json.writes
}
