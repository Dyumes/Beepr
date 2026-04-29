package sns

/** A function that modifies `simulator` and returns a JSON description of this update iff it
 *  could be applied. Otherwise, returns `None` without modifying `simulator`.
 */
type EventFactory = (simulator: Simulator) => Option[String]

object Event:

  /** A collection of event constructors. */
  val factories: IArray[EventFactory] =
    def repeat(factory: EventFactory, times: Int): Iterator[EventFactory] =
      new Iterator[EventFactory] {
        var n = 0
        def hasNext: Boolean = n < times
        def next(): EventFactory = { n += 1 ; factory }
      }

    val builder = IArray.newBuilder[EventFactory]
    builder.addAll(repeat(newUser, 2))
    builder.addAll(repeat(newPost, 10))
    builder.addAll(repeat(newComment, 10))
    builder.addAll(repeat(newLike, 5))
    builder.addOne(removeUser)
    builder.addOne(removePost)
    builder.addOne(modifyPost)
    builder.result()

  /** The creation of a new user. */
  def newUser(simulator: Simulator): Option[String] =
    val a = simulator.randomFirstName()
    val b = simulator.randomLastName()
    val i = simulator.addUser(a, b)
    Some(describe("new-user", IArray("id" -> i, "first" -> a, "last" -> b)))

  /** The ceation of a new post. */
  def newPost(simulator: Simulator): Option[String] =
    simulator.randomUser().map { (u) =>
      val a = simulator.randomText()
      val b = simulator.randomDate()
      val i = simulator.addPost(u, a, b)
      describe("new-post", IArray("id" -> i, "user" -> u, "text" -> a, "date" -> b))
    }

  /** The creation of a comment. */
  def newComment(simulator: Simulator): Option[String] =
    simulator.randomPost().flatMap { (p) =>
      simulator.randomUser().map { (u) =>
        val a = simulator.randomText()
        val b = simulator.randomDate(from = simulator.posts(p).get.date)
        val i = simulator.addComment(p, u, a, b)
        describe(
          "new-comment", IArray("id" -> i, "post" -> p, "user" -> u, "text" -> a, "date" -> b))
      }
    }

  /** The liking of a post. */
  def newLike(simulator: Simulator): Option[String] =
    simulator.randomPost().flatMap { (p) =>
      simulator.randomUser().map { (u) =>
        simulator.addLike(p)
        describe("like", IArray("post" -> p, "user" -> u))
      }
    }

  /** The deletion of a user. */
  def removeUser(simulator: Simulator): Option[String] =
    simulator.randomUser().map { (u) =>
      simulator.removeUser(u)
      describe("delete-user", IArray("id" -> u))
    }

  /** The deletion of a post. */
  def removePost(simulator: Simulator): Option[String] =
    simulator.randomPost().map { (p) =>
      simulator.removePost(p)
      describe("delete-post", IArray("id" -> p))
    }

  /** The modification of a post. */
  def modifyPost(simulator: Simulator): Option[String] =
    simulator.randomPost().map { (p) =>
      val a = simulator.randomText()
      simulator.modifyPost(p, a)
      describe("update-post", IArray("id" -> p, "text" -> a))
    }

  /** Returns a description of `operation` applied with `arguments`. */
  private def describe(operation: String, arguments: Seq[(String, Any)]): String =
    val s = StringBuilder()
    s.append(s"{\"event\": \"${operation}\", \"arguments\": {")
    var first = true
    for (k, v) <- arguments do
      if first then first = false else s.append(", ")
      s.append(s"\"${k}\": \"${v}\"")
    s.append(s"}}")
    s.toString()
