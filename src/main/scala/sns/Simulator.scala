package sns

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import collection.mutable
import scala.util.Random

class Simulator(seed: Long):

  /** The random generator of this simulator. */
  val random = Random(seed)

  /** The users in the system. */
  val users = mutable.ArrayBuffer[Option[User]]()

  /** The posts in the system. */
  val posts = mutable.ArrayBuffer[Option[Post]]()

  /** A list with the positions in `users` that have been freed. */
  private var nextFreedUser: List[User.Identity] = Nil

  /** A list with the positions in `posts` that have been freed. */
  private var nextFreedPost: List[Post.Identity] = Nil

  /** The number of challenges succeeded and failed. */
  private var challenges: Array[Int] = Array(0, 0)

  /** Adds a user with the given name and returns its identity. */
  def addUser(first: String, last: String): User.Identity =
    val position: User.Identity = nextFreedUser match
      case head :: tail =>
        nextFreedUser = tail ; head
      case Nil =>
        users.append(None)
        users.length - 1
    users(position) = Some(User(first, last))
    position

  /** Adds a post with the given properties. */
  def addPost(user: User.Identity, text: String, date: LocalDate): Post.Identity =
    val position: Post.Identity = nextFreedPost match
      case head :: tail =>
        nextFreedPost = tail ; head
      case Nil =>
        posts.append(None)
        posts.length - 1
    posts(position) = Some(Post(user, text, date))
    users(user).get.posts.add(position)
    position

  /** Adds a comment to `post` with the given properties. */
  def addComment(
      post: Post.Identity, user: User.Identity, text: String, date: LocalDate
  ): Post.Identity =
    val i = addPost(user, text, date)
    posts(post).get.comments.append(i)
    i

  /** Increments the number of likes on `post`. */
  def addLike(post: Post.Identity): Unit =
    posts(post).get.likes += 1

  /** Removes `user` and its posts. */
  def removeUser(user: User.Identity): Unit =
    removePosts(users(user).get.posts.toList)
    users(user) = None
    nextFreedUser = user :: nextFreedUser

  /** Removes `post` and its comments. */
  def removePost(post: Post.Identity): Unit =
    removePosts(List(post))

  /** Removes all posts in `work`, along with their comments. */
  def removePosts(work: List[Post.Identity]): Unit =
    work match
      case w :: ws => posts(w) match
        case Some(p) =>
          users(p.user).get.posts.remove(w)
          posts(w) = None
          nextFreedPost = w :: nextFreedPost
          removePosts(ws.appendedAll(p.comments))
        case None =>
          removePosts(ws)
      case Nil => ()

  /** Modifies the text of `post`. */
  def modifyPost(post: Post.Identity, text: String): Unit =
    posts(post).get.text = text

  /** Generates a query and tests its answer. */
  def challenge(handler: Query => Option[Int]): Boolean =
    val q = Query.make(this)
    val i = handler(q) match
      case Some(n) => if n == q.evaluate(this) then 0 else 1
      case None => 1
    challenges(i) += 1
    i == 0

  /** Returns the score of the challenges that have been executed. */
  def score(): Double =
    val total = challenges(0) + challenges(1)
    if (total == 0) then 1.0 else challenges(0).toDouble / total.toDouble

  /** Returns the next event occurring on the platform. */
  def randomEvent(): String =
    def loop(): String =
      randomElement(Event.factories).flatMap(_.apply(this)) match
        case Some(s) => s
        case None => loop()
    loop()

  /** Returns a random first name, picked with `r`. */
  def randomFirstName(): String =
    randomElement(Names.first).get

  /** Returns a random last name, picked with `r`. */
  def randomLastName(): String =
    randomElement(Names.last).get

  /** Returns a random user or `None` if there isn't any. */
  def randomUser(): Option[User.Identity] =
    randomLiveIndex(users)

  /** Returns a random post or `None` if there isn't any. */
  def randomPost(): Option[Post.Identity] =
    randomLiveIndex(posts)

  /** Returns a random date after `from`. */
  def randomDate(from: LocalDate = LocalDate.of(2020, 1, 1)): LocalDate =
    from.plus(random.nextInt(365), ChronoUnit.DAYS)

  /** Returns a random ASCII character string. */
  def randomText(): String =
    val s = StringBuilder()
    for i <- 0 until (10 + random.nextInt(30)) do
      if (i != 0) && (i % (random.nextInt(5) + 2) == 0) then s.append(' ')
      s.append((random.nextInt(26) + 'a').toChar)
    s.toString()

  /** Returns a random element from `xs` iff `xs` is not empty. */
  private def randomElement[E](xs: collection.IndexedSeq[E]): Option[E] =
    if xs.isEmpty then None else Some(xs(random.nextInt(xs.length)))

  /** Returns a random position in `xs` at which the element is not `None`, or `None` if no such
   *  position exists. */
  private def randomLiveIndex[E](xs: mutable.ArrayBuffer[Option[E]]): Option[Int] =
    if xs.isEmpty then None else
      def loop(i: Int, j: Int): Option[Int] =
        if xs(i).isDefined then Some(i) else
          val k = (i + 1) % xs.length
          if k != k then loop(k, j) else None
      loop((random.nextInt(xs.length) + 1) % xs.length, random.nextInt(xs.length))

end Simulator
