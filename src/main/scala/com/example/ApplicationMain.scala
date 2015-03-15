package com.example

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{ ExecutionContext, ExecutionContext$, Future, Promise, Await }
import scala.concurrent.duration._

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

object ApplicationMain extends App {
  val client = ElasticClient.remote("localhost", 9300)
  val resp1Future = client.execute {
      count from "prototype"
    }
  val resp2Future = client.execute {
      search in "megacorp"
    }
  for {
  	resp <- resp1Future
    resp2 <- resp2Future
  } yield {
  	println(resp)
  	println(resp2)
  }
  val system = ActorSystem("MyActorSystem")
  val pingActor = system.actorOf(PingActor.props, "pingActor")
  pingActor ! PingActor.Initialize
  // This example app will ping pong 3 times and thereafter terminate the ActorSystem - 
  // see counter logic in PingActor
  system.awaitTermination()

   Await.result(resp1Future, 5000 millis);
}