/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.Equality
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eusubsidycompliancestub.BaseSpec
import uk.gov.hmrc.eusubsidycompliancestub.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{AmendmentType, EORI, EisAmendmentType, EisSubsidyAmendmentType, Sector}
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, BusinessEntityUpdate, NilSubmissionDate, NonHmrcSubsidy, Undertaking, UndertakingSubsidies, UndertakingSubsidyAmendment}
import uk.gov.hmrc.eusubsidycompliancestub.repositories.UndertakingCache
import uk.gov.hmrc.eusubsidycompliancestub.util.TestInstances.{arbContactDetails, arbEori, arbSubsidies, arbUndertaking}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.smartstub.AdvGen

import java.time.LocalDate

class EscServiceSpec extends BaseSpec with BeforeAndAfterEach {
  private val escService = app.injector.instanceOf[EscService]
  private val undertakingCache = app.injector.instanceOf[UndertakingCache]
  private val mongoComponent = app.injector.instanceOf[MongoComponent]
  private implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "EscService" must {
    "create undertaking" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(escService.createUndertaking(eori, undertaking))
        await(undertakingCache.get[Undertaking](eori)) mustBe Some(undertaking)
      }
    }

    "delete undertaking" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        await(escService.deleteUndertaking(undertaking.reference))
        await(undertakingCache.get[Undertaking](eori)) mustBe None
      }
    }

    "retrieve undertaking by business entity EORI" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        undertaking.undertakingBusinessEntity.foreach { be =>
          await(escService.retrieveUndertaking(be.businessEntityIdentifier)) mustBe Some(undertaking)
        }
      }
    }

    "delete undertaking if updateUndertakingBusinessEntities is called with a delete of the lead EORI" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        val update =
          BusinessEntityUpdate(AmendmentType.delete, LocalDate.now(), BusinessEntity(eori, leadEORI = true, None))
        await(escService.updateUndertakingBusinessEntities(undertaking.reference, List(update)))
        await(undertakingCache.get[Undertaking](eori)) mustBe None
      }
    }

    "update undertaking business entities with delete/add/amend" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        val existingEntities = undertaking.undertakingBusinessEntity.tail
        val deletedEntity = Gen.oneOf(existingEntities).sample
        val amendedEntities =
          existingEntities
            .filterNot(deletedEntity.contains)
            .map(entity => entity.copy(contacts = Some(arbContactDetails.arbitrary.sample.get)))
        val amendments =
          deletedEntity.toList.flatMap(e => List(BusinessEntityUpdate(AmendmentType.delete, LocalDate.now(), e))) ++
            amendedEntities.map(e => BusinessEntityUpdate(AmendmentType.amend, LocalDate.now(), e))
        await(escService.updateUndertakingBusinessEntities(undertaking.reference, amendments))
        val actualEntities = await(
          undertakingCache.findUndertakingByEori(undertaking.undertakingBusinessEntity.head.businessEntityIdentifier)
        ).get.undertakingBusinessEntity
        val expectedEntities = undertaking.undertakingBusinessEntity.headOption.toList ++ amendedEntities
        actualEntities.toSet mustBe expectedEntities.toSet
      }
    }

    "delete undertaking if updateUndertaking is called with Delete" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        await(escService.updateUndertaking(undertaking.reference, EisAmendmentType.D, None, null))
        await(undertakingCache.get[Undertaking](eori)) mustBe None
      }
    }

    "update undertaking" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        val updatedSector: Sector = Gen.oneOf(Sector.values - undertaking.industrySector).sample.get
        await(
          escService.updateUndertaking(undertaking.reference, EisAmendmentType.A, None, updatedSector)
        )
        val actualUndertaking = await(undertakingCache.get[Undertaking](eori)).get
        actualUndertaking.industrySector mustBe updatedSector
        actualUndertaking.industrySectorLimit mustBe appConfig.sectorCap(updatedSector)
      }
    }

    "update lastSubsidyUsage" in {
      implicit val arbDate: Arbitrary[LocalDate] =
        Arbitrary(Gen.date(LocalDate.now().minusYears(5), LocalDate.now().plusYears(5)))
      forAll { (eori: EORI, undertaking: Undertaking, date: LocalDate) =>
        await(undertakingCache.put(eori, undertaking))
        await(escService.updateLastSubsidyUsage(undertaking.reference, date))
        await(undertakingCache.get[Undertaking](eori)) mustBe Some(undertaking.copy(lastSubsidyUsageUpdt = Some(date)))
      }
    }

    "ignore the operation if update subsidies is for NilSubmission" in {
      forAll { (eori: EORI, undertaking: Undertaking) =>
        await(undertakingCache.put(eori, undertaking))
        val update = NilSubmissionDate(LocalDate.now)
        await(escService.updateSubsidies(undertaking.reference, update))
      }
    }

    "add subsidies when updateSubsidies is called" in {
      implicit val equalsWithIgnore: Equality[NonHmrcSubsidy] = { case (a, b: NonHmrcSubsidy) =>
        a.copy(subsidyUsageTransactionId = b.subsidyUsageTransactionId, amendmentType = b.amendmentType) == b
      }
      forAll { (eori: EORI, undertaking: Undertaking, subsidies: List[NonHmrcSubsidy]) =>
        await(undertakingCache.put(eori, undertaking))
        val update = UndertakingSubsidyAmendment(
          subsidies.map(_.copy(amendmentType = Some(EisSubsidyAmendmentType("1"))))
        )
        await(escService.updateSubsidies(undertaking.reference, update))
        val actuals = await(undertakingCache.get[UndertakingSubsidies](eori)).get.nonHMRCSubsidyUsage
        actuals must have size subsidies.length
        actuals.zip(subsidies).foreach { case (actual, expected) =>
          actual mustEqual expected
        }
      }
    }
  }

  override def beforeEach(): Unit = {
    await(mongoComponent.database.getCollection(undertakingCache.collectionName).drop().toFuture())
  }
}
