package com.imaginea.labs.bgpsec.db.operations


import java.util.UUID

import com.imaginea.labs.bgpsec.db.configs.DBConfig
import com.imaginea.labs.bgpsec.db.models.{BGPUpdateHistoryCache, BGPUpdateHistoryCacheDao}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by bipulk on 4/11/17.
  */
object CreateDatabase extends App {


  val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)
  val f1 = bGPUpdateHistoryCacheDao.createSchema
  //val f2 = bGPUpdateHistoryCacheDao.insert(BGPUpdateHistoryCache(1212,"212",1211,1212,None))
  // val f3 = bGPUpdateHistoryCacheDao.insert(BGPUpdateHistoryCache(1213,"212",1211,1212,None))

  println(Await.result(f1, 10.seconds))
  /* println(Await.result(f2, 10.seconds))
   println(Await.result(f3, 10.seconds))*/

  /*var obj = BGPUpdateHistoryCache(UUID.randomUUID().toString, "212", 1211, 1212, None)
  val f2 = bGPUpdateHistoryCacheDao.insert(obj)
  val f3 = bGPUpdateHistoryCacheDao.updateById(obj.id,obj.copy(withdrawalTimeStamp = Option(223)))


  println(Await.result(f2, 10.seconds))
  println(Await.result(f3, 10.seconds))
*/

}




