package com.imaginea.labs.bgpsec.db.configs

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

/**
  * Created by bipulk on 4/11/17.
  */
object DBConfig {
  val configMySql = DatabaseConfig.forConfig[JdbcProfile]("db")
}

