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

import java.time.LocalDateTime

case class CreateUndertakingApiResponse(createUndertakingResponse: CreateUndertakingResponse)
object CreateUndertakingApiResponse {

  def apply(errorCode: String, errorText: String): CreateUndertakingApiResponse = CreateUndertakingApiResponse(
    CreateUndertakingResponse(
      responseCommon = ResponseCommon(
        errorCode,
        errorText
      ),
      None
    )
  )

  def apply(undertakingRef: UndertakingRef): CreateUndertakingApiResponse = CreateUndertakingApiResponse(
    CreateUndertakingResponse(
      responseCommon = ResponseCommon(
        EisStatus.OK,
        EisStatusString("String"),
        LocalDateTime.now,
        None
      ),
      CreateUndertakingResponseDetail(undertakingRef).some
    )
  )
  implicit val writes: Writes[CreateUndertakingApiResponse] = Json.writes[CreateUndertakingApiResponse]
}

case class CreateUndertakingResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[CreateUndertakingResponseDetail]
)

object CreateUndertakingResponse {
  implicit val writes: Writes[CreateUndertakingResponse] = Json.writes
}

case class CreateUndertakingResponseDetail(undertakingReference: UndertakingRef)
object CreateUndertakingResponseDetail {
  implicit val writes: Writes[CreateUndertakingResponseDetail] = Json.writes
}
