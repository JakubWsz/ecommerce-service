db = db.getSiblingDB("admin");

db.createUser({
    user: "eventstore",
    pwd: "password",
    roles: [
        { role: "readWrite", db: "eventstore" }
    ]
});

db = db.getSiblingDB("eventstore");
db.createCollection("events");

db.events.createIndex({ "aggregateId": 1 });
db.events.createIndex({ "eventType": 1 });
db.events.createIndex({ "timestamp": 1 });

db = db.getSiblingDB("admin");
db.createUser({
    user: "customer_user",
    pwd: "customer_password",
    roles: [
        { role: "readWrite", db: "customer" }
    ]
});

db = db.getSiblingDB("customer");
db.createCollection("customers");


db.customers.createIndex({ "id": 1 }, { unique: true });
db.customers.createIndex({ "email": 1 }, { unique: true });
