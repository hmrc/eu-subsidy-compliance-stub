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

import play.api.libs.json.Json
import shapeless.tag.@@

package object types extends SimpleJson {

  type IndustrySectorLimit = BigDecimal @@ IndustrySectorLimit.Tag
  object IndustrySectorLimit extends ValidatedType[BigDecimal] {
    override def validateAndTransform(in: BigDecimal): Option[BigDecimal] = {
      Some(in).filter { x =>
        (x <= 99999999999.99) && (x.scale <= 2)
      }
    }
  }

  type UndertakingName = String @@ UndertakingName.Tag
  object UndertakingName extends RegexValidatedString(
    regex = """.{1,105}"""
  )

  type EORI = String @@ EORI.Tag
  object EORI extends RegexValidatedString(
    """^(GB|XI)[0-9]{12,15}$"""
  )

  type UndertakingRef = String @@ UndertakingRef.Tag
  object UndertakingRef extends RegexValidatedString(
    regex = """.{1,17}"""
  )

  type Sector = String @@ Sector.Tag // TODO if using enumeratum do enumeratum otherwise sealed trait
  object Sector extends RegexValidatedString(
    regex = "0|1|2|3"
  )

  type Postcode = String @@ Postcode.Tag
  object Postcode extends RegexValidatedString(
    """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}$""",
    _.trim.replaceAll("[ \\t]+", " ").toUpperCase
  )

  type AddressLine = String @@ AddressLine.Tag
  object AddressLine extends RegexValidatedString(
    regex = """^[a-zA-Z0-9 '&.-]{1,40}$"""
  )

  type PhoneNumber = String @@ PhoneNumber.Tag
  object PhoneNumber extends RegexValidatedString(
    regex = """.{1,24}"""
  )

  type CountryCode = String @@ CountryCode.Tag
  object CountryCode extends RegexValidatedString(
    """^[A-Z][A-Z]$""",
    _.toUpperCase match {
      case "UK" => "GB"
      case other => other
    }
  )

  object EisStatus extends Enumeration {
    type EisStatus = Value
    val OK, NOT_OK = Value

    implicit val format = Json.formatEnum(EisStatus)
  }

  object EisParamName extends Enumeration {
    type EisParamName = Value
    val ERRORCODE, ERRORTEXT = Value

    implicit val format = Json.formatEnum(EisParamName)
  }

  type EisParamValue = String @@ EisParamValue.Tag
  object EisParamValue extends RegexValidatedString(
    """.{1,255}"""
  )

  type EisStatusString = String @@ EisStatusString.Tag
  object EisStatusString extends RegexValidatedString(
    """.{0,100}"""
  )

  type ErrorCode = String @@ ErrorCode.Tag
  object ErrorCode extends RegexValidatedString(
    """.{1,35}"""
  )

  type ErrorMessage = String @@ ErrorMessage.Tag
  object ErrorMessage extends RegexValidatedString(
    """.{1,255}"""
  )

  type Source = String @@ Source.Tag
  object Source extends RegexValidatedString(
    """.{1,40}"""
  )

  type CorrelationID = String @@ CorrelationID.Tag
  object CorrelationID extends RegexValidatedString(
    """.{1,36}"""
  )

  type NonEmptyString = String @@ NonEmptyString.Tag
  object NonEmptyString extends ValidatedType[String]{
    def validateAndTransform(in: String): Option[String] =
      Some(in).filter(_.nonEmpty)
  }


}