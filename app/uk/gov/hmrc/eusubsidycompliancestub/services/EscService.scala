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

package uk.gov.hmrc.eusubsidycompliancestub.services

import cats.data.EitherT
import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsPath, JsonValidationError, Reads}
import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, BusinessEntityUpdate, NilSubmissionDate, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidies, UndertakingSubsidyAmendment, Update}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, EisAmendmentType, EisSubsidyAmendmentType, IndustrySectorLimit, SubsidyAmount, SubsidyRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.undertakingResponses.UndertakingBalance
import uk.gov.hmrc.eusubsidycompliancestub.repositories.UndertakingCache
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector

import scala.util.Random

@Singleton
class EscService @Inject() (
  undertakingCache: UndertakingCache
)(implicit ec: ExecutionContext)
    extends Logging {

  def createUndertaking(eori: EORI, undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[UndertakingRef] =
    undertakingCache.put[Undertaking](eori, undertaking).map(_.reference)

  def deleteUndertaking(undertakingRef: UndertakingRef)(implicit hc: HeaderCarrier): Future[Unit] =
    undertakingCache.deleteUndertaking(undertakingRef)

  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[Undertaking]] =
    undertakingCache.findUndertakingByEori(eori)

  def retrieveUndertakingSubsidiesByEori(
    eori: EORI
  )(implicit hc: HeaderCarrier): Future[Option[UndertakingSubsidies]] = {
    undertakingCache.get[UndertakingSubsidies](eori)
  }

  def updateUndertakingBusinessEntities(
    undertakingRef: UndertakingRef,
    updates: List[BusinessEntityUpdate]
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    val remove: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.delete).map(_.businessEntity)

    if (!remove.filter(_.leadEORI).isEmpty) {
      deleteUndertaking(undertakingRef)
    } else {
      val add: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.add).map(_.businessEntity)
      val amend: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.amend).map(_.businessEntity)
      val amendEoris = amend.map(_.businessEntityIdentifier)

      for {
        eori <- findEoriByUndertakingReference(undertakingRef)
        undertaking <- undertakingCache.get[Undertaking](eori).map { case Some(e) =>
          e //fixme handle None case
        }
        existingBusinessEntities = undertaking.undertakingBusinessEntity
        updated =
          existingBusinessEntities
            .diff(remove)
            .filterNot(p => amendEoris.contains(p.businessEntityIdentifier)) ++ add ++ amend
        _ = {
          val eorisInUndertaking = updated.collect(_.businessEntityIdentifier)
          if (eorisInUndertaking.size != eorisInUndertaking.distinct.size) {
            throw new IllegalStateException("trying assign eori to multiple undertakings")
          }
        }
        _ <- undertakingCache.put[Undertaking](eori, undertaking.copy(undertakingBusinessEntity = updated))
      } yield ()
    }
  }

  def updateUndertaking(
    undertakingRef: UndertakingRef,
    amendmentType: EisAmendmentType,
    undertakingName: Option[UndertakingName],
    sector: Sector
  )(implicit appConfig: AppConfig, headerCarrier: HeaderCarrier): Future[Unit] =
    amendmentType match {
      case EisAmendmentType.D => undertakingCache.deleteUndertaking(undertakingRef)
      case _ =>
        for {
          eori <- findEoriByUndertakingReference(undertakingRef)
          undertaking <- undertakingCache.get[Undertaking](eori).map {
            case Some(e) => e
          }
          _ <- undertakingCache.put[Undertaking](
            eori,
            undertaking
              .copy(industrySector = sector, industrySectorLimit = IndustrySectorLimit(appConfig.sectorCap(sector)))
          )
        } yield ()
    }

  def updateLastSubsidyUsage(
    undertakingRef: UndertakingRef,
    lastSubsidyUsageUpdt: LocalDate
  )(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    for {
      eori <- findEoriByUndertakingReference(undertakingRef)
      undertaking <- undertakingCache.get[Undertaking](eori).map {
        case Some(e) => e
      }
      _ <- undertakingCache.put[Undertaking](eori, undertaking.copy(lastSubsidyUsageUpdt = Some(lastSubsidyUsageUpdt)))
    } yield ()

  def updateSubsidies(undertakingRef: UndertakingRef, update: Update)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Unit] =
    update match {
      case _: NilSubmissionDate => Future.successful(())
      case UndertakingSubsidyAmendment(updates) =>
        // Setting amendmentType to None as it will not matter once that are stored in UndertakingSubsidies,
        // plus while retrieving, the amendmentType is not a part of the response
        val addList =
          updates
            .filter(_.amendmentType.contains(EisSubsidyAmendmentType("1")))
            .map(
              _.copy(
                amendmentType = None,
                subsidyUsageTransactionId = Some(SubsidyRef(s"Z${Random.alphanumeric.take(9).mkString}"))
              )
            )

        val amendList: List[NonHmrcSubsidy] =
          updates
            .filter(_.amendmentType.contains(EisSubsidyAmendmentType("2")))
            .map(_.copy(amendmentType = None))

        val removeList: List[NonHmrcSubsidy] =
          updates
            .filter(_.amendmentType.contains(EisSubsidyAmendmentType("3")))
            .map(_.copy(amendmentType = None))

        val removeSubsidyTransactionIds = removeList.map(_.subsidyUsageTransactionId)

        for {
          undertakingSubsidies <- retrieveAllSubsidies(undertakingRef)
          currentNonHMRCSubsidyList: List[NonHmrcSubsidy] = undertakingSubsidies.nonHMRCSubsidyUsage
          updatedList = getUpdatedList(amendList, currentNonHMRCSubsidyList)
          filteredUpdatedList =
            updatedList.filterNot(sub =>
              removeSubsidyTransactionIds.contains(sub.subsidyUsageTransactionId)
            ) ++ addList // Updating and removing the currentNonHMRCSubsidyList by subsidyUsageTransactionId
          updatedTotal = filteredUpdatedList.map(_.nonHMRCSubsidyAmtEUR).fold(BigDecimal(0))((acc, n) => acc + n)
          updatedSubsidies =
            undertakingSubsidies.copy(
              nonHMRCSubsidyUsage = filteredUpdatedList,
              nonHMRCSubsidyTotalEUR = SubsidyAmount(updatedTotal)
            )
          eori <- findEoriByUndertakingReference(undertakingRef)
        } yield undertakingCache.put[UndertakingSubsidies](eori, updatedSubsidies)
    }

  def getUndertakingBalance(eori: EORI)(implicit hc: HeaderCarrier): Future[Option[UndertakingBalance]] = {
    for {
      undertaking <- retrieveUndertaking(eori)
      subsidies <- retrieveUndertakingSubsidiesByEori(eori)
    } yield {
      (undertaking, subsidies) match {
        case (Some(u), us @ _) =>
          Some(UndertakingBalance(u, us.getOrElse(UndertakingSubsidies.emptyInstance(u.reference))))
        case _ => None
      }
    }
  }

  def retrieveAllSubsidies(
    undertakingRef: UndertakingRef
  )(implicit hc: HeaderCarrier): Future[UndertakingSubsidies] =
    retrieveSubsidies(SubsidyRetrieve(undertakingRef, Option.empty))

  private def retrieveSubsidies(
    subsidyRetrieve: SubsidyRetrieve
  )(implicit hc: HeaderCarrier): Future[UndertakingSubsidies] = {
    for {
      eori <- findEoriByUndertakingReference(subsidyRetrieve.undertakingIdentifier)
      subsidies <- undertakingCache.get[UndertakingSubsidies](eori)
    } yield subsidies.getOrElse(UndertakingSubsidies.emptyInstance(subsidyRetrieve.undertakingIdentifier))
  }

  private def findEoriByUndertakingReference(
    undertakingRef: UndertakingRef
  )(implicit hc: HeaderCarrier): Future[EORI] = {
    undertakingCache.findUndertakingEoriByUndertakingRef(undertakingRef).map {
      case Some(e) => e
      case None => throw new IllegalStateException(s"No undertaking for undertaking reference: $undertakingRef")
    }
  }

  private def getUpdatedList(
    amendList: List[NonHmrcSubsidy],
    currentList: List[NonHmrcSubsidy]
  ): List[NonHmrcSubsidy] =
    if (amendList.isEmpty) currentList
    else {
      for {
        amendData <- amendList
        currentData <- currentList
        hasChanges = amendData.subsidyUsageTransactionId == currentData.subsidyUsageTransactionId
      } yield
        if (hasChanges) {
          currentData
            .copy(
              publicAuthority = amendData.publicAuthority,
              traderReference = amendData.traderReference,
              nonHMRCSubsidyAmtEUR = amendData.nonHMRCSubsidyAmtEUR,
              businessEntityIdentifier = amendData.businessEntityIdentifier
            )
        } else {
          currentData
        }
    }

}
