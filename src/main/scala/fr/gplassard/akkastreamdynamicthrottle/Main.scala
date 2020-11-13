package fr.gplassard.akkastreamdynamicthrottle


import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Attributes
import akka.stream.Attributes.LogLevels
import akka.stream.alpakka.dynamodb.scaladsl.DynamoDb
import akka.stream.contrib.TokenThrottle
import akka.stream.scaladsl.{Sink, Source}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem("akkassembly")
  implicit val loggingAdapter = system.log
  import system.dispatcher
  implicit val client: DynamoDbAsyncClient = DynamoDbAsyncClient.builder().build()

  val tableName = "tableName"

  val start = System.currentTimeMillis()

  val tokens = Source.tick(0 seconds, 5 seconds, NotUsed)   //Fréquence de refresh du throttle
    .mapAsync(1)(_ => {
      for {
        describe <- DynamoDb.single(DescribeTableRequest.builder().tableName(tableName).build())
        provisioned = describe.table().provisionedThroughput().readCapacityUnits()
      } yield (0.8 * provisioned).toLong
    })
    .expand(throttle => Iterator.continually(throttle))   //répète la valeur tant qu'il n'y en a pas de nouvelle
    .log("tokens", t => s"Throttle actuel ${t}/s")
    .throttle(1, 1 second)


  //20 pendant les 20 premières secondes
  //60 pendant les 20 secondes suivantes
  //20 derniers en 4 secondes
  //Completed in 45115ms -> on est plutôt bons !
  Source(1 to 100)
    .via(TokenThrottle(tokens)(_ => 1))
    //.log("items")
    .withAttributes(Attributes.logLevels(LogLevels.Info))
    .runWith(Sink.seq)
    .andThen { result =>
      val time = System.currentTimeMillis() - start
      println(s"Completed in ${time}ms with $result")
      system.terminate()
    }

}
