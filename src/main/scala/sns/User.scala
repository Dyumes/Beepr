package sns

import collection.mutable

import sns.Post

/** A user. */
class User(var first: String, var last: String):

  /** The posts that have been written by the user. */
  val posts = mutable.HashSet[Post.Identity]()

  /** The likes that have been made by the user. */
  val likes = mutable.HashSet[Post.Identity]()

object User:

  /** The identity of a user. */
  type Identity = Int

end User
