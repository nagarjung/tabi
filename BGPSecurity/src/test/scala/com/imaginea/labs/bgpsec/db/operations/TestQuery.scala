package com.imaginea.labs.bgpsec.db.operations

import java.util.UUID

import com.imaginea.labs.bgpsec.db.configs.DBConfig
import com.imaginea.labs.bgpsec.db.models.{BGPUpdateHistoryCache, BGPUpdateHistoryCacheDao}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.runtime.Nothing$

/**
  * Created by bipulk on 4/14/17.
  */
object TestQuery extends App {

  /*val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)

  var obj = BGPUpdateHistoryCache(UUID.randomUUID(), "212", 1211, 1212, None)
  val f2 = bGPUpdateHistoryCacheDao.insert(obj)


  println(Await.result(f2, 10.seconds))

  //val f3 = bGPUpdateHistoryCacheDao.updateById(obj.id, obj.copy(withdrawalTimeStamp = Option(2234)))
  //println(Await.result(f3, 10.seconds))*/

  val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)
  val result = bGPUpdateHistoryCacheDao.queryByIPSorted("212")

  println(Await.result(result, 10.seconds).toList)

}

class Test {

  def test(): (Boolean, BGPUpdateHistoryCache) = {

    val bgpUpdateHistoryCache: BGPUpdateHistoryCache = BGPUpdateHistoryCache("asa", "asas", 2323, 2323, Option(12112))

    (true, bgpUpdateHistoryCache)

  }

}