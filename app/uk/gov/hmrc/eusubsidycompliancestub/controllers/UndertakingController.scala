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

package uk.gov.hmrc.eusubsidycompliancestub.controllers

import cats.implicits.catsSyntaxOptionId

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntityUpdate, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{ErrorDetails, receiptDate, undertakingRequestReads}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, ErrorCode, ErrorMessage, IndustrySectorLimit, Sector, UndertakingName, UndertakingRef, UndertakingStatus}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses.{AmendUndertakingApiResponse, CreateUndertakingApiResponse, GetUndertakingBalanceApiResponse, RetrieveUndertakingApiResponse, UpdateUndertakingApiResponse}
import uk.gov.hmrc.eusubsidycompliancestub.services.{EisService, Store}
import uk.gov.hmrc.eusubsidycompliancestub.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import scala.concurrent.Future

@Singleton
class UndertakingController @Inject() (
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
)(implicit appConfig: AppConfig)
    extends BackendController(cc) {

  def create: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "createUndertakingRequest") match {
        case Some(errorDetail) => // payload schema check failed
          Forbidden(Json.toJson(errorDetail)).toFuture
        case _ =>
          val eori: EORI = (json \ "createUndertakingRequest" \ "requestDetail" \ "businessEntity" \ "idValue").as[EORI]
          getCreateResponse(eori, json)
      }
    }
  }

  private def getCreateResponse(eori: EORI, json: JsValue): Future[Result] =
    eori match {
      case a if a.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case b if b.endsWith("888") => // fake 004s
        Ok(Json.toJson(CreateUndertakingApiResponse("004", "Duplicate submission acknowledgment reference"))).toFuture

      case c if c.endsWith("777") || Store.undertakings.retrieveByEori(c).nonEmpty =>
        Ok(
          Json.toJson(
            CreateUndertakingApiResponse("101", s"EORI $eori already associated with another Undertaking $eori")
          )
        ).toFuture

      case d if d.endsWith("666") =>
        Ok(
          Json.toJson(
            CreateUndertakingApiResponse("102", s"Invalid EORI number $eori")
          )
        ).toFuture

      case e if e.endsWith("555") =>
        Ok(Json.toJson(CreateUndertakingApiResponse("113", s"Postcode missing for the address"))).toFuture

      //create an Undertaking with lastSubsidyUsageUpdt which is 77 days older than today i.e between the range of 76-90 days
      case f if f.endsWith("444") =>
        val JsSuccess(undertaking, _) = Json.fromJson(json)(undertakingRequestReads)
        val madeUndertaking =
          EisService.makeUndertaking(undertaking, eori, LocalDate.now.minusDays(77).some, UndertakingStatus(0).some)
        Store.undertakings.put(madeUndertaking)
        Ok(Json.toJson(CreateUndertakingApiResponse(madeUndertaking.reference))).toFuture

      case _ =>
        val JsSuccess(undertaking, _) = Json.fromJson(json)(undertakingRequestReads)
        val madeUndertaking =
          EisService.makeUndertaking(undertaking, eori, undertakingStatus = UndertakingStatus(0).some)
        Store.undertakings.put(madeUndertaking)
        Ok(Json.toJson(CreateUndertakingApiResponse(madeUndertaking.reference))).toFuture
    }

  def retrieve: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "retrieveUndertakingRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Forbidden(Json.toJson(errorDetail)).toFuture
        case _ =>
          val eori: EORI = (json \ "retrieveUndertakingRequest" \ "requestDetail" \ "idValue").as[EORI]
          getRetrieveResponse(eori)
      }
    }
  }

  private def getRetrieveResponse(eori: EORI): Future[Result] = {
    lazy val maybeStoredUndertaking: Option[Undertaking] = Store.undertakings.retrieveByEori(eori)

    val noneSubscribedResponse = Ok(
      Json.toJson(
        RetrieveUndertakingApiResponse("107", "Undertaking reference in the API not Subscribed in ETMP")
      )
    ).toFuture

    eori match {

      case possible500Eori if possible500Eori.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case possibleInvalidEori if possibleInvalidEori.endsWith("777") => // ID invalid
        Ok(Json.toJson(RetrieveUndertakingApiResponse("055", "ID number missing or invalid"))).toFuture

      case possibleNotSubscribedEori if possibleNotSubscribedEori.endsWith("888") =>
        noneSubscribedResponse

      case _ =>
        maybeStoredUndertaking match {
          case Some(undertaking) =>
            Ok(Json.toJson(RetrieveUndertakingApiResponse(undertaking))).toFuture

          case _ =>
            // Original logic said should be 404 but was not 404 but did not explain why
            noneSubscribedResponse
        }
    }
  }

  def amendUndertakingMemberData: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "amendUndertakingMemberDataRequest") match {
        case Some(errorDetail) =>
          Forbidden(Json.toJson(errorDetail)).toFuture

        case _ =>
          val undertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]
          getAmendUndertakingResponse(undertakingRef, json)
      }
    }
  }

  private def getAmendUndertakingResponse(undertakingRef: UndertakingRef, json: JsValue) =
    undertakingRef match {
      case a if a.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case b if b.endsWith("888") =>
        Ok(Json.toJson(AmendUndertakingApiResponse("004", "Duplicate submission acknowledgment reference"))).toFuture

      case c if c.endsWith("777") =>
        val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
        Ok(Json.toJson(AmendUndertakingApiResponse("106", s"EORI not Subscribed in ETMP $eori"))).toFuture

      case d if d.endsWith("666") =>
        Ok(
          Json.toJson(AmendUndertakingApiResponse("107", "Undertaking reference in the API not Subscribed in ETMP"))
        ).toFuture

      case e if e.endsWith("555") =>
        val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
        Ok(
          Json.toJson(
            AmendUndertakingApiResponse("108", s"Relationship with another undertaking exist for EORI $eori")
          )
        ).toFuture

      case f if f.endsWith("444") =>
        val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
        Ok(Json.toJson(AmendUndertakingApiResponse("109", s"Relationship does not exist for EORI $eori"))).toFuture

      case g if g.endsWith("333") =>
        val eori = (json \ "memberAmendments" \ 0 \ "businessEntity" \ "businessEntityIdentifier").as[EORI]
        Ok(
          Json.toJson(AmendUndertakingApiResponse("110", s"Subsidy Compliance address does not exist for EORI $eori"))
        ).toFuture

      case _ =>
        val success = Json.obj(
          "amendUndertakingMemberDataResponse" -> Json.obj(
            "responseCommon" -> Json.obj(
              "status" -> "OK",
              "processingDate" -> receiptDate
            )
          )
        )
        val undertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]
        val updates: List[BusinessEntityUpdate] =
          (json \ "memberAmendments").as[List[BusinessEntityUpdate]]

        try {
          Store.undertakings.updateUndertakingBusinessEntities(undertakingRef, updates)
          Ok(Json.toJson(success)).toFuture
        } catch {
          case _: IllegalStateException =>
            Ok(
              Json.toJson(
                AmendUndertakingApiResponse("108", s"Relationship with another undertaking exist for EORI ...")
              )
            ).toFuture

        }
    }

  def update: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "updateUndertakingRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Forbidden(Json.toJson(errorDetail)).toFuture

        case _ =>
          val undertakingRef: UndertakingRef =
            (json \ "updateUndertakingRequest" \ "requestDetail" \ "undertakingId").as[UndertakingRef]
          updateResponse(undertakingRef, json)
      }
    }
  }

  private def updateResponse(undertakingRef: UndertakingRef, json: JsValue) =
    undertakingRef match {
      case a if a.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case b if b.endsWith("888") => // fake 004
        Ok(Json.toJson(UpdateUndertakingApiResponse("004", "Duplicate submission acknowledgment reference"))).toFuture

      case c if c.endsWith("777") || Store.undertakings.retrieve(c).isEmpty => // fake 116
        Ok(Json.toJson(UpdateUndertakingApiResponse("116", s"Invalid Undertaking ID $c"))).toFuture

      case _ => // successful amend
        val amendmentType: EisAmendmentType =
          (json \ "updateUndertakingRequest" \ "requestDetail" \ "amendmentType").as[EisAmendmentType]
        val undertakingRef: UndertakingRef =
          (json \ "updateUndertakingRequest" \ "requestDetail" \ "undertakingId").as[UndertakingRef]
        val name: Option[UndertakingName] =
          (json \ "updateUndertakingRequest" \ "requestDetail" \ "undertakingName").asOpt[UndertakingName]
        val sector: Option[Sector] =
          (json \ "updateUndertakingRequest" \ "requestDetail" \ "industrySector").asOpt[Sector]
        Store.undertakings.updateUndertaking(undertakingRef, amendmentType, name, sector)
        Ok(Json.toJson(UpdateUndertakingApiResponse(UndertakingRef(undertakingRef)))).toFuture
    }

  def getUndertakingBalance: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "getUndertakingBalanceRequest") match {
        case Some(errorDetail: ErrorDetails) => // payload fails schema check
          Forbidden(Json.toJson(errorDetail)).toFuture
        case _ => {
          val eoriOpt: Option[EORI] = (json \ "eori").asOpt[EORI]
          val undertakingIdentifierOpt: Option[UndertakingRef] = (json \ "undertakingIdentifier").asOpt[UndertakingRef]
          getUndertakingBalanceResponse(eoriOpt, undertakingIdentifierOpt)
        }
      }
    }
  }

  private def getUndertakingBalanceResponse(
    eoriOpt: Option[EORI],
    undertakingIdentifierOpt: Option[UndertakingRef]
  ): Future[Result] = {
    def getSubsidies(undertakingRef: UndertakingRef): UndertakingSubsidies =
      Store.subsidies.retrieveSubsidies(undertakingRef).getOrElse(UndertakingSubsidies.emptyInstance(undertakingRef))

    //return undertaking does not exist error when eori ends with 111908
    if (eoriOpt.map(_.endsWith("111908")) == Some(true)) {
      Ok(
        Json.toJson(
          GetUndertakingBalanceApiResponse("500", "Undertaking doesn't exist")
        )
      ).toFuture
    } else {
      val maybeStoredUndertaking: Option[Undertaking] = (eoriOpt, undertakingIdentifierOpt) match {
        case (Some(eori), None) => Store.undertakings.retrieveByEori(eori)
        case (None, Some(undertakingIdentifier)) => Store.undertakings.retrieve(undertakingIdentifier)
      }

      val noneSubscribedResponse = Ok(
        Json.toJson(
          GetUndertakingBalanceApiResponse("107", "Undertaking reference in the API not Subscribed in ETMP")
        )
      ).toFuture

      maybeStoredUndertaking match {
        case Some(undertaking) => {
          val subsidies = getSubsidies(undertaking.reference)
          Ok(Json.toJson(GetUndertakingBalanceApiResponse(undertaking, subsidies))).toFuture
        }
        case _ => noneSubscribedResponse
      }
    }

  }

}
