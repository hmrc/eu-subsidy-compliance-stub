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

import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.AmendmentType.AmendmentType

import java.time.LocalDate

case class BusinessEntityUpdate(
	amendmentType: AmendmentType,
	amendmentEffectiveDate: LocalDate,
	businessEntity: BusinessEntity
)

object BusinessEntityUpdate {
	implicit val businessEntityWrites: Writes[BusinessEntity] = new Writes[BusinessEntity] {
		override def writes(o: BusinessEntity): JsValue = { Json.obj(
			"businessEntityIdentifier" -> o.businessEntityIdentifier,
			"leadEORIIndicator" -> o.leadEORI,
			"contacts" -> o.contacts
		)
		}
	}
 implicit val format: OFormat[BusinessEntityUpdate] = Json.format[BusinessEntityUpdate]
}