# Steps of setup

## 1) Checkout Project
	git clone https://gitlab.pramati.com/bipul.kumar/BGPSecurity.git

## 2) Setup MySql Docker
	Refer https://hub.docker.com/r/mysql/mysql-server/
	or refer mysqlsetup.sh

	NOTE: Use the new password in application.conf generated during step 1

## 3) Compile Project
	sbt compile

## 4) Run Project
	sbt run

    Multiple main classes detected, select one to run:

      [1] com.imaginea.labs.bgpsec.api.HistoryBasedValidationApi
      [2] com.imaginea.labs.bgpsec.db.operations.CreateDatabase
      [3] com.imaginea.labs.bgpsec.spark.HistoryBasedFileProcessingJob


    Enter number:<1/2/3> (enter any one)

	2 :: for creating database
	3 :: to run Spark Streaming job (configure the directory to watch in application.conf)
	1 :: to start the API server (sample web api http://localhost:4567/validate?ipPrefix=2a0a:7500::/48)
	
## 5) Build Fat Jar
	sbt assembly

## 5) Run Standalone
	java -cp target/scala-2.11/BGPSecHistoryService.jar com.imaginea.labs.bgpsec.db.operations.CreateDatabase
	java -cp target/scala-2.11/BGPSecHistoryService.jar com.imaginea.labs.bgpsec.spark.HistoryBasedFileProcessingJob
	java -cp target/scala-2.11/BGPSecHistoryService.jar com.imaginea.labs.bgpsec.api.HistoryBasedValidationApi
	
	
	