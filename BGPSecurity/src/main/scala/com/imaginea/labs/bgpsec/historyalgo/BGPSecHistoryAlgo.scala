package com.imaginea.labs.bgpsec.historyalgo

import java.util.UUID

import com.imaginea.labs.bgpsec.db.configs.DBConfig
import com.imaginea.labs.bgpsec.db.models.{BGPUpdateHistoryCache, BGPUpdateHistoryCacheDao, UpdateEntry}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by bipulk on 4/15/17.
  */
object BGPSecHistoryAlgo {

  val STABLE_DURATION = 172800000L

  def updateDatabase(updateEntry: UpdateEntry) = {

    if ("\'U\'".equals(updateEntry.updateType)) {

      processUpdateEvent(updateEntry)

    } else {

      processWithdrawlEvent(updateEntry)

    }

  }


  private def processUpdateEvent(updateEntry: UpdateEntry): Unit = {

    val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)
    val result = bGPUpdateHistoryCacheDao.queryByIPOASSorted(updateEntry.ipPrefix, updateEntry.originAs)

    val list = Await.result(result, 10.seconds).toList

    if ((list.length == 0) || (list.length == 1 && !list(0).withdrawalTimeStamp.isEmpty)) {

      val newUpdateEntry = BGPUpdateHistoryCache(UUID.randomUUID().toString, updateEntry.ipPrefix, updateEntry.originAs, updateEntry.timeStamp, None)
      val f2 = bGPUpdateHistoryCacheDao.insert(newUpdateEntry)

      Await.result(f2, 10.seconds)

    }

  }

  private def processWithdrawlEvent(updateEntry: UpdateEntry): Unit = {

    val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)
    val result = bGPUpdateHistoryCacheDao.queryByIPSorted(updateEntry.ipPrefix)

    val list = Await.result(result, 10.seconds).toList


    list.map(_.originAs).distinct.foreach { x =>

      val subsetForAs = list.filter(_.originAs == x)

      if (subsetForAs.length > 1 && subsetForAs(1).withdrawalTimeStamp.isEmpty) {

        if (updateEntry.timeStamp - subsetForAs(1).advertismentTimeStamp >= STABLE_DURATION) {

          val updatedObj = subsetForAs(0).copy(withdrawalTimeStamp = Option(updateEntry.timeStamp))
          val result = bGPUpdateHistoryCacheDao.updateById(subsetForAs(1).id, updatedObj)
          Await.result(result, 10.seconds)

          val result1 = bGPUpdateHistoryCacheDao.deleteById(subsetForAs(0).id)
          Await.result(result1, 10.seconds)

        } else {

          val result = bGPUpdateHistoryCacheDao.deleteById(subsetForAs(1).id)
          Await.result(result, 10.seconds)

        }

      } else if (subsetForAs.length == 1 && subsetForAs(0).withdrawalTimeStamp.isEmpty) {

        val updatedObj = subsetForAs(0).copy(withdrawalTimeStamp = Option(updateEntry.timeStamp))
        val result = bGPUpdateHistoryCacheDao.updateById(subsetForAs(0).id, updatedObj)
        Await.result(result, 10.seconds)
      }

    }


  }


  def getLastestBestStableEntry(ipPrefix: String): (Boolean, BGPUpdateHistoryCache) = {

    val bGPUpdateHistoryCacheDao = new BGPUpdateHistoryCacheDao(DBConfig.configMySql)
    val result = bGPUpdateHistoryCacheDao.queryByIPSorted(ipPrefix)

    val list = Await.result(result, 10.seconds).toList

    if (list.length == 0) {

      (false, null)

    } else if (list.length == 1) {

      if (list(0).withdrawalTimeStamp.isEmpty) {

        (true, list(0))

      } else {

        if (list(0).withdrawalTimeStamp.get - list(0).advertismentTimeStamp >= STABLE_DURATION) {

          (true, list(0))
        } else {

          (false, list(0))
        }
      }

    } else {

      val modifiedList = list.map { x =>

        if (x.withdrawalTimeStamp.isEmpty) {
          x.copy(withdrawalTimeStamp = Option(System.currentTimeMillis()/1000))

        } else {
          x

        }

      }

      var latestBestStable = modifiedList(0)
      var stableFlag = false

      modifiedList.foreach { x =>

        if (x.withdrawalTimeStamp.get - x.advertismentTimeStamp >= STABLE_DURATION) {

          latestBestStable = x
          stableFlag = true

        }

      }

      if(stableFlag == false){

        (false,modifiedList(modifiedList.length -1))

      }else{

        (true, latestBestStable)
      }

    }

  }

}
