// =============================================================================
// Lab 2 – Beepr — Test Queries (hardcoded values for Neo4j browser / IDE)
// =============================================================================
// Run these in the Neo4j browser at http://localhost:7474
// or in your IDE to verify the schema works correctly.
//
// The parameterised versions (with $id, $text, etc.) in schema.cypher are
// intended to be called from Scala code in Task 3.
// =============================================================================


// -----------------------------------------------------------------------------
// STEP 0 — Wipe the database (useful to reset between test runs)
// -----------------------------------------------------------------------------
MATCH (n) DETACH DELETE n;


// -----------------------------------------------------------------------------
// STEP 1 — Constraints (run once on a fresh database)
// -----------------------------------------------------------------------------
CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE;
CREATE CONSTRAINT post_id IF NOT EXISTS FOR (p:Post) REQUIRE p.id IS UNIQUE;


// -----------------------------------------------------------------------------
// STEP 2 — new-user
// -----------------------------------------------------------------------------
CREATE (:User {id: 0, first: 'Alice', last: 'Smith'});
CREATE (:User {id: 1, first: 'Bob',   last: 'Jones'});
CREATE (:User {id: 2, first: 'Carol', last: 'White'});


// -----------------------------------------------------------------------------
// STEP 3 — new-post
// -----------------------------------------------------------------------------
MATCH (u:User {id: 0})
CREATE (u)-[:POSTED]->(p:Post {id: 0, text: 'Hello world!', date: '2024-03-01', likes: 0});

MATCH (u:User {id: 1})
CREATE (u)-[:POSTED]->(p:Post {id: 1, text: 'My first post', date: '2024-03-02', likes: 0});


// -----------------------------------------------------------------------------
// STEP 4 — new-comment (Bob comments on Alice's post)
// -----------------------------------------------------------------------------
MATCH (u:User {id: 1}), (parent:Post {id: 0})
CREATE (u)-[:POSTED]->(c:Post {id: 2, text: 'Nice post!', date: '2024-03-03', likes: 0})
CREATE (parent)-[:HAS_COMMENT]->(c);


// -----------------------------------------------------------------------------
// STEP 5 — like (Carol likes Alice's post)
// -----------------------------------------------------------------------------
MATCH (p:Post {id: 0})
SET p.likes = p.likes + 1;


// -----------------------------------------------------------------------------
// STEP 6 — update-post (Alice edits her post)
// -----------------------------------------------------------------------------
MATCH (p:Post {id: 0})
SET p.text = 'Hello world! (edited)';


// -----------------------------------------------------------------------------
// STEP 7 — Verify the graph looks correct
// -----------------------------------------------------------------------------
MATCH (u:User)-[:POSTED]->(p:Post)
RETURN u.first, u.last, p.id, p.text, p.likes, p.date
ORDER BY p.id;

MATCH (parent:Post)-[:HAS_COMMENT]->(comment:Post)
RETURN parent.id AS post_id, comment.id AS comment_id, comment.text;


// -----------------------------------------------------------------------------
// STEP 8 — delete-post (delete post 1 and its comments recursively)
// -----------------------------------------------------------------------------
MATCH (p:Post {id: 1})
OPTIONAL MATCH (p)-[:HAS_COMMENT*1..]->(c:Post)
WITH p, collect(c) AS comments
FOREACH (c IN comments | DETACH DELETE c)
DETACH DELETE p;


// -----------------------------------------------------------------------------
// STEP 9 — delete-user (delete Alice and all her posts)
// -----------------------------------------------------------------------------
MATCH (u:User {id: 0})
OPTIONAL MATCH (u)-[:POSTED]->(p:Post)
DETACH DELETE p
WITH u
DELETE u;


// -----------------------------------------------------------------------------
// STEP 10 — Final state check (should only see Bob and Carol)
// -----------------------------------------------------------------------------
MATCH (n) RETURN n;
