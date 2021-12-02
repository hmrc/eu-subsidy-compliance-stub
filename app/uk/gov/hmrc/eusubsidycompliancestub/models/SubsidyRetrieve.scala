/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.UndertakingRef


// assuming that we always want both subsidy types, and that any date range should apply to both
case class SubsidyRetrieve(
  undertakingIdentifier: UndertakingRef,
  inDateRange: Option[(LocalDate, LocalDate)]
)

object SubsidyRetrieve {

  // only writes the field if the value is defined, Play version relies on fields being in case class
  def nullableOpt[A](name: String, value: Option[A]): List[(String, Json.JsValueWrapper)] =
    value.fold(List.empty[(String, Json.JsValueWrapper)]) { v =>
      List((name, JsString(v.toString)))
    }

  implicit val writes: Writes[SubsidyRetrieve] = new Writes[SubsidyRetrieve] {
    override def writes(o: SubsidyRetrieve): JsValue = {
      val l: List[(String, Json.JsValueWrapper)] =
        List(
          ("undertakingIdentifier", JsString(o.undertakingIdentifier)),
          ("getNonHMRCUsageTransaction", JsBoolean(true)),
          ("getHMRCUsageTransaction", JsBoolean(true))
        )
      val x =
        nullableOpt[LocalDate]("dateFromNonHMRCSubsidyUsage", o.inDateRange.map(_._1)) ++
          nullableOpt[LocalDate]("dateFromHMRCSubsidyUsage", o.inDateRange.map(_._1)) ++
          nullableOpt[LocalDate]("dateToNonHMRCSubsidyUsage", o.inDateRange.map(_._2)) ++
          nullableOpt[LocalDate]("dateToHMRCSubsidyUsage", o.inDateRange.map(_._2))

      Json.obj(l ++ x: _*)
    }
  }
}
