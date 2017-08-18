package com.imaginea.labs.bgpsec.historyalgo


import org.apache.commons.lang3.StringUtils

/**
  * Created by bipulk on 7/13/17.
  */
object UpdateEntryParser {

  def parse(entry: String): String = {

    val updateType = StringUtils.substringBetween(entry, "type=", ", timestamp=")
    val timestamp = StringUtils.substringBetween(entry, "timestamp=", ".0, collector=")
    val prefix = StringUtils.substringBetween(entry, "prefix=", ", origin=")
    val origin = StringUtils.substringBetween(entry, "origin=", ", as_path=")

    return s"($updateType, $timestamp, $prefix, $origin)"
  }

}
