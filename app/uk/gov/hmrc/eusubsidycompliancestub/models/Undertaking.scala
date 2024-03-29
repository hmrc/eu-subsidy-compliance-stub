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

package uk.gov.hmrc.eusubsidycompliancestub.models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate
import uk.gov.hmrc.eusubsidycompliancestub.models.types._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector

case class Undertaking(
  reference: UndertakingRef,
  name: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: IndustrySectorLimit,
  lastSubsidyUsageUpdt: Option[LocalDate],
  undertakingStatus: Option[Int],
  undertakingBusinessEntity: List[BusinessEntity]
) {
  def leadEORI: Option[EORI] =
    undertakingBusinessEntity.find(_.leadEORI).map(_.businessEntityIdentifier)
}
object Undertaking {
  import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits._
  implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
}
