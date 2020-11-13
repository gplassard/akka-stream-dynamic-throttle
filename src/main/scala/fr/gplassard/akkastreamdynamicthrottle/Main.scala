package fr.gplassard.akkastreamdynamicthrottle

import java.util.concurrent.atomic.AtomicLong

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Attributes
import akka.stream.Attributes.LogLevels
import akka.stream.contrib.TokenThrottle
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem("akkassembly")
  implicit val loggingAdapter = system.log
  import system.dispatcher

  println("hello world !")

  val consumeRate = new AtomicLong(1)   //on commence à 1/s
  system.scheduler.scheduleOnce(20 seconds, new Runnable {
    override def run(): Unit = consumeRate.set(3)   //après 20s on passe à 3/s
  })
  system.scheduler.scheduleOnce(40 seconds,  new Runnable {
    override def run(): Unit = consumeRate.set(5)   //après 40s on passe à 5/s
  })

  val start = System.currentTimeMillis()

  val tokens = Source.tick(0 seconds, 5 seconds, NotUsed)   //Fréquence de refresh du throttle
    .map(_ => {
      val throttle = consumeRate.get()
      println(s"Get throttle rate $throttle/s at ${System.currentTimeMillis() - start}ms")
      throttle    //nouvelle valeur de throttle
    })
    .expand(throttle => Iterator.continually(throttle))   //répète la valeur tant qu'il n'y en a pas de nouvelle
    .log("tokens", t => s"Throttle actuel ${t}/s")
    .throttle(1, 1 second)

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
