// --- Eventstore DB ---
db = db.getSiblingDB("admin");

db.createUser({
    user: "eventstore",
    pwd: "password",
    roles: [{ role: "readWrite", db: "eventstore_dev" }]
});

db = db.getSiblingDB("eventstore_dev");
db.createCollection("events");

// Utworzenie indeksów dla kolekcji events
db.events.createIndex({ "aggregateId": 1 });
db.events.createIndex({ "eventType": 1 });
db.events.createIndex({ "timestamp": 1 });

// --- Customer DB ---
db = db.getSiblingDB("admin");

db.createUser({
    user: "customer_user",
    pwd: "customer_password",
    roles: [{ role: "readWrite", db: "customer" }]
});

db = db.getSiblingDB("customer");
db.createCollection("customers");

// Indeksy unikalne
db.customers.createIndex({ "id": 1 }, { unique: true });
db.customers.createIndex({ "email": 1 }, { unique: true });

// --- Vendor DB ---
db = db.getSiblingDB("admin");

db.createUser({
    user: "vendor_user",
    pwd: "vendor_password",
    roles: [{ role: "readWrite", db: "vendor_dev" }]
});

db = db.getSiblingDB("vendor_dev");
db.createCollection("vendor_categories");

// Indeksy dla vendor_categories
db.vendor_categories.createIndex({ "vendorId": 1 });
db.vendor_categories.createIndex({ "category.id": 1 });
db.vendor_categories.createIndex({ "status": 1 });
