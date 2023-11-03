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

package uk.gov.hmrc.eusubsidycompliancestub.services

import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, IndustrySectorLimit, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{CreateUndertakingRequest, SubsidyRetrieve, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.smartstub._

import java.time.LocalDate

object EisService {

  implicit class RichEORI(in: EORI) {
    def toLong: Long = in.substring(2).toLong
  }

  def makeUndertaking(
    undertaking: CreateUndertakingRequest,
    eori: EORI,
    lastSubsidyUsageUpdt: Option[LocalDate] = None
  )(implicit appConfig: AppConfig): Undertaking = {
    val madeUndertaking = retrieveUndertaking(eori)
    Undertaking(
      reference = madeUndertaking.reference,
      name = undertaking.name,
      industrySector = undertaking.industrySector,
      industrySectorLimit = IndustrySectorLimit(appConfig.sectorCap(undertaking.industrySector)),
      lastSubsidyUsageUpdt = lastSubsidyUsageUpdt,
      undertakingBusinessEntity = undertaking.businessEntity
    )
  }

  def retrieveUndertaking(eori: EORI): Undertaking =
    DataGenerator
      .genRetrievedUndertaking(eori)
      .seeded(
        eori.toLong
      )
      .get

  def undertakingRef(eori: EORI): UndertakingRef =
    DataGenerator.genUndertakingRef
      .seeded(
        eori.toLong
      )
      .get

  def retrieveSubsidies(r: SubsidyRetrieve): UndertakingSubsidies =
    DataGenerator
      .genSubsidies(
        r
      )
      .seeded(
        r.undertakingIdentifier.hashCode.toLong
      )
      .get

}
