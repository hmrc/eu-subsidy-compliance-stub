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

package uk.gov.hmrc.eusubsidycompliancestub.services

import uk.gov.hmrc.eusubsidycompliancestub.models.types.EisAmendmentType.EisAmendmentType
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, EisAmendmentType, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, BusinessEntityUpdate, Undertaking, UndertakingSubsidies}

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

    def put(undertaking: Undertaking): Unit = {
      undertakingStore.put(undertaking.reference.get, undertaking)
    }

    def updateUndertaking(
      undertakingRef: UndertakingRef,
      amendmentType: EisAmendmentType,
      undertakingName: Option[UndertakingName],
      sector: Option[Sector]
    ): Unit = {
      amendmentType match {
        case EisAmendmentType.D =>
          undertakingStore.remove(undertakingRef)
        case _ =>
          retrieve(undertakingRef).foreach { u =>
            val ed = u.copy(
              name = undertakingName.getOrElse(u.name),
              industrySector = sector.getOrElse(u.industrySector)
            )
            undertakingStore.update(u.reference.get, ed)
          }
      }
    }


    def updateUndertakingBusinessEntities(
      undertakingRef: UndertakingRef,
      updates: List[BusinessEntityUpdate]
    ): Unit = {
      val businessEntities: List[BusinessEntity] = retrieve(undertakingRef).get.undertakingBusinessEntity
      val remove: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.delete).map(_.businessEntity)
      val add: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.add).map(_.businessEntity)
      val amend: List[BusinessEntity] = updates.filter(_.amendmentType == AmendmentType.amend).map(_.businessEntity)
      val updated: List[BusinessEntity] = businessEntities.diff(remove ++ amend) ++ add
      if (updated.forall(_.leadEORI == false)) {
        throw new IllegalStateException("there must be a lead BusinessEntity") // TODO - no EIS err for this!
      }
      overwriteBusinessEntities(undertakingRef, updated)
    }

    private def overwriteBusinessEntities(
      undertakingRef: UndertakingRef,
      businessEntities: List[BusinessEntity]
    ): Unit = retrieve(undertakingRef).foreach { u =>

      if (businessEntities.forall( be =>
        retrieveByEori(be.businessEntityIdentifier).isEmpty ||
          retrieveByEori(be.businessEntityIdentifier).fold(true){undertaking =>
            undertaking.reference.get == undertakingRef
          }
      )) {
        val ed = u.copy(
          undertakingBusinessEntity = businessEntities.toList
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

    val subsidyStore = mutable[UndertakingRef, UndertakingSubsidies]
  }

}



