package com.imaginea.labs.bgpsec.api;

import com.imaginea.labs.bgpsec.db.models.BGPUpdateHistoryCache;
import com.imaginea.labs.bgpsec.historyalgo.BGPSecHistoryAlgo;
import scala.Option;
import scala.Tuple2;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.StringWriter;

/**
 * Created by bipulk on 4/17/17.
 */

 /*class HistoryBasedValidationApi {

    public static void main(String[] args) {

       *//* Spark.get("/validate", (request, response) -> {

            StringWriter writer = new StringWriter();
            try {


                String ipPrefix = request.queryParams("ipPrefix").trim();// params(":ipPrefix");

                Tuple2 bgpUpdateHistoryCache = BGPSecHistoryAlgo.getLastestBestStableEntry(ipPrefix);

                writer.append(toJson(bgpUpdateHistoryCache));

                return writer;

            } catch (Exception ex) {

                ex.printStackTrace();
                writer.append("{\"status\" : \"error\"}");

                return writer;
            }

        });*//*


        new ValidationController();

    }


}*/

public class ValidationController {

    ValidationController() {

        Spark.get("/validate", new Route() {

            @Override

            public Object handle(Request request, Response response) {

                StringWriter writer = new StringWriter();
                try {


                    String ipPrefix = request.queryParams("ipPrefix").trim();// params(":ipPrefix");

                    Tuple2 bgpUpdateHistoryCache = BGPSecHistoryAlgo.getLastestBestStableEntry(ipPrefix);

                    writer.append(toJson(bgpUpdateHistoryCache));

                    return writer;

                } catch (Exception ex) {

                    ex.printStackTrace();
                    writer.append("{\"status\" : \"error\"}");

                    return writer;
                }

            }

        });

// more routes

    }

    public static String toJson(Tuple2 bgpUpdateHistoryCache) {

        if (bgpUpdateHistoryCache._2() != null) {

            StringBuffer stringBuffer = new StringBuffer("");

            stringBuffer.append("{");

            stringBuffer.append("\"status\" : " + bgpUpdateHistoryCache._1() + " ,");

            stringBuffer.append("\"id\" : \"" + ((BGPUpdateHistoryCache) (bgpUpdateHistoryCache._2())).id() + "\" ,");

            stringBuffer.append("\"ipPrefix\" : \"" + ((BGPUpdateHistoryCache) (bgpUpdateHistoryCache._2())).ipPrefix() + "\" ,");

            stringBuffer.append("\"originAs\" : " + ((BGPUpdateHistoryCache) (bgpUpdateHistoryCache._2())).originAs() + " ,");

            stringBuffer.append("\"advertismentTimeStamp\" : " + ((BGPUpdateHistoryCache) (bgpUpdateHistoryCache._2())).advertismentTimeStamp() + " ,");

            Option<Object> longOption = ((BGPUpdateHistoryCache) (bgpUpdateHistoryCache._2())).withdrawalTimeStamp();

            stringBuffer.append("\"withdrawalTimeStamp\" : " + (longOption.isDefined() ? longOption.get() : "null") + "");

            stringBuffer.append("}");

            return stringBuffer.toString();
        } else {
            return "{\"status\" : \"false\"}";
        }

    }

}