package sns

import java.time.LocalDate
import java.time.temporal.ChronoUnit

import collection.mutable
import scala.util.Random

import sns.{Event, Post, User, Names}
import sns.Simulator.Query

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
    val ps = users(user).get.posts.clone()
    for p <- ps do removePost(p, recursively = false)
    users(user) = None
    nextFreedUser = user :: nextFreedUser

  /** Removes `post` and its comments. */
  def removePost(post: Post.Identity, recursively: Boolean): Unit =
    users(posts(post).get.user).get.posts.remove(post)
    if recursively then
      for p <- posts(post).get.comments do removePost(p, recursively)
    posts(post) = None
    nextFreedPost = post :: nextFreedPost

  /** Modifies the text of `post`. */
  def modifyPost(post: Post.Identity, text: String): Unit =
    posts(post).get.text = text

  /** Generates a query and tests its answer. */
  def challenge(handler: Query => Option[Int]): Boolean =
    false

  /** Returns the score of the challenges that have been executed. */
  def score(): Double =
    0.0

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

object Simulator:

  /** A query against the contents of the simulator's data. */
  type Query = String

end Simulator
