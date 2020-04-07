package com.knoldus.lagomkafkacassandraes.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import com.knoldus.ProductCommands
import com.knoldus.constants.Queries
import com.knoldus.lagomkafkacassandraes.api.{Product, ProductApi}
import com.knoldus.lagomkafkacassandraes.impl.eventsourcing.ProductEntity
import com.knoldus.lagomkafkacassandraes.impl.eventsourcing.commands.AddProduct
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRef, PersistentEntityRegistry}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implementation of the ProductApi
  */
class ProductImpl(
                   persistentEntityRegistry: PersistentEntityRegistry, session: CassandraSession
                 )(implicit ec: ExecutionContext)
  extends ProductApi {

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def getProductDetails(id: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    getProductById(id).map(product => s"Product named ${product.get.name} has id: $id and quantity in inventory: ${product.get.quantity}")
  }

  def getProductById(id: String): Future[Option[Product]] =
    session.selectOne(Queries.GET_PRODUCT,id).map { rows =>
      rows.map { row =>
        val id = row.getString("id")
        val name = row.getString("name")
        val quantity = row.getLong("quantity")
        Product(id, name, quantity)
      }
    }

  override def addProduct(): ServiceCall[Product, String] = ServiceCall { product =>
    ref(product.id).ask(AddProduct(product)).map {
      case Done =>
        s"${product.name} is added"
    }
  }

  def ref(id: String): PersistentEntityRef[ProductCommands[_]] = {
    persistentEntityRegistry
      .refFor[ProductEntity](id)
  }

}