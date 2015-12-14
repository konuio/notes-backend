// mongo development scripts/setup_mongo.js

// Session expiration index
db['session-tokens'].createIndex({lastActive: 1}, {expireAfterSeconds: 21600})

// Index for testing
// db['session-tokens'].dropIndex({lastActive: 1}); db['session-tokens'].createIndex({lastActive: 1}, {expireAfterSeconds: 10})