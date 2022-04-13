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
import uk.gov.hmrc.eusubsidycompliancestub.models.UndertakingSubsidies
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ResponseCommon
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisStatus, EisStatusString}

import java.time.LocalDateTime

case class GetUndertakingTransactionApiResponse(getUndertakingTransactionResponse: GetUndertakingTransactionResponse)

object GetUndertakingTransactionApiResponse {
  implicit val writes: Writes[GetUndertakingTransactionApiResponse] = Json.writes
  def apply(errorCode: String, errorText: String): GetUndertakingTransactionApiResponse =
    GetUndertakingTransactionApiResponse(
      GetUndertakingTransactionResponse(
        responseCommon = ResponseCommon(
          errorCode,
          errorText
        ),
        None
      )
    )

  def apply(us: UndertakingSubsidies): GetUndertakingTransactionApiResponse = GetUndertakingTransactionApiResponse(
    GetUndertakingTransactionResponse(
      responseCommon = ResponseCommon(
        EisStatus.OK,
        EisStatusString("Success"),
        LocalDateTime.now,
        None
      ),
      responseDetail = us.some
    )
  )
}

case class GetUndertakingTransactionResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[UndertakingSubsidies]
)

object GetUndertakingTransactionResponse {
  implicit val writes: Writes[GetUndertakingTransactionResponse] = Json.writes
}
