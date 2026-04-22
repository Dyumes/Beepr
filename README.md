# Social Network Simulator

## Task 1 – Design

The simulator generates 7 event types: `new-user`, `new-post`, `new-comment`, `like`, `delete-user`, `delete-post`, `update-post`.

**Entities and their data:**
- **User** : `id`, `first`, `last`
- **Post** : `id`, `text`, `date`, `likes` (also used for comments)

**Relationships:**
- A user authors posts and comments
- A post can have comments (which are themselves posts)
- A post accumulates likes (counter only, no user tracking)
- Deleting a user removes all their posts/comments; deleting a post removes it and all its comments recursively

---

## Task 2 – Implementation

### Schema

Two node labels, two relationship types:

```
(:User {id, first, last})
(:Post {id, text, date, likes})

(:User)-[:POSTED]->(:Post)      // user authored a post or comment
(:Post)-[:HAS_COMMENT]->(:Post) // post has a comment
```
