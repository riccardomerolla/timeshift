package timeshift

import java.util.{UUID, Date}

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.source.{JacksonSource, DocumentSource, DocumentMap}
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import com.sksamuel.elastic4s.ElasticDsl._
import timeshift.entity.TimedDocument

/**
 * @author Riccardo Merolla
 *         Created on 15/03/15.
 */
class CountTest extends FlatSpec with MockitoSugar with ElasticSugar {

  val now = new Date

  val oldDate = "2014-01-01"

  case class Landmarks(name: String, timestamp: Date) extends DocumentMap with TimedDocument {
    override def map: Map[String, Any] =
      Map("uuid" -> uuid, "name" -> name, "timestamp" -> timestamp, "data-in" -> dataIn, "doc" -> source)

    override def uuid: UUID = UUID.randomUUID()

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

    override def json: String = Json.stringify(Json.toJson(this))
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
    }.await
    logger.info(s">> RESP: $resp")
    assert(4 === resp.getHits.getTotalHits)
  }

  "a search request" should "return the document searched for the correct type" in {
    val resp = client.execute {
      search in "london" types  "landmarks" rawQuery(
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
      """.stripMargin)
    }.await
    logger.info(s">> RESP: $resp")
    assert(2 === resp.getHits.getTotalHits)
  }

}
