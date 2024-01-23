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

import uk.gov.hmrc.eusubsidycompliancestub.models._
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, UndertakingRef}

object Store {

  def mutable[K, V] =
    collection.concurrent.TrieMap.empty[K, V]

  def clear() = {
    undertakings.undertakingStore.clear()
    subsidyStore.clear()
    isEmpty
  }

  def isEmpty: Boolean =
    undertakings.undertakingStore.keys.toList.isEmpty &&
      subsidyStore.toList.isEmpty

  object undertakings {
    def put(undertaking: Undertaking): Unit =
      undertakingStore.put(undertaking.reference, undertaking)

    def retrieveByEori(eori: EORI): Option[Undertaking] = {
      val values = undertakingStore.values

      values.find { undertaking =>
        undertaking.undertakingBusinessEntity.map(_.businessEntityIdentifier).contains(eori)
      }
    }

    val undertakingStore = mutable[UndertakingRef, Undertaking]
  }

  val subsidyStore = mutable[UndertakingRef, UndertakingSubsidies]
}
