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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.controllers.SubsidyController.{geFilteredNonHMRCSubsidyList, getFilteredHMRCSubsidyList}
import uk.gov.hmrc.eusubsidycompliancestub.models.{SubsidyUndertakingTransactionRequest, SubsidyUpdate, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, SubsidyRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingSubsidyResponses.{AmendUndertakingSubsidyUsageApiResponse, GetUndertakingTransactionApiResponse}
import uk.gov.hmrc.eusubsidycompliancestub.services.{EscService, Store}
import uk.gov.hmrc.eusubsidycompliancestub.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class SubsidyController @Inject() (
  escService: EscService,
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc) {

  def updateUsage: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[JsValue] { json =>
      processPayload(json, "updateSubsidyUsageRequest") match {
        case Some(errorDetail) => // payload fails schema check
          Forbidden(Json.toJson(errorDetail)).toFuture
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
          Forbidden(Json.toJson(errorDetail)).toFuture
        case _ =>
          val undertakingRef: UndertakingRef = (json \ "undertakingIdentifier").as[UndertakingRef]
          val subsidyUndertakingTransactionRequest = json.as[SubsidyUndertakingTransactionRequest]
          getRetrieveResponse(undertakingRef, subsidyUndertakingTransactionRequest)
      }
    }
  }

  private def getUpdateResponse(undertakingRef: UndertakingRef, json: JsValue, subsidyUpdate: SubsidyUpdate)(implicit
    headerCarrier: HeaderCarrier
  ) =
    undertakingRef match {
      case a if a.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case b if b.endsWith("888") => // fake 004
        Ok(
          Json.toJson(AmendUndertakingSubsidyUsageApiResponse("004", "Duplicate submission acknowledgment reference"))
        ).toFuture

      case c if c.endsWith("777") =>
        Ok(
          Json.toJson(
            AmendUndertakingSubsidyUsageApiResponse("107", s"Undertaking reference in the API not Subscribed in ETMP")
          )
        ).toFuture

      case d if d.endsWith("666") =>
        val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
        Ok(Json.toJson(AmendUndertakingSubsidyUsageApiResponse("106", s"EORI not Subscribed in ETMP $eori"))).toFuture

      case e if e.endsWith("555") =>
        val eori = (json \ "undertakingSubsidyAmendment" \ 0 \ "businessEntityIdentifier").as[EORI]
        Ok(
          Json.toJson(AmendUndertakingSubsidyUsageApiResponse("112", s"EORI $eori not linked with undertaking."))
        ).toFuture

      case f if f.endsWith("444") =>
        val sutID = (json \ "undertakingSubsidyAmendment" \ 0 \ "subsidyUsageTransactionId").as[SubsidyRef]
        Ok(
          Json.toJson(
            AmendUndertakingSubsidyUsageApiResponse(
              "111",
              s"Subsidy allocation ID number $sutID or date is invalid is invalid"
            )
          )
        ).toFuture

      case _ =>
        for {
          _ <- escService.updateLastSubsidyUsage(
            undertakingRef,
            subsidyUpdate.nilSubmissionDate.map(_.d).getOrElse(LocalDate.now)
          )
          _ <- escService.updateSubsidies(undertakingRef, subsidyUpdate.update)
        } yield Ok(Json.toJson(AmendUndertakingSubsidyUsageApiResponse(subsidyUpdate.undertakingIdentifier)))
    }

  private def getRetrieveResponse(
    undertakingRef: UndertakingRef,
    subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest
  )(implicit headerCarrier: HeaderCarrier) =
    undertakingRef match {
      case a if a.endsWith("999") => // fake 500
        InternalServerError(Json.toJson(errorDetailFor500)).toFuture

      case b if b.endsWith("888") => // fake 004
        Ok(
          Json.toJson(GetUndertakingTransactionApiResponse("004", "Duplicate submission acknowledgment reference"))
        ).toFuture

      case c if c.endsWith("777") =>
        Ok(Json.toJson(GetUndertakingTransactionApiResponse("201", "Invalid Undertaking identifier"))).toFuture

      case d if d.endsWith("666") =>
        Ok(
          Json.toJson(
            GetUndertakingTransactionApiResponse("202", "Error while fetching the Currency conversion values")
          )
        ).toFuture

      case _ =>
        for {
          subsidies <- escService.retrieveAllSubsidies(undertakingRef)
          filteredHMRCSubsidyListOpt = getFilteredHMRCSubsidyList(subsidyUndertakingTransactionRequest, subsidies)
          filteredNonHMRCSubsidyListOpt =
            geFilteredNonHMRCSubsidyList(subsidyUndertakingTransactionRequest, subsidies)
          retrieveResponse = subsidies.copy(
            nonHMRCSubsidyUsage = filteredNonHMRCSubsidyListOpt
              .getOrElse(List.empty),
            hmrcSubsidyUsage = filteredHMRCSubsidyListOpt.getOrElse(List.empty)
          )
        } yield Ok(Json.toJson(GetUndertakingTransactionApiResponse(retrieveResponse)))
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
  def getFilteredHMRCSubsidyList(
    subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest,
    subsidies: UndertakingSubsidies
  ) =
    if (subsidyUndertakingTransactionRequest.getHMRCUsageTransaction) {
      for {
        dateFrom <- subsidyUndertakingTransactionRequest.dateFromHMRCSubsidyUsage
        dateTo <- subsidyUndertakingTransactionRequest.dateToHMRCSubsidyUsage
      } yield subsidies.hmrcSubsidyUsage.filter(x =>
        (x.acceptanceDate.isAfter(dateFrom) || x.acceptanceDate.isEqual(dateFrom)) && (x.acceptanceDate.isBefore(
          dateTo
        ) || x.acceptanceDate.isEqual(dateTo))
      )
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
  def geFilteredNonHMRCSubsidyList(
    subsidyUndertakingTransactionRequest: SubsidyUndertakingTransactionRequest,
    subsidies: UndertakingSubsidies
  ) =
    if (subsidyUndertakingTransactionRequest.getNonHMRCUsageTransaction) {
      for {
        dateFrom <- subsidyUndertakingTransactionRequest.dateFromNonHMRCSubsidyUsage
        dateTo <- subsidyUndertakingTransactionRequest.dateToNonHMRCSubsidyUsage
      } yield subsidies.nonHMRCSubsidyUsage
        .filter(x =>
          (x.allocationDate.isAfter(dateFrom) || x.allocationDate.isEqual(dateFrom)) && (x.allocationDate
            .isBefore(dateTo) || x.allocationDate.isEqual(dateTo))
        )
        .map(_.copy(amendmentType = None))
    } else
      //In this scenario I am assuming that if getNonHMRCUsageTransaction is false , then we don't have to fetch anything. Please do confirm on this assumption
      List().some
}
