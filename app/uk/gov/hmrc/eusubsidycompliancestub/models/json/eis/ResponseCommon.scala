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

package uk.gov.hmrc.eusubsidycompliancestub.models.json.eis

import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisParamName.EisParamName
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisStatus.EisStatus
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisParamValue, EisStatusString}


case class Params(
  paramName: EisParamName,
  paramValue: EisParamValue
)

case object Params {
  implicit val paramsFormat: OFormat[Params] =
    Json.format[Params]
}

case class ResponseCommon(
  status: EisStatus,
  statusText: EisStatusString,
  processingDate: LocalDateTime,
  returnParameters: Option[Params]
)

case object ResponseCommon {
  implicit val format: OFormat[ResponseCommon] =
    Json.format[ResponseCommon]
}

