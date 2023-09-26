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

package uk.gov.hmrc.eusubsidycompliancestub.models.json.eis

import cats.implicits.catsSyntaxOptionId

import java.time.LocalDateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisParamName.EisParamName
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisStatus.EisStatus
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EisParamName, EisParamValue, EisStatus, EisStatusString}

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
  returnParameters: Option[List[Params]] // TODO make an option
)

object ResponseCommon {

  implicit val ldtwrites: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    override def writes(o: LocalDateTime): JsValue = JsString(o.eisFormat)
  }
  implicit val writes: Writes[ResponseCommon] = (
    (JsPath \ "status").write[EisStatus] and
      (JsPath \ "statusText").write[EisStatusString] and
      (JsPath \ "processingDate").write[LocalDateTime] and
      (JsPath \ "returnParameters").writeNullable[List[Params]]
  )(unlift(ResponseCommon.unapply))

  def apply(errorCode: String, errorText: String): ResponseCommon =
    ResponseCommon(
      EisStatus.NOT_OK,
      EisStatusString("String"), // taken verbatim from spec
      LocalDateTime.now,
      List(
        Params(
          EisParamName.ERRORCODE,
          EisParamValue(errorCode)
        ),
        Params(
          EisParamName.ERRORTEXT,
          EisParamValue(errorText)
        )
      ).some
    )

  def apply(receiptDate: String): ResponseCommon =
    ResponseCommon(
      EisStatus.OK,
      EisStatusString("String"), // taken verbatim from spec
      LocalDateTime.parse(receiptDate),
      None
    )

  implicit val format: Format[ResponseCommon] = Json.format[ResponseCommon]

}
