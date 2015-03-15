package timeshift

import java.util.Date

import com.sksamuel.elastic4s.source.DocumentMap
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import com.sksamuel.elastic4s.ElasticDsl._

/**
 * @author Riccardo Merolla
 *         Created on 15/03/15.
 */
class CountTest extends FlatSpec with MockitoSugar with ElasticSugar {

  val now = new Date

  val oldDate = "2014-01-01"

  case class Landmarks(name: String, timestamp: Date) extends DocumentMap {
    override def map: Map[String, Any] = Map("name" -> name, "timestamp" -> timestamp)
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

  client.execute {
    index into "london/pubs" fields "name" -> "blue bell"
  }.await

  refresh("london")
  blockUntilCount(3, "london")

  "a count request" should "return total count when no query is specified" in {
    val resp = client.execute {
      count from "london"
    }.await
    assert(3 === resp.getCount)
  }

  "a count request" should "return the document count for the correct type" in {
    val resp = client.execute {
      count from "london" -> "landmarks"
    }.await
    assert(2 === resp.getCount)
  }

  "a search request" should "return the document searched for the correct type" in {
    val to = new Date().getTime
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
