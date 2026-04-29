package sns

import sns.Clause.LikeCount

/** A query against the contents of a simulator. */
sealed trait Query:

  /** Returns the result of this query evaluated on the contents of `simulator`. */
  def evaluate(simulator: Simulator): Int

object Query:

  /** Creates a random instance using the random generator of `simulator`. */
  def make(simulator: Simulator): Query =
    if simulator.random.nextBoolean() then
      Users.make(simulator)
    else
      Posts.make(simulator)

end Query

case class Users(clause: Clause) extends Query:

  def evaluate(simulator: Simulator): Int =
    val result = simulator.users.flatMap {
      case Some(u) if simulator.satisfies(u, clause) => Some(u)
      case _ => None
    }
    result.length

object Users:

  /** Creates a random instance using the random generator of `simulator`. */
  def make(simulator: Simulator): Users =
    new Users(makeClause(simulator))

  /** Creates a random user clause using the random generator of `simulator`. */
  def makeClause(simulator: Simulator): Clause =
    simulator.random.nextInt(4) match
      case 0 => Clause.True
      case 1 => Clause.HasFirstName(simulator.randomFirstName())
      case 2 => Clause.HasLastName(simulator.randomLastName())
      case 3 => Clause.HasPost(Posts.makeClause(simulator))
      case _ => throw IllegalArgumentException()

end Users

case class Posts(clause: Clause) extends Query:

  def evaluate(simulator: Simulator): Int =
    val result = simulator.posts.flatMap {
      case Some(p) if simulator.satisfies(p, clause) => Some(p)
      case _ => None
    }
    result.length

object Posts:

  /** Creates a random instance using the random generator of `simulator`. */
  def make(simulator: Simulator): Posts =
    new Posts(makeClause(simulator))

  /** Creates a random post clause using the random generator of `simulator`. */
  def makeClause(simulator: Simulator): Clause =
    simulator.random.nextInt(4) match
      case 0 => Clause.True
      case 1 => Clause.HasAuthor(Users.makeClause(simulator))
      case 2 => Clause.HasComment(Posts.makeClause(simulator))
      case 3 => Clause.LikeCount(Condition.make(simulator))
      case _ => throw IllegalArgumentException()

end Posts

/** A filter on a result set. */
sealed trait Clause

object Clause:

  /** A predicate that is always satisfied. */
  case object True extends Clause

  /** A filter selecting users whose first name is `fitst`.  */
  case class HasFirstName(first: String) extends  Clause

  /** A filter selecting users whose last name is `last`.  */
  case class HasLastName(last: String) extends  Clause

  /** A filter selecting users that have authored post satisfying `clause`. */
  case class HasPost(where: Clause) extends Clause

  /** A filter selecting posts that have been authored by a user satisfying `clause`. */
  case class HasAuthor(where: Clause) extends Clause

  /** A filter selecting posts that have comments satisfying `clause`. */
  case class HasComment(where: Clause) extends Clause

  /** A filter selecting posts that have `condition` likes. */
  case class LikeCount(condition: Condition) extends Clause

end Clause

/** A condition expressing a property of a number. */
enum Condition:

  /** The number is equal to `n`. */
  case Exactly(n: Int)

  /** The number is greater than `n`. */
  case GreaterThan(n: Int)

  /** The number is less than `n`. */
  case LessThan(n: Int)

  def apply(x: Int): Boolean = this match
    case Exactly(n) => x == n
    case GreaterThan(n) => x > n
    case LessThan(n) => x < n

object Condition:

  /** Creates a random instance using the random generator of `simulator`. */
  def make(simulator: Simulator): Condition =
    val n = simulator.random.nextInt(10)
    simulator.random.nextInt(3) match
      case 0 => Exactly(n)
      case 1 => GreaterThan(n)
      case 2 => LessThan(n)

end Condition

extension (self: Simulator)

  /** Returns `true` iff `u` satisfies `c`. */
  def satisfies(u: User, c: Clause): Boolean = c match
    case Clause.True =>
      true
    case Clause.HasFirstName(n) =>
      u.first == n
    case Clause.HasLastName(n) =>
      u.last == n
    case Clause.HasPost(w) =>
      u.posts.exists((p) => satisfies(self.posts(p).get, w))
    case _ =>
      throw IllegalArgumentException()

  /** Returns `true` iff `p` satisfies `c`. */
  def satisfies(p: Post, c: Clause): Boolean = c match
    case Clause.True =>
      true
    case Clause.HasAuthor(w) =>
      satisfies(self.users(p.user).get, w)
    case Clause.HasComment(w) =>
      p.comments.exists((c) => self.posts(c).map((x) => satisfies(x, w)).getOrElse(false))
    case LikeCount(n) =>
      n(p.likes)
    case _ =>
      throw IllegalArgumentException()

end extension
