/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.beneficiaryrequest.BeneficiaryValidationRequest
import uk.gov.hmrc.eusubsidycompliancestub.models.beneficiaryResponses._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancestub.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancestub.services.EscService
import uk.gov.hmrc.eusubsidycompliancestub.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable

@Singleton
class BeneficiaryController @Inject() (
  escService: EscService,
  cc: ControllerComponents,
  authAndEnvAction: AuthAndEnvAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  // In-memory store of EORIs validated via a V request — subsequent R requests return validated=true for these
  private val validatedEoris: mutable.Set[String] = mutable.Set.empty

  private def processingDate: String = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString

  private def errorResponse(code: String, text: String): Result =
    UnprocessableEntity(
      Json.toJson(BeneficiaryValidationErrorResponse(BeneficiaryErrorDetail(processingDate, code, text)))
    )

  def validate: Action[JsValue] = authAndEnvAction.async(parse.json) { implicit request =>
    withJsonBody[BeneficiaryValidationRequest] { req =>
      getValidationResponse(req)
    }
  }
  private def getValidationResponse(req: BeneficiaryValidationRequest): Future[Result] = {
    if (req.idType == "UTID") {
      escService
        .findEoriByUndertakingReference(UndertakingRef(req.idValue))
        .flatMap { eori =>
          processValidation(eori.toString, req.requestType == "V")
        }
        .recover { case _: Exception =>
          errorResponse("007", "No Beneficiary ID Found")
        }
    } else {
      processValidation(req.idValue, req.requestType == "V")
    }
  }

  private def processValidation(id: String, isValidateRequest: Boolean): Future[Result] = {
    id match {
      case a if a.endsWith("999") =>
        InternalServerError("").toFuture

      case b077 if b077.endsWith("077") =>
        escService.retrieveUndertaking(EORI(id)).flatMap {
          case Some(_) => errorResponse("007", "No Beneficiary ID Found").toFuture
          case None =>
            if (isValidateRequest) validatedEoris.add(id)
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01234567"),
                        validated = isValidateRequest
                      )
                    )
                  )
                )
              )
            ).toFuture
        }
      case b if b.endsWith("007") =>
        errorResponse("007", "No Beneficiary ID Found").toFuture

      case c if c.endsWith("006") =>
        errorResponse("006", "No EORI Information Found").toFuture

      // ------------------------ all validated multiple
      case e if e.endsWith("005") =>
        escService.retrieveUndertaking(EORI(id)).map {
          case Some(undertaking) =>
            if (isValidateRequest)
              undertaking.undertakingBusinessEntity.foreach(be => validatedEoris.add(be.businessEntityIdentifier))
            Ok(Json.toJson(successFor(undertaking, isValidateRequest)))
          case None =>
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01234567"),
                        validated = true
                      ),
                      BeneficiaryDetail(
                        eori = "GB503000000112",
                        benName = Some("GB503000000112"),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01230123"),
                        validated = true
                      ),
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("4564567"),
                        validated = true
                      )
                    )
                  )
                )
              )
            )
        }

      //  ---------------------------- not validated multiple
      case f if f.endsWith("505") =>
        escService.retrieveUndertaking(EORI(id)).map {
          case Some(undertaking) =>
            if (isValidateRequest)
              undertaking.undertakingBusinessEntity.foreach(be => validatedEoris.add(be.businessEntityIdentifier))
            Ok(Json.toJson(successFor(undertaking, isValidateRequest)))
          case None =>
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01234567"),
                        validated = true
                      ),
                      BeneficiaryDetail(
                        eori = "GB503000000112",
                        benName = Some("GB503000000112"),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01230123"),
                        validated = true
                      ),
                      BeneficiaryDetail(
                        eori = "GB503000000113",
                        benName = Some("GB503000000113"),
                        benIDType = Some("CRN"),
                        benIDValue = Some("4564567"),
                        validated = false
                      )
                    )
                  )
                )
              )
            )
        }

      // ----------------- not validated single EORI
      case g if g.endsWith("606") =>
        escService.retrieveUndertaking(EORI(id)).map {
          case Some(undertaking) =>
            if (isValidateRequest)
              undertaking.undertakingBusinessEntity.foreach(be => validatedEoris.add(be.businessEntityIdentifier))
            Ok(Json.toJson(successFor(undertaking, isValidateRequest)))
          case None =>
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01234567"),
                        validated = false
                      )
                    )
                  )
                )
              )
            )
        }

      case h if h.endsWith("101") =>
        escService.retrieveUndertaking(EORI(id)).map {
          case Some(undertaking) =>
            if (isValidateRequest)
              undertaking.undertakingBusinessEntity.foreach(be => validatedEoris.add(be.businessEntityIdentifier))
            Ok(Json.toJson(successFor(undertaking, isValidateRequest)))
          case None =>
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = None,
                        benIDValue = None,
                        validated = false
                      )
                    )
                  )
                )
              )
            )
        }

      // ----------------- Validated single EORI
      case _ =>
        escService.retrieveUndertaking(EORI(id)).map {
          case Some(undertaking) =>
            if (isValidateRequest)
              undertaking.undertakingBusinessEntity.foreach(be => validatedEoris.add(be.businessEntityIdentifier))
            Ok(Json.toJson(successFor(undertaking, isValidateRequest)))
          case None =>
            Ok(
              Json.toJson(
                BeneficiaryValidationSuccessResponse(
                  BeneficiarySuccess(
                    processingDate = processingDate,
                    beneficiaryInfo = List(
                      BeneficiaryDetail(
                        eori = id,
                        benName = Some(id),
                        benIDType = Some("CRN"),
                        benIDValue = Some("01234567"),
                        validated = true
                      )
                    )
                  )
                )
              )
            )
        }
    }
  }

  private def successFor(undertaking: Undertaking, validated: Boolean): BeneficiaryValidationSuccessResponse =
    BeneficiaryValidationSuccessResponse(
      BeneficiarySuccess(
        processingDate = processingDate,
        beneficiaryInfo = undertaking.undertakingBusinessEntity.map { be =>
          val leadEori =
            undertaking.undertakingBusinessEntity.find(_.leadEORI).map(_.businessEntityIdentifier).getOrElse("")
          val hasId = be.leadEORI || leadEori.endsWith("033") || be.businessEntityIdentifier.endsWith("088")
          BeneficiaryDetail(
            eori = be.businessEntityIdentifier,
            benName = if (hasId) Some(undertaking.name) else None,
            benIDType = if (hasId) Some("CRN") else None,
            benIDValue = if (hasId) Some("01234567") else None,
            validated = hasId && (validated || validatedEoris.contains(be.businessEntityIdentifier))
          )
        }
      )
    )
}
