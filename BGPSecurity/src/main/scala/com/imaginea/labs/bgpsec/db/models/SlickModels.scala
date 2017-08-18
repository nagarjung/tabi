package com.imaginea.labs.bgpsec.db.models


import java.util.UUID

import com.google.gson.Gson
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

/**
  * Created by bipulk on 4/11/17.
  */

case class BGPUpdateHistoryCache(id: String, ipPrefix: String, originAs: Int, advertismentTimeStamp: Long, withdrawalTimeStamp: Option[Long])

class BGPUpdateHistoryCacheDao(val config: DatabaseConfig[JdbcProfile]) {

  import config.driver.api._


  class BGPUpdateHistoryCaches(tag: Tag) extends Table[BGPUpdateHistoryCache](tag, "BGP_UPDATE_HISTORY_CACHE") {
    // Columns
    def id = column[String]("ID", O.PrimaryKey)

    def ipPrefix = column[String]("IP_PREFIX", O.Length(100))

    def originAs = column[Int]("ORIGIN_AS")

    def advertismentTimeStamp = column[Long]("ADV_TIMESTAMP")

    def withdrawalTimeStamp = column[Option[Long]]("WTHDRWL_TIMESTAMP")

    // Indexes
    //def ipPrefixIndex = index("IP_PREFIX_IDX", ipPrefix, false)

    def ipPrefix_OAS_Index = index("IP_PREFIX_OAS_IDX", (ipPrefix, originAs), false)

    // Select
    def * = (id, ipPrefix, originAs, advertismentTimeStamp, withdrawalTimeStamp) <> (BGPUpdateHistoryCache.tupled, BGPUpdateHistoryCache.unapply)


  }

  val bgpUpdateHistoryCaches = TableQuery[BGPUpdateHistoryCaches]

  def createSchema = config.db.run(bgpUpdateHistoryCaches.schema.create)

  def insert(bGPUpdateHistoryCache: BGPUpdateHistoryCache) = config.db.run(bgpUpdateHistoryCaches += bGPUpdateHistoryCache)

  def deleteById(id: String) = config.db.run(bgpUpdateHistoryCaches.filter(_.id === id).delete)

  def updateById(id: String, bGPUpdateHistoryCache: BGPUpdateHistoryCache) =
    config.db.run(bgpUpdateHistoryCaches.filter(_.id === id).update(bGPUpdateHistoryCache.copy(id = id)))

  def queryByIPOASSorted(ipPrefix: String, originAs: Int) = config.db.run(bgpUpdateHistoryCaches
    .filter(_.ipPrefix === ipPrefix).filter(_.originAs === originAs).sortBy(_.advertismentTimeStamp).result)

  def queryByIPSorted(ipPrefix: String) = config.db.run(bgpUpdateHistoryCaches
    .filter(_.ipPrefix === ipPrefix).sortBy(_.advertismentTimeStamp).result)

}






