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

import cats.implicits.catsSyntaxOptionId

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.controllers.SubsidyController.{geFilteredNonHMRCSubsidyList, getFilteredHMRCSubsidyList}
import uk.gov.hmrc.eusubsidycompliancestub.models.{SubsidyUndertakingTransactionRequest, SubsidyUpdate, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancestub.models.json.eis.{eisRetrieveUndertakingSubsidiesResponse, eisUpdateSubsidyUsageResponse}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, SubsidyRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.services.Store
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class SubsidyController @Inject()(
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
) extends BackendController(cc) {

  def updateUsage: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "updateSubsidyUsageRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val subsidyUpdate: SubsidyUpdate = json.as[SubsidyUpdate]
          val undertakingRef: UndertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]

          getUpdateResponse(undertakingRef, json, subsidyUpdate)
      }
    }
  }

  def retrieveUsage: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "retrieveUndertakingSubsidiesRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Future.successful(Forbidden(Json.toJson(errorDetail)))
        case _ =>
          val undertakingRef: UndertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]
          val subsidyUndertakingTransactionRequest = json.as[SubsidyUndertakingTransactionRequest]
          getRetrieveResponse(undertakingRef, subsidyUndertakingTransactionRequest)
      }
    }
  }

  private def getUpdateResponse(undertakingRef: UndertakingRef, json: JsValue, subsidyUpdate: SubsidyUpdate ) =  undertakingRef match {
    case a if a.endsWith("999") => // fake 500
      Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
    case b if b.endsWith("888") => // fake 004
      val dupeAckRef: JsValue = Json.obj(
        "amendUndertakingSubsidyUsageResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "004",
            "Duplicate submission acknowledgment reference"
          )
        )
      )
      Future.successful(Ok(Json.toJson(dupeAckRef)))
    case c if c.endsWith("777") =>
      val dupeEori: JsValue = Json.obj(
        "amendUndertakingSubsidyUsageResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "107",
            s"Undertaking reference in the API not Subscribed in ETMP"
          )
        )
      )
      Future.successful(Ok(Json.toJson(dupeEori)))
    case d if d.endsWith("666") =>
      val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
      val invalidEori: JsValue = Json.obj(
        "amendUndertakingSubsidyUsageResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "106",
            s"EORI not Subscribed in ETMP $eori"
          )
        )
      )
      Future.successful(Ok(Json.toJson(invalidEori)))
    case e if e.endsWith("555") =>
      val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
      val invalidEori: JsValue = Json.obj(
        "amendUndertakingSubsidyUsageResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "112",
            s"EORI $eori not linked with undertaking." // TODO check full stop
          )
        )
      )
      Future.successful(Ok(Json.toJson(invalidEori)))
    case f if f.endsWith("444") =>
      val sutID = (json \ "undertakingSubsidyAmendment" \ 0 \ "subsidyUsageTransactionId").as[SubsidyRef]
      val invalidEori: JsValue = Json.obj(
        "amendUndertakingSubsidyUsageResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "111",
            s"Subsidy allocation ID number $sutID or date is invalid is invalid" // TODO this string looks wrong
          )
        )
      )
      Future.successful(Ok(Json.toJson(invalidEori)))

    case _ =>
      Try(Store.subsidies.updateSubsidies(undertakingRef, subsidyUpdate.update)) match {
        case Success(_) => Future.successful(Ok(Json.toJson(subsidyUpdate)(eisUpdateSubsidyUsageResponse)))
        case Failure(_) => val updateSubsidyFailed = notOkCommonResponse(
          "amendUndertakingSubsidyUsageResponse",
          "003",
          s"Request couldn't be processed"
        )
          Future.successful(Ok(Json.toJson(updateSubsidyFailed)))
      }
  }

  private def getRetrieveResponse(undertakingRef: UndertakingRef,
                                  subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest) =
    undertakingRef match {
    case a if a.endsWith("999") => // fake 500
      Future.successful(InternalServerError(Json.toJson(errorDetailFor500)))
    case b if b.endsWith("888") => // fake 004
      val dupeAckRef: JsValue = Json.obj(
        "getUndertakingTransactionResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "004",
            "Duplicate submission acknowledgment reference"
          )
        )
      )
      Future.successful(Ok(Json.toJson(dupeAckRef)))
    case c if c.endsWith("777") =>
      val dupeAckRef: JsValue = Json.obj(
        "getUndertakingTransactionResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "201",
            "Invalid Undertaking identifier"
          )
        )
      )
      Future.successful(Ok(Json.toJson(dupeAckRef)))
    case d if d.endsWith("666") =>
      val dupeAckRef: JsValue = Json.obj(
        "getUndertakingTransactionResponse" -> Json.obj(
          "responseCommon" -> badResponseCommon(
            "202",
            "Error while fetching the Currency conversion values"
          )
        )
      )
      Future.successful(Ok(Json.toJson(dupeAckRef)))
    case _ =>
      Try {
        val subsidies: UndertakingSubsidies = Store.subsidies.retrieveSubsidies(undertakingRef).get
        val filteredHMRCSubsidyListOpt = getFilteredHMRCSubsidyList(subsidyUndertakingTransactionRequest, subsidies)
        val filteredNonHMRCSubsidyListOpt = geFilteredNonHMRCSubsidyList(subsidyUndertakingTransactionRequest, subsidies)
        subsidies.copy(nonHMRCSubsidyUsage = filteredNonHMRCSubsidyListOpt.getOrElse(List.empty), hmrcSubsidyUsage = filteredHMRCSubsidyListOpt.getOrElse(List.empty))
      } match {
        case Success(retrieveResponse) =>
          Future.successful(Ok(Json.toJson(retrieveResponse)(eisRetrieveUndertakingSubsidiesResponse)))
        case Failure(_) =>
          val updateSubsidyFailed = notOkCommonResponse(
          "getUndertakingTransactionResponse",
          "003",
          s"Request couldn't be processed"
        )
          Future.successful(Ok(Json.toJson(updateSubsidyFailed)))
      }
  }
}

