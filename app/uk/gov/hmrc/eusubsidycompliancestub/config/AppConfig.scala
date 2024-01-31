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

package uk.gov.hmrc.eusubsidycompliancestub.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector

@Singleton
class AppConfig @Inject() (config: Configuration) {

  def sectorCap(sector: Sector): BigDecimal = {
    val sectorName = sector.id match {
      case 0 => "other"
      case 1 => "transport"
      case 2 => "agriculture"
      case 3 => "aquaculture"
    }
    //The Play ConfigLoader that is used below only has a set number of Data types, BigDecimal is not one of them, so we need to manually convert the value we get
    val stringValue = config.get[String](s"sectorCap.$sectorName")
    BigDecimal(stringValue)
  }
}
