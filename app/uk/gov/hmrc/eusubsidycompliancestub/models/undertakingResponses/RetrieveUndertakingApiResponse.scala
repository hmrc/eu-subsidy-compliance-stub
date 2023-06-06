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

import cats.implicits.catsSyntaxOptionId
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.ResponseCommon
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisStatus, EisStatusString, IndustrySectorLimit, UndertakingName, UndertakingRef}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

case class RetrieveUndertakingApiResponse(retrieveUndertakingResponse: RetrieveUndertakingResponse)

object RetrieveUndertakingApiResponse {
  implicit val writes: Writes[RetrieveUndertakingApiResponse] = Json.writes

  def apply(u: Undertaking): RetrieveUndertakingApiResponse = RetrieveUndertakingApiResponse(
    RetrieveUndertakingResponse(
      responseDetail = UndertakingResponse(
        u.reference,
        u.name,
        u.industrySector,
        u.industrySectorLimit,
        u.lastSubsidyUsageUpdt,
        u.undertakingBusinessEntity
      ).some
    )
  )

  def apply(errorCode: String, errorText: String): RetrieveUndertakingApiResponse = RetrieveUndertakingApiResponse(
    RetrieveUndertakingResponse(
      responseCommon = ResponseCommon(
        errorCode,
        errorText
      ),
      None
    )
  )

}

case class RetrieveUndertakingResponse(
  responseCommon: ResponseCommon = ResponseCommon(
    EisStatus.OK,
    EisStatusString("ok"),
    LocalDateTime.now,
    None
  ),
  responseDetail: Option[UndertakingResponse]
)
object RetrieveUndertakingResponse {

  implicit val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  implicit val writes: Writes[RetrieveUndertakingResponse] = Json.writes
}

case class UndertakingResponse(
  undertakingReference: Option[UndertakingRef],
  undertakingName: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: Option[IndustrySectorLimit],
  lastSubsidyUsageUpdt: Option[LocalDate],
  undertakingBusinessEntity: List[BusinessEntity]
)

object UndertakingResponse {
  implicit val writes: Writes[UndertakingResponse] = Json.writes
}
