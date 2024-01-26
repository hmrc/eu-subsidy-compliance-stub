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

package uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ResponseCommon

case class AmendUndertakingApiResponse(amendUndertakingMemberDataResponse: AmendUndertakingMemberDataResponse)

object AmendUndertakingApiResponse {
  implicit val writes: Writes[AmendUndertakingApiResponse] = Json.writes

  def apply(errorCode: String, errorText: String): AmendUndertakingApiResponse = AmendUndertakingApiResponse(
    AmendUndertakingMemberDataResponse(
      responseCommon = ResponseCommon(
        errorCode,
        errorText
      )
    )
  )
}

case class AmendUndertakingMemberDataResponse(
  responseCommon: ResponseCommon
)
object AmendUndertakingMemberDataResponse {
  implicit val writes: Writes[AmendUndertakingMemberDataResponse] = Json.writes
}
