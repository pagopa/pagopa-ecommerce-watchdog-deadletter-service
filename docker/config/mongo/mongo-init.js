db = db.getSiblingDB(process.env.MONGO_INITDB_NAME);

db.createCollection('notes');
db.createCollection('actions');
db.createCollection('operators');

db.operators.insertMany([
    {_id: 'test', password: '$2a$16$O0IiSKSJ9/00mOBxyEb6bOezH1ptyFkY8ZqOL/lxwGAbxMAcGzQsC', name: 'test', surname: 'test', email: 'test@test.com'},
    {_id: 'test2', password: '$2a$16$dMVYkSmpXvPCDAKXABEEmekb5ryxtwyqCDTIOKH3XW9wFVRNOia1e', name: 'test2', surname: 'test2', email: 'test2@test.com'}
]);