object SubsidyController {

  /**
   * This function will  (if getHMRCUsageTransaction is true), filter the HMRC subsidy list from the retrieved UndertakingSubsidies by the date range given in the request body
   * else fetch empty list.
   * @param subsidyUndertakingTransactionRequest
   * @param subsidies
   * @return optional list of HMRCSubsidy
   */
  def getFilteredHMRCSubsidyList(subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest,
                                 subsidies: UndertakingSubsidies) =
    if(subsidyUndertakingTransactionRequest.getHMRCUsageTransaction) {
    for {
      dateFrom <- subsidyUndertakingTransactionRequest.dateFromHMRCSubsidyUsage
      dateTo <- subsidyUndertakingTransactionRequest.dateToHMRCSubsidyUsage
    } yield subsidies.hmrcSubsidyUsage.filter(x => (x.acceptanceDate.isAfter(dateFrom) || x.acceptanceDate.isEqual(dateFrom)) && (x.acceptanceDate.isBefore(dateTo) || x.acceptanceDate.isEqual(dateTo)))
  } else {
      //In this scenario I am assuming that if getHMRCUsageTransaction is false , then we don't have to fetch anything. Please do confirm on this assumption
      List().some
    }


  /**
   * This function will  (if getNonHMRCUsageTransaction is true), filter the non HMRC subsidy list from the retrieved UndertakingSubsidies by the date range given in the request body
   * else fetch empty list.
   * @param subsidyUndertakingTransactionRequest
   * @param subsidies
   * @return optional list of nonHMRCSubsidy
   */
  def geFilteredNonHMRCSubsidyList(subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest,
                                   subsidies: UndertakingSubsidies) =
    if(subsidyUndertakingTransactionRequest.getNonHMRCUsageTransaction) {
    for {
      dateFrom <- subsidyUndertakingTransactionRequest.dateFromNonHMRCSubsidyUsage
      dateTo <- subsidyUndertakingTransactionRequest.dateToNonHMRCSubsidyUsage
    } yield {
      subsidies.nonHMRCSubsidyUsage.filter(x => (x.allocationDate.isAfter(dateFrom) || x.allocationDate.isEqual(dateFrom)) && (x.allocationDate.isBefore(dateTo) || x.allocationDate.isEqual(dateTo))).map(_.copy(amendmentType = None))
    }
  } else
    //In this scenario I am assuming that if getNonHMRCUsageTransaction is false , then we don't have to fetch anything. Please do confirm on this assumption
      List().some
}
