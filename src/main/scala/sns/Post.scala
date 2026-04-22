package sns

import java.time.LocalDate

import collection.mutable

/** A post left on the profile of a user. */
class Post(val user: User.Identity, var text: String, val date: LocalDate):

  /** The number of likes on this post. */
  var likes = 0

  /** The comments that have been left on this post. */
  val comments = mutable.ArrayBuffer[Post.Identity]()

object Post:

  /** The identity of a post. */
  type Identity = Int

end Post
