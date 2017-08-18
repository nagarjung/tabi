package com.imaginea.labs.bgpsec.db.models

/**
  * Created by bipulk on 4/12/17.
  */

case class UpdateEntry(updateType: String, timeStamp: Long, ipPrefix: String, originAs: Int) {
  override def equals(obj: scala.Any): Boolean = {
    if (obj.isInstanceOf[UpdateEntry]) {

      val otherObj = obj.asInstanceOf[UpdateEntry]

      (this.updateType.equals(otherObj.updateType) && this.ipPrefix.equals(otherObj.ipPrefix) && this.originAs.equals(otherObj.originAs))

    } else {

      false

    }
  }


}