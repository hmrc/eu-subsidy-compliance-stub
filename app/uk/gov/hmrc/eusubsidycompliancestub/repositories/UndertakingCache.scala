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

package uk.gov.hmrc.eusubsidycompliancestub.repositories

import cats.implicits.toFunctorOps
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.libs.json.{JsValue, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.repositories.UndertakingCache._
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag
import uk.gov.hmrc.eusubsidycompliancestub.repositories.PersistenceHelpers.dataKeyForType

@Singleton
class UndertakingCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository[EORI](
      mongoComponent = mongoComponent,
      collectionName = "undertakingCache",
      ttl = DefaultCacheTtl,
      timestampSupport = new CurrentTimestampSupport,
      cacheIdType = EoriIdType,
      extraIndexes = Seq(
        undertakingCacheIndex(UndertakingReference, "undertakingReference"),
        undertakingCacheIndex(UndertakingSubsidiesIdentifier, "undertakingSubsidiesIdentifier"),
        undertakingCacheIndex(industrySectorLimit, "sectorLimit")
      ),
      replaceIndexes = true
    ) {

  def get[A : ClassTag](eori: EORI)(implicit reads: Reads[A]): Future[Option[A]] = {
    super.get[A](eori)(dataKeyForType[A])
  }

  def findUndertakingByEori(eori: EORI): Future[Option[Undertaking]] = {
    collection
      .find(
        filter = Filters.equal("data.Undertaking.undertakingBusinessEntity.businessEntityIdentifier", eori)
      )
      .toFuture()
      .map { items: Seq[CacheItem] =>
        items.headOption.flatMap { i =>
          val data = i.data.as[Map[String, JsValue]]
          data.get("Undertaking").map(u => u.as[Undertaking])
        }
      }
  }

  def findUndertakingEoriByUndertakingRef(
    ref: UndertakingRef
  ): Future[Option[EORI]] = {
    collection
      .find(
        filter = Filters.equal(UndertakingReference, ref)
      )
      .toFuture()
      .map { items: Seq[CacheItem] =>
        items.headOption.map { i =>
          i.id.asInstanceOf[EORI]
        }
      }
  }

  def put[A](eori: EORI, in: A)(implicit
    writes: Writes[A]
  ): Future[A] = {
    super
      .put[A](eori)(DataKey(in.getClass.getSimpleName), in)
      .as(in)
  }

  def deleteUndertaking(ref: UndertakingRef): Future[Unit] = {
    collection
      .deleteOne(
        filter = Filters.equal(UndertakingReference, ref)
      )
      .toFuture()
      .void
  }
}

object UndertakingCache {
  private val DefaultCacheTtl: FiniteDuration = 14 days
  private val UndertakingReference = "data.Undertaking.reference"
  private val UndertakingSubsidiesIdentifier = "data.UndertakingSubsidies.undertakingIdentifier"
  private val industrySectorLimit = "data.Undertaking.industrySectorLimit"
  private def undertakingCacheIndex(field: String, name: String): IndexModel =
    IndexModel(
      Indexes.ascending(field),
      IndexOptions().background(false).name(name).sparse(false).unique(false)
    )

}
