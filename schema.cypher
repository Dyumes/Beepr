// =============================================================================
// Lab 2 – Beepr — Neo4j Schema & Event Queries
// =============================================================================
// This file contains:
//   1. Constraints (run once on a fresh database)
//   2. One parameterised Cypher query per simulator event type
// =============================================================================


// -----------------------------------------------------------------------------
// 1. CONSTRAINTS
// Run these once to set up the database.
// -----------------------------------------------------------------------------

CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE;
CREATE CONSTRAINT post_id IF NOT EXISTS FOR (p:Post) REQUIRE p.id IS UNIQUE;


// -----------------------------------------------------------------------------
// 2. EVENT: new-user
// JSON: {"event": "new-user", "arguments": {"id": <Int>, "first": <String>, "last": <String>}}
//
// Creates a new User node.
// Parameters: $id (Int), $first (String), $last (String)
// -----------------------------------------------------------------------------

CREATE (:User {id: $id, first: $first, last: $last});


// -----------------------------------------------------------------------------
// 3. EVENT: new-post
// JSON: {"event": "new-post", "arguments": {"id": <Int>, "user": <Int>, "text": <String>, "date": <String>}}
//
// Creates a new Post node and links it to its author via [:POSTED].
// Parameters: $id (Int), $user (Int), $text (String), $date (String)
// -----------------------------------------------------------------------------

MATCH (u:User {id: $user})
CREATE (u)-[:POSTED]->(p:Post {id: $id, text: $text, date: $date, likes: 0});


// -----------------------------------------------------------------------------
// 4. EVENT: new-comment
// JSON: {"event": "new-comment", "arguments": {"id": <Int>, "post": <Int>, "user": <Int>, "text": <String>, "date": <String>}}
//
// Creates a new Post node (the comment), links it to its author via [:POSTED],
// and links the parent post to it via [:HAS_COMMENT].
// Parameters: $id (Int), $post (Int), $user (Int), $text (String), $date (String)
// -----------------------------------------------------------------------------

MATCH (u:User {id: $user}), (parent:Post {id: $post})
CREATE (u)-[:POSTED]->(c:Post {id: $id, text: $text, date: $date, likes: 0})
CREATE (parent)-[:HAS_COMMENT]->(c);


// -----------------------------------------------------------------------------
// 5. EVENT: like
// JSON: {"event": "like", "arguments": {"post": <Int>, "user": <Int>}}
//
// Increments the likes counter on the target Post.
// Note: the simulator only tracks a like count (not which user liked what),
// so we only update the counter here.
// Parameters: $post (Int)
// -----------------------------------------------------------------------------

MATCH (p:Post {id: $post})
SET p.likes = p.likes + 1;


// -----------------------------------------------------------------------------
// 6. EVENT: delete-user
// JSON: {"event": "delete-user", "arguments": {"id": <Int>}}
//
// Deletes the User and ALL posts/comments they authored (without recursively
// deleting comments on those posts, matching simulator behaviour where
// removeUser calls removePost with recursively = false).
// Parameters: $id (Int)
// -----------------------------------------------------------------------------

MATCH (u:User {id: $id})
OPTIONAL MATCH (u)-[:POSTED]->(p:Post)
DETACH DELETE p
WITH u
DELETE u;


// -----------------------------------------------------------------------------
// 7. EVENT: delete-post
// JSON: {"event": "delete-post", "arguments": {"id": <Int>}}
//
// Deletes a Post and ALL of its comments recursively (matching simulator
// behaviour where removePost is called with recursively = true).
// We collect the entire comment subtree first, then delete everything.
// Parameters: $id (Int)
// -----------------------------------------------------------------------------

MATCH (p:Post {id: $id})
OPTIONAL MATCH (p)-[:HAS_COMMENT*1..]->(c:Post)
WITH p, collect(c) AS comments
FOREACH (c IN comments | DETACH DELETE c)
DETACH DELETE p;


// -----------------------------------------------------------------------------
// 8. EVENT: update-post
// JSON: {"event": "update-post", "arguments": {"id": <Int>, "text": <String>}}
//
// Updates the text of an existing Post.
// Parameters: $id (Int), $text (String)
// -----------------------------------------------------------------------------

MATCH (p:Post {id: $id})
SET p.text = $text;
