package timeshift

import java.util.{Date, UUID}

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.{DocumentMap, DocumentSource, JacksonSource}
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsObject, JsString}
import timeshift.entity.TimedDocument

/**
 * @author Riccardo Merolla
 *         Created on 15/03/15.
 */
class CountTest extends FlatSpec with MockitoSugar with ElasticSugar with Matchers {

  val now = new Date

  val oldDate = "2014-01-01"

  case class Landmarks(name: String, timestamp: Date) extends DocumentMap with TimedDocument {
    override def map: Map[String, Any] =
      Map("uuid" -> uuid, "name" -> name, "timestamp" -> timestamp, "data-in" -> dataIn, "doc" -> source)

    val uuidValue: UUID = UUID.randomUUID()

    override def uuid: UUID = uuidValue

    override def source: Any =
      s"""
        |{"name": "$name", "timestamp": "$timestamp"}
      """.stripMargin

    override def dataIn: Date = new Date

    override def dataOut: Date = ???
  }
  val hampton = Landmarks("hampton court palace", now)

  client.execute {
    index into "london/landmarks" doc hampton
  }.await

  client.execute {
    index into "london/landmarks" fields (
      "name" -> "tower of london",
      "uuid" -> hampton.uuid,
      "timestamp" -> oldDate
      )
  }.await

  val jsonPub =
    """
      |{"name":"bull dogs"}
    """.stripMargin

  val mapper = new ObjectMapper

  client.execute {
    index into "london" -> "pubs" doc JacksonSource(mapper.readTree(jsonPub))
  }.await

  case class Pub(name: String) extends DocumentSource {
    import play.api.libs.json.Json

    implicit val pubWrites = Json.writes[Pub]

    //override def json: String = Json.stringify(Json.toJson(this).as[JsObject] + ("test", JsString("testString")))
    override def json: String = Json.stringify(Json.parse(s"""
        {
          "name" : "$name",
          "location" : {
            "lat" : 51.235685,
            "long" : -1.309197
          },
          "residents" : [ {
            "name" : "Fiver",
            "age" : 4,
            "role" : null
          }, {
            "name" : "Bigwig",
            "age" : 6,
            "role" : "Owsla"
          } ]
        }""").as[JsObject] + ("test", JsString("testString")))
  }

  client.execute {
    index into "london/pubs" doc Pub("blue bell")
  }.await

  refresh("london")
  blockUntilCount(4, "london")

  "a count request" should "return total count when no query is specified" in {
    val resp = client.execute {
      count from "london"
    }.await
    assert(4 === resp.getCount)
  }

  "a count request" should "return the document count for the correct type" in {
    val resp = client.execute {
      count from "london" -> "landmarks"
    }.await
    assert(2 === resp.getCount)
  }

  "an all search request" should "return all the document searched" in {
    val resp = client.execute {
      search in "london"
    }.await.getHits.getTotalHits shouldBe 4
  }

  "a search request" should "return the document searched for the correct type" in {
    val resp = client.execute {
      search in "london" types  "landmarks" rawQuery
        """
          |{
          |    "range" : {
          |        "timestamp" : {
          |            "gte": "2012-01-01",
          |            "lte": "now",
          |            "time_zone": "+1:00"
          |        }
          |    }
          |}
      """.stripMargin
    }.await
    //logger.info(s">> RESP: $resp")
    assert(2 === resp.getHits.getTotalHits)
  }

  "a match query" should
    "query uuid field" in {
      val resp = client.execute {
        search in "london" types "landmarks" query ("uuid:" + hampton.uuid)
      }.await
      //logger.info(s">> RESP: $resp")
      assert(2 === resp.getHits.totalHits)
  }

  "a current document search query" should
    "return the current document" in {
    val uuidQuery = hampton.uuid
    val resp = client.execute {
      search in "london" types "landmarks" rawQuery
        s"""
          |{
          |   "match" : {
          |        "uuid" : "$uuidQuery"
          |    },
          |    "range" : {
          |        "timestamp" : {
          |            "lte": "now",
          |            "time_zone": "+1:00"
          |        }
          |    }
          |}
        """.stripMargin sort2
        SortBuilders.fieldSort("timestamp").order(SortOrder.DESC) limit 1
    }.await
    logger.info(s">> RESP: $resp")
    assert(1 === resp.getHits.hits().length)
  }

}
