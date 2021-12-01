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

package uk.gov.hmrc.eusubsidycompliancestub.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{eisCreateUndertakingResponse, eisRetrieveUndertakingResponse, eisUpdateUndertakingResponse, receiptDate}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.services.EisService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

@Singleton
class UndertakingController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction,
  eis: EisService
) extends BackendController(cc) {

  def create: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "createUndertakingRequest") match {
        case Some(errorDetail) => // payload schema check failed
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val eori: String = (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "idValue").as[String]
          eori match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake 004
              val dupeAckRef: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "004",
                    "Duplicate submission acknowledgment reference"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeAckRef)))
            case c if c.endsWith("777") =>
              val dupeEori: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "101",
                    s"EORI $eori already associated with another Undertaking $eori"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeEori)))
            case d if d.endsWith("666") =>
              val invalidEori: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "102",
                    s"Invalid EORI number $eori"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(invalidEori)))
            case e if e.endsWith("555") =>
              val missingPostcode: JsValue = Json.obj(
                "createUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "113",
                    s"Postcode missing for the address"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(missingPostcode)))
            case _ =>
              val undertakingRef = eis.undertakingRef(eori)
              Future.successful(Ok(Json.toJson(undertakingRef)(eisCreateUndertakingResponse)))
          }
      }
    }
  }

  def retrieve: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "retrieveUndertakingRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val eori: String = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[String]
          eori match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake not found (ideally should have been 404)
              val noUndertakingFoundResponse: JsValue = Json.obj(
                "retrieveUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "107",
                    "Undertaking reference in the API not Subscribed in ETMP"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(noUndertakingFoundResponse)))
            case _ => // successful retrieval
              val undertaking = eis.retrieveUndertaking(eori)
              Future.successful(Ok(Json.toJson(undertaking)(eisRetrieveUndertakingResponse)))
          }
      }
    }
  }

  def amendUndertakingMemberData: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "amendUndertakingMemberDataRequest") match {
        case Some(errorDetail) =>
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val undertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]
          undertakingRef match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") =>
              val dupeAck = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "004",
                "Duplicate submission acknowledgment reference"
              )
              Future.successful(Ok(Json.toJson(dupeAck)))
            case c if c.endsWith("777") =>
              val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
              val eoriNotFound = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "106",
                s"EORI not Subscribed in ETMP $eori"
              )
              Future.successful(Ok(Json.toJson(eoriNotFound)))
            case d if d.endsWith("666") =>
              val UndRefNotFound = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "107",
                "Undertaking reference in the API not Subscribed in ETMP"
              )
              Future.successful(Ok(Json.toJson(UndRefNotFound)))
            case e if e.endsWith("555") =>
              val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
              val incorrectEORIForUnd = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "108",
                s"Relationship with another undertaking exist for EORI $eori"
              )
              Future.successful(Ok(Json.toJson(incorrectEORIForUnd)))
            case f if f.endsWith("444") =>
              val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
              val noRelationshipExists = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "109",
                s"Relationship does not exist for EORI $eori"
              )
              Future.successful(Ok(Json.toJson(noRelationshipExists)))
            case g if g.endsWith("333") =>
              val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
              val eoriNotFound = notOkCommonResponse(
                "amendUndertakingMemberDataResponse",
                "110",
                s"Subsidy Compliance address does not exist for EORI $eori"
              )
              Future.successful(Ok(Json.toJson(eoriNotFound)))
            case _ =>
              val success = Json.obj(
                "amendUndertakingMemberDataResponse" -> Json.obj(
                  "responseCommon" -> Json.obj(
                    "status" -> "OK",
                    "processingDate" -> receiptDate
                  )
                )
              )
              Future.successful(Ok(Json.toJson(success)))
          }
      }
    }

  }

  def update: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "updateUndertakingRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val undertakingRef: String = (json \ "updateUndertakingRequest" \ "requestDetail" \ "undertakingId").as[String]
          undertakingRef match {
            case a if a.endsWith("999") => // fake 500
              Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
            case b if b.endsWith("888") => // fake 004
              val dupeAckRef: JsValue = Json.obj(
                "updateUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "004",
                    "Duplicate submission acknowledgment reference"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeAckRef)))
            case c if c.endsWith("777") => // fake 116
              val dupeAckRefTwo: JsValue = Json.obj(
                "updateUndertakingResponse" -> Json.obj(
                  "responseCommon" -> badResponseCommon(
                    "116",
                    s"Invalid Undertaking ID $c"
                  )
                )
              )
              Future.successful(Ok(Json.toJson(dupeAckRefTwo)))
            case _ => // successful ammend
              Future.successful(Ok(Json.toJson(UndertakingRef(undertakingRef))(eisUpdateUndertakingResponse)))
          }
      }
    }
  }

}
