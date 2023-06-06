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

package uk.gov.hmrc.eusubsidycompliancestub.models.json

import cats.implicits._
import java.time.format.DateTimeFormatter
import java.time._

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models._
import uk.gov.hmrc.eusubsidycompliancestub.models.types._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector

package object eis {

  val clock: Clock = Clock.systemUTC()
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
  val oddEisDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def receiptDate: String = {
    val instant = Instant.now(clock)
    val withoutNanos = instant.minusNanos(instant.getNano)
    formatter.format(withoutNanos)
  }

  implicit class RichLocalDateTime(in: LocalDateTime) {
    def eisFormat: String =
      formatter.format(in.toInstant(ZoneOffset.UTC).minusNanos(in.getNano))
  }

  // convenience reads so we can store a created undertaking
  val undertakingRequestReads: Reads[Undertaking] = new Reads[Undertaking] {
    override def reads(json: JsValue): JsResult[Undertaking] = {
      val contacts: ContactDetails = ContactDetails(
        (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "contacts" \ "phone")
          .asOpt[PhoneNumber],
        (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "contacts" \ "mobile")
          .asOpt[PhoneNumber]
      )
      val businessEntity: BusinessEntity = BusinessEntity(
        (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "idValue").as[EORI],
        true,
        contacts.some
      )
      JsSuccess(
        Undertaking(
          None,
          (json \ "createUndertakingRequest" \ "requestDetail" \ "undertakingName").as[UndertakingName],
          (json \ "createUndertakingRequest" \ "requestDetail" \ "industrySector").as[Sector],
          None,
          None,
          List(businessEntity)
        )
      )
    }
  }

  // convenience reads so we can store business entity updates
  val businessEntityReads: Reads[BusinessEntity] = new Reads[BusinessEntity] {
    override def reads(json: JsValue): JsResult[BusinessEntity] = JsSuccess(
      BusinessEntity(
        (json \ "businessEntityIdentifier").as[EORI],
        (json \ "leadEORIIndicator").as[Boolean],
        (json \ "contacts").asOpt[ContactDetails]
      )
    )
  }
}
