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

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.{Filters, IndexOptions, Indexes, Updates}
import play.api.libs.json.{JsValue, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancestub.models.{BusinessEntity, SubsidyRetrieve, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancestub.models.types.{EORI, IndustrySectorLimit, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancestub.repositories.UndertakingCache.DefaultCacheTtl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag
import uk.gov.hmrc.eusubsidycompliancestub.repositories.PersistenceHelpers.dataKeyForType

import java.time.LocalDate

@Singleton
class UndertakingCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository[EORI](
      mongoComponent = mongoComponent,
      collectionName = "undertakingCache",
      ttl = DefaultCacheTtl,
      timestampSupport = new CurrentTimestampSupport,
      cacheIdType = EoriIdType
    ) {

  private val UndertakingReference = "data.Undertaking.reference"
  private val UndertakingSubsidiesIdentifier = "data.UndertakingSubsidies.undertakingIdentifier"
  private val industrySectorLimit = "data.Undertaking.industrySectorLimit"

  // Ensure additional indexes for undertaking and undertaking subsidies deletion are present.
  private lazy val indexedCollection: Future[MongoCollection[CacheItem]] =
    for {
      _ <- collection
        .createIndex(
          Indexes.ascending(UndertakingReference),
          IndexOptions()
            .background(false)
            .name("undertakingReference")
            .sparse(false)
            .unique(false)
        )
        .headOption()
      _ <- collection
        .createIndex(
          Indexes.ascending(UndertakingSubsidiesIdentifier),
          IndexOptions()
            .background(false)
            .name("undertakingSubsidiesIdentifier")
            .sparse(false)
            .unique(false)
        )
        .headOption()
      _ <- collection
        .createIndex(
          Indexes.ascending(industrySectorLimit),
          IndexOptions()
            .background(false)
            .name("sectorLimit")
            .sparse(false)
            .unique(false)
        )
        .headOption()
    } yield collection

  def get[A : ClassTag](eori: EORI)(implicit reads: Reads[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    indexedCollection.flatMap { _ =>
      super
        .get[A](eori)(dataKeyForType[A])
    }
  }

  def findUndertakingByEori(eori: EORI)(implicit headerCarrier: HeaderCarrier): Future[Option[Undertaking]] = {
    indexedCollection.flatMap { c =>
      c.find(
        filter = Filters.equal("data.Undertaking.undertakingBusinessEntity.businessEntityIdentifier", eori)
      ).toFuture()
        .map { items: Seq[CacheItem] =>
          if (items.isEmpty) None
          else
            items.headOption.flatMap { i =>
              val data = i.data.as[Map[String, JsValue]]
              data.get("Undertaking").map(u => u.as[Undertaking])
            }
        }
    }
  }

  def findUndertakingEoriByUndertakingRef(
    ref: UndertakingRef
  )(implicit headerCarrier: HeaderCarrier): Future[Option[EORI]] = {
    indexedCollection.flatMap { c =>
      c.find(
        filter = Filters.equal(UndertakingReference, ref)
      ).toFuture()
        .map { items: Seq[CacheItem] =>
          if (items.isEmpty) None
          else
            items.headOption.map { i =>
              i.id.asInstanceOf[EORI]
            }
        }
    }
  }

  def updateUndertakingBusinessEntities(ref: UndertakingRef, businessEntities: List[BusinessEntity]): Future[Unit] = {
    indexedCollection.flatMap { c =>
      c.updateOne(
        filter = Filters.equal(UndertakingReference, ref),
        update = Updates.set("data.Undertaking.undertakingBusinessEntity", businessEntities)
      ).toFuture()
        .map(_ => ())
    }

  }

  def put[A](eori: EORI, in: A)(implicit
    writes: Writes[A],
    classTag: ClassTag[A],
    headerCarrier: HeaderCarrier
  ): Future[A] = {
    indexedCollection.flatMap { _ =>
      super
        .put[A](eori)(DataKey(in.getClass.getSimpleName), in)
        .map(_ => in)
    }
  }

  def deleteUndertaking(ref: UndertakingRef)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    indexedCollection.flatMap { c =>
      c.deleteOne(
        filter = Filters.equal(UndertakingReference, ref)
      ).toFuture()
        .map(_ => ())
    }

  }

  def deleteUndertakingSubsidies(ref: UndertakingRef)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    indexedCollection.flatMap { c =>
      c.updateMany(
        filter = Filters.equal(UndertakingSubsidiesIdentifier, ref),
        update = Updates.unset("data.UndertakingSubsidies")
      ).toFuture()
        .map(_ => ())
    }
  }
}

object UndertakingCache {
  val DefaultCacheTtl: FiniteDuration = 14 days
}
