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

package uk.gov.hmrc.eusubsidycompliancestub.services

import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, IndustrySectorLimit, Sector, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{SubsidyRetrieve, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.smartstub._

import java.time.LocalDate

object EisService {

  private val SectorLimits = Map[Sector, IndustrySectorLimit](
    Sector.agriculture -> IndustrySectorLimit(30000),
    Sector.aquaculture -> IndustrySectorLimit(20000),
    Sector.other -> IndustrySectorLimit(200000),
    Sector.transport -> IndustrySectorLimit(100000),
  )

  implicit class RichEORI(in: EORI) {
    def toLong: Long = in.substring(2).toLong
  }

  def makeUndertaking(undertaking: Undertaking, eori: EORI, lastSubsidyUsageUpdt: Option[LocalDate] = None): Undertaking = {
    val madeUndertaking = retrieveUndertaking(eori)
    val merged = undertaking.copy(
      reference = madeUndertaking.reference,
      industrySectorLimit = SectorLimits.get(undertaking.industrySector),
      lastSubsidyUsageUpdt = if(lastSubsidyUsageUpdt.isDefined) lastSubsidyUsageUpdt else madeUndertaking.lastSubsidyUsageUpdt
    )
    merged
  }

  def retrieveUndertaking(eori: EORI): Undertaking =
    DataGenerator
      .genRetrievedUndertaking(eori)
      .seeded(
        eori.toLong
      ).get


  def undertakingRef(eori: EORI): UndertakingRef =
    DataGenerator
      .genUndertakingRef
      .seeded(
        eori.toLong
      ).get

  def retrieveSubsidies(r: SubsidyRetrieve): UndertakingSubsidies =
    DataGenerator
      .genSubsidies(
        r
      )
      .seeded(
        r.undertakingIdentifier.hashCode.toLong
      ).get

}
