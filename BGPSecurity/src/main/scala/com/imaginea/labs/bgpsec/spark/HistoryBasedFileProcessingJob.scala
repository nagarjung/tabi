package com.imaginea.labs.bgpsec.spark

/**
  * Created by bipulk on 4/10/17.
  */

import com.imaginea.labs.bgpsec.db.models.UpdateEntry
import com.imaginea.labs.bgpsec.historyalgo.{BGPSecHistoryAlgo, UpdateEntryParser}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{HashPartitioner, SparkConf}
import scala.collection.mutable
import scala.util.Try

object HistoryBasedFileProcessingJob {

  def main(args: Array[String]): Unit = {

    import com.typesafe.config.ConfigFactory
    val conf = ConfigFactory.load()

    val directoryToMonitor = conf.getString("spark.streaming.directoryToMonitor")
    val microBatchTime = conf.getInt("spark.streaming.microBatchTime")
    val sparkConf = new SparkConf().setAppName("HDFSFileStream").setMaster("local[4]")
    val sparkStreamingContext = new StreamingContext(sparkConf, Seconds(microBatchTime))

    println("Value of microBatchTime " + microBatchTime)
    println("DirectoryToMonitor " + directoryToMonitor)

    val directoryStream = sparkStreamingContext.textFileStream(directoryToMonitor)

    println("After starting directoryStream")

    directoryStream.foreachRDD { fileRdd => {
      if (fileRdd.count() != 0) {
        Try(processNewFile(fileRdd))
      } else {

        println("No data for this interval")

      }
    }
    }

    sparkStreamingContext.start()
    sparkStreamingContext.awaitTermination()
    println("Exiting HDFSFileStream.main")
  }

  def processNewFile(fileRDD: RDD[String]): Unit = {
    println("Entering processNewFile ")


    val rdd = fileRDD.map(x => UpdateEntryParser.parse(x)).map(line => line.substring(1, line.length - 1))
      .map(x => x.split(",")).map(x => (x(2), (x(0), x(1), x(2), x(3))))

    //val numberOfParallism = rdd.groupByKey().count().toInt

    val numberOfParallism = 16

    val repartionedRdd = rdd.partitionBy(new HashPartitioner(numberOfParallism))
      .map(x => UpdateEntry(x._2._1, x._2._2.trim.toLong, x._2._3.substring(3, x._2._3.length - 1), Try(x._2._4.trim.toInt).getOrElse(-1)))
      .foreachPartition(x => processPartition(x.toArray))
    //.saveAsTextFile("/home/bipulk/Spark_Streaming_out/" + System.currentTimeMillis() + "/")

    //println("############## " + numberOfParallism)

    println("Exiting processNewFile ")
  }

  private def processPartition(data: Array[UpdateEntry]): Unit = {

    if (data.length > 0) {
      println("#####################")
      val stack = new mutable.Stack[UpdateEntry]

      data.sortBy(_.ipPrefix).foreach { x =>

        val currentStackTop = stack.headOption.getOrElse(None)

        (currentStackTop) match {

          case (None) => stack.push(x)

          case (_: UpdateEntry) => {

            if (x.equals(currentStackTop) && "\'U\'".equals(x.updateType)) {
              //donothing

            } else if (x.equals(currentStackTop) && "\'W\'".equals(x.updateType)) {
              stack.pop()
              stack.push(x)

            } else {
              stack.push(x)

            }
          }
        }
      }

      println("################" + stack.toList.reverse.size)
      stack.toList.reverse.foreach(BGPSecHistoryAlgo.updateDatabase)
      println("#####################")
    }
  }


}


