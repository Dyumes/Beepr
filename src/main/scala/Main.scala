import sns.{Clause, Condition, Posts, Query, Simulator, Users}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, QueryConfig, Session, Values}
import sns.Condition.{Exactly, GreaterThan, LessThan}
import ujson.*
import upickle.core.LinkedHashMap

class Namecreator {
  private var count = 0

  def getname(): String = {
    count += 1
    toBase26(count)
  }

  private def toBase26(n: Int): String = {
    var num = n
    var result = ""

    while (num > 0) {
      val rem = (num - 1) % 26
      result = ('a' + rem).toChar + result
      num = (num - 1) / 26
    }

    result
  }
}

def handler(query: sns.Query): Option[Int] = {
  val driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", "beydb-beepr"))
  val session = driver.session()
  val namecreator = new Namecreator
  val var_name = namecreator.getname()
  try {
    query match
      case Users(c) => {
        user_clause_handler(c, var_name, namecreator) match {
          case Some(q) => Some(session.run(s"$q" + s"RETURN COUNT(DISTINCT $var_name) AS cnt").single().get("cnt").asInt())
          case None => None
        }
      }
      case Posts(c) => {
        post_clause_handler(c, var_name, namecreator) match {
          case Some(q) => Some(session.run(s"$q" + s"RETURN COUNT(DISTINCT $var_name) AS cnt").single().get("cnt").asInt())
          case None => None
        }
      }
  } catch {
    case e: Exception => {
      println("Error: " + e.getMessage)
      None
    }
  } finally {
    session.close()
    driver.close()
  }
}

def user_clause_handler(clause: Clause, var_name: String, namecreator: Namecreator): Option[String] = {
  val var_name2 = namecreator.getname()
  clause match {
    case Clause.True => Some(s"MATCH ($var_name:user)")
    case Clause.HasFirstName(name) => Some(s"MATCH ($var_name:user {first:\"$name\"})")
    case Clause.HasLastName(name) => Some(s"MATCH ($var_name:user {last:\"$name\"})")
    case Clause.HasPost(p) => post_clause_handler(p, var_name2, namecreator) match {
      case Some(q) => Some(s"""$q
                               MATCH ($var_name:user) -[:Posted]->(p)""")
      case None => None
    }
    case _ => None
  }
}

def post_clause_handler(clause: Clause, var_name: String, namecreator: Namecreator): Option[String] = {
  val var_name2 = namecreator.getname()
  clause match {
    case Clause.True => Some(s"MATCH ($var_name:post)")
    case Clause.HasAuthor(subclause) => user_clause_handler(subclause, var_name2, namecreator) match {
      case Some(q) => Some(s"$q" +
        s"MATCH ($var_name) -[:Posted]->(p:post)")
      case None => None
    }

    case Clause.HasComment(subclause) => post_clause_handler(subclause, var_name2, namecreator) match {
      case Some(q) => Some(s"$q" +
        s"MATCH ($var_name:post) -[:Has]->($var_name2:post)")
      case None => None
    }
    case Clause.LikeCount(likes) => condition_handler(likes) match {
      case Some(q) => Some(s"OPTIONAL MATCH ($var_name:post) <-[l:Like]-() " +
                            s"WITH $var_name, COUNT(l) AS likeCount " +
                            s"WHERE $q")
      case None => None
    }
    case _ => None
  }
}

def condition_handler(condition: Condition): Option[String] = {
  condition match {
    case Exactly(n) => Some(s"likeCount = $n ")
    case LessThan(n) => Some(s"likeCount < $n ")
    case GreaterThan(n) => Some(s"likeCount > $n ")
  }
}

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
        MERGE (p) -[:Has] -> (c:post {id:${args("id")},text:${args("text")},date:${args("date")}}) <- [:Posted] -(u)
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
        OPTIONAL MATCH (p)-[:Has]->(c:post)
        OPTIONAL MATCH (u)-[:Posted]->(cu:post)
        DETACH DELETE c,cu, p, u
         """)
    case "delete-post" => session.run(
      s"""
        MATCH(p:post {id:${args("id")}})
        OPTIONAL MATCH (p) -[:Has]->(c:post)
        DETACH DELETE c,p
         """)
    case "update-post" => session.run(
      s"""
        MATCH(p:post {id:${args("id")}})
        SET p.text = ${args("text")}
         """)
    case _ => println(s"Unknown event: $event")

//@main def Main =
//  val driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", "beydb-beepr"))
//  System.out.println("Connection established.")
//  val session = driver.session()
//  val s = Simulator(seed = 2006)
//  session.run("MATCH (n) DETACH DELETE n")
//  for i <- 0 until 1000 do
//    try {
//      var event = s.randomEvent()
//      println(event)
//      handleEvent(session, event)
//    } catch {
//      case e: Exception => println("Error: " + e.getMessage)
//    }
//  session.close()
//  driver.close()


@main def Main =
  val driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", "beydb-beepr"))
  System.out.println("Connection established.")
  val session = driver.session()
  val s = Simulator(seed = 1337)
  session.run("MATCH (n) DETACH DELETE n")
  for i <- 0 until 1000 do
    if (i & 0b11) != 0b11 then
      val e = s.randomEvent()
      handleEvent(session, e)

    else
      val c = s.challenge(handler)
  println(s.score())


  session.close()
  driver.verifyConnectivity()
  driver.close()