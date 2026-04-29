import sns.{Query, Simulator, User}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, QueryConfig, Session, Values}
import ujson.*
import upickle.core.LinkedHashMap


def handler(query: sns.Query): Option[Int] =
  //if query == "count:users" then Some(0) else None
  println(query)
  /*query match {
    case a => ""
    case _ => "hihi"
  }

   */
  None

def parseEvent(json: String): (String, LinkedHashMap[String, Value]) =
  val sanitized = json.filter(c => !c.isControl)
  val parsed = ujson.read(sanitized)
  val event = parsed("event").str

  val args = parsed("arguments").obj
  (event, args)

def handleEvent(session: Session, json: String): Unit =
  val (event, args) = parseEvent(json)
  event match
    case "new-user" =>
      session.run(s"CREATE (u:user {id: ${args("id")}, first: ${args("first")}, last: ${args("last")}})")
    case "new-post" => {
      session.run(
        s"""
           MATCH (n:user{id:${args("user")}})
           MERGE (n) -[:Posted] ->(p:post{id:${args("id")},text:${args("text")},date:${args("date")}})
        """)
    }
    case "new-comment" => session.run(
      s"""
        MATCH(p:post {id:${args("post")}})
        MATCH(u:user {id:${args("user")}})
        MERGE (p) -[:Has] -> (c:comment {id:${args("id")},text:${args("text")},date:${args("date")}}) <- [:Commented] -(u)
         """)
    case "like" => session.run(
      s"""
        MATCH(p:post {id:${args("post")}})
        MATCH(u:user {id:${args("user")}})
         MERGE (u) -[:Like] -> (p)
         """)
    case "delete-user" => session.run(
      s"""
        MATCH (u:user {id: ${args("id")}})
        OPTIONAL MATCH (u)-[:Posted]->(p:post)
        OPTIONAL MATCH (p)-[:Has]->(c:comment)
        OPTIONAL MATCH (u)-[:Commented]->(cu:comment)
        DETACH DELETE c,cu, p, u
         """)
    case "delete-post" => session.run(
      s"""
        MATCH(p:post {id:${args("id")}})
        OPTIONAL MATCH (p) -[:Has]->(c:comment)
        DETACH DELETE c,p
         """)
    case "update-post" => session.run(
      s"""
        MATCH(p:post {id:${args("id")}})
        SET p.text = ${args("text")}
         """)
    case _ => println(s"Unknown event: $event")
/*
@main def Main =
  val driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", "beydb-beepr"))
  System.out.println("Connection established.")
  val session = driver.session()
  val s = Simulator(seed = 2006)
  session.run("MATCH (n) DETACH DELETE n")
  for i <- 0 until 1000 do
    try {
      var event = s.randomEvent()
      println(event)
      handleEvent(session, event)
    } catch {
      case e: Exception => println("Error: " + e.getMessage)
    }


  session.close()
  driver.close()
*/

@main def Main =
  val s = Simulator(seed = 1337)
  for i <- 0 until 1000 do
    if (i & 0b11) != 0b11 then
      val e = s.randomEvent()
      //println(e)
    else
      s.challenge(handler)
  println(s.score())

  val driver = GraphDatabase.driver(
    "neo4j://localhost:7687", AuthTokens.basic("neo4j", "beydb-beepr"))
  driver.verifyConnectivity()
  driver.close()