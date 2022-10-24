/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, EisAmendmentType, EisSubsidyAmendmentType, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models._

import java.time.LocalDate
import scala.util.Random

object Store {

  def mutable[K, V] =
    collection.concurrent.TrieMap.empty[K, V]

  def clear() = {
    undertakings.undertakingStore.clear()
    subsidies.subsidyStore.clear()
    isEmpty
  }

  def isEmpty: Boolean =
    undertakings.undertakingStore.keys.toList.isEmpty &&
      subsidies.subsidyStore.toList.isEmpty

  object undertakings {

    private val SectorLimits = Map(
      Sector.agriculture -> IndustrySectorLimit(30000.00),
      Sector.aquaculture -> IndustrySectorLimit(20000.00),
      Sector.other -> IndustrySectorLimit(200000.00),
      Sector.transport -> IndustrySectorLimit(100000.00)
    )

    def put(undertaking: Undertaking): Unit =
      undertakingStore.put(undertaking.reference.get, undertaking)

    def updateUndertaking(
      undertakingRef: UndertakingRef,
      amendmentType: EisAmendmentType,
      undertakingName: Option[UndertakingName],
      sector: Option[Sector]
    ): Unit =
      amendmentType match {
        case EisAmendmentType.D =>
          undertakingStore.remove(undertakingRef)
        case _ =>
          retrieve(undertakingRef).foreach { u =>
            val updatedSector = sector.getOrElse(u.industrySector)
            undertakingStore.update(
              u.reference.get,
              u.copy(
                name = undertakingName.getOrElse(u.name),
                industrySector = updatedSector,
                industrySectorLimit = SectorLimits.get(updatedSector)
              )
            )
          }
      }

    def updateLastSubsidyUsage(
      undertakingRef: UndertakingRef,
      lastSubsidyUsageUpdt: LocalDate
    ): Unit =
      retrieve(undertakingRef).foreach { u =>
        val updatedUndertaking = u.copy(
          lastSubsidyUsageUpdt = Some(lastSubsidyUsageUpdt)
        )
        undertakingStore.update(u.reference.get, updatedUndertaking)
      }

    def updateUndertakingBusinessEntities(
      undertakingRef: UndertakingRef,
      updates: List[BusinessEntityUpdate]
    ): Unit = {
      val businessEntities: List[BusinessEntity] = retrieve(undertakingRef).get.undertakingBusinessEntity

      val remove: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.delete).map(_.businessEntity)

      if (!remove.filter(_.leadEORI).isEmpty) {
        undertakingStore.remove(undertakingRef)
      } else {
        val add: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.add).map(_.businessEntity)
        val amend: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.amend).map(_.businessEntity)

        val amendEoris = amend.map(_.businessEntityIdentifier)

        val updated: List[BusinessEntity] =
          businessEntities.diff(remove).filterNot(p => amendEoris.contains(p.businessEntityIdentifier)) ++ add ++ amend

        if (updated.forall(_.leadEORI == false)) {
          throw new IllegalStateException("there must be a lead BusinessEntity") // TODO - no EIS err for this!
        }
        overwriteBusinessEntities(undertakingRef, updated)
      }
    }

    private def overwriteBusinessEntities(
      undertakingRef: UndertakingRef,
      businessEntities: List[BusinessEntity]
    ): Unit = retrieve(undertakingRef).foreach { u =>
      if (
        businessEntities.forall(be =>
          retrieveByEori(be.businessEntityIdentifier).isEmpty ||
            retrieveByEori(be.businessEntityIdentifier).fold(true) { undertaking =>
              undertaking.reference.get == undertakingRef
            }
        )
      ) {

        val ed = u.copy(
          undertakingBusinessEntity = businessEntities
        )
        undertakingStore.update(u.reference.get, ed)
      } else {
        throw new IllegalStateException("trying assign eori to multiple undertakings")
      }

    }

    def retrieve(ref: UndertakingRef): Option[Undertaking] =
      undertakingStore.get(ref)

    def retrieveByEori(eori: EORI): Option[Undertaking] =
      undertakingStore.values.find(x => x.undertakingBusinessEntity.map(_.businessEntityIdentifier).contains(eori))

    val undertakingStore = mutable[UndertakingRef, Undertaking]

  }

  object subsidies {

    def put(undertakingSubsidies: UndertakingSubsidies): Unit =
      subsidyStore.put(undertakingSubsidies.undertakingIdentifier, undertakingSubsidies)

    def retrieveSubsidies(ref: UndertakingRef): Option[UndertakingSubsidies] = subsidyStore.get(ref)

    def updateSubsidies(undertakingRef: UndertakingRef, update: Update) =
      update match {
        case _: NilSubmissionDate => ()
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

          // If there is no undertakingSubsidies for the given undertakingRef, it's creating a dummy placeholder
          // Will be helpful in testing when undertakingSubsidies are not created. Can be removed later on
          val undertakingSubsidies =
            retrieveSubsidies(undertakingRef)
              .getOrElse(UndertakingSubsidies.emptyInstance(undertakingRef))

          val currentNonHMRCSubsidyList: List[NonHmrcSubsidy] = undertakingSubsidies.nonHMRCSubsidyUsage

          // Updating and removing the currentNonHMRCSubsidyList by subsidyUsageTransactionId
          val updatedList = getUpdatedList(amendList, currentNonHMRCSubsidyList)
            .filterNot(sub => removeSubsidyTransactionIds.contains(sub.subsidyUsageTransactionId)) ++ addList

          val updatedTotal = updatedList.map(_.nonHMRCSubsidyAmtEUR).fold(BigDecimal(0))((acc, n) => acc + n)

          val updatedSubsidies = undertakingSubsidies.copy(
            nonHMRCSubsidyUsage = updatedList,
            nonHMRCSubsidyTotalEUR = SubsidyAmount(updatedTotal)
          )

          put(updatedSubsidies)
      }

    /**
      * This function compare the amend list from the request with the current NonHmrcSubsidy list.
      * Look for the subsidyUsageTransactionId, if a match is found , then it's updated else kept the original list.
      * Duplicates are removed later when it's converted toSet.
      * @param amendList
      * @param currentList
      * @return List of updated NonHmrcSubsidy
      */
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

    val subsidyStore = mutable[UndertakingRef, UndertakingSubsidies]
  }

}
