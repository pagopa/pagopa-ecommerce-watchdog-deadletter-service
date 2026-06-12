let pathDep = require('path');
const getFile = (path) => fs.readFileSync(pathDep.join(__dirname, path), { encoding: "utf8" });

db = db.getSiblingDB(process.env.MONGO_INITDB_NAME);

db.createCollection('notes');
db.createCollection('actions');
db.createCollection('operators');

db.actions.insertMany(EJSON.parse(getFile('d-actions.json')));
db.notes.insertMany(EJSON.parse(getFile('d-notes.json')));
db.operators.insertMany(EJSON.parse(getFile('d-operators.json')));
