const { test, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc, deleteDoc, getDocs, collection, query, where } = require('firebase/firestore');
const { ref, uploadBytes, deleteObject } = require('firebase/storage');
let env;
const restaurant = { id: 'r1', ownerId: 'owner', name: 'Restaurant', description: '', city: 'Istanbul',
 address: '', tableCount: 10, imageUrl: '', imageResId: 0, reviews: [], hygieneScore: 0, tasteScore: 0, priceScore: 0, rwScore: 0 };
const review = { id: 'review1', restaurantId: 'r1', userId: 'alice', userName: 'Alice',
 hygieneRating: 4, tasteRating: 5, priceRating: 3, comment: 'Good', date: '14.10.2026', isPositive: true, ownerReply: '' };
const request = { id: 'q1', userId: 'alice', restaurantId: 'r1', tableName: '1', requestType: 'service', note: '', status: 'pending', timestamp: 1 };
const db = uid => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-restaview-security',
 firestore: { rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8') },
 storage: { rules: fs.readFileSync(path.join(__dirname, '..', 'storage.rules'), 'utf8') } }); });
beforeEach(async () => {
 await env.clearFirestore(); await env.clearStorage();
 await env.withSecurityRulesDisabled(async c => {
  await setDoc(doc(c.firestore(), 'restaurants/r1'), restaurant);
  await setDoc(doc(c.firestore(), 'restaurants/r1/reviews/review1'), review);
  await setDoc(doc(c.firestore(), 'requests/q1'), request);
  await setDoc(doc(c.firestore(), 'users/alice'), { uid: 'alice', name: 'Alice', email: 'alice@example.com' });
 });
});
after(async () => { await env.cleanup(); });
test('private profiles deny strangers and anonymous users; self read/update works', async () => {
 await assertFails(getDoc(doc(db(), 'users/alice')));
 await assertFails(getDoc(doc(db('bob'), 'users/alice')));
 await assertSucceeds(getDoc(doc(db('alice'), 'users/alice')));
 await assertSucceeds(updateDoc(doc(db('alice'), 'users/alice'), { name: 'Updated' }));
 await assertFails(updateDoc(doc(db('alice'), 'users/alice'), { isAdmin: true }));
});
test('restaurant owner may edit content, but cannot transfer ownership or forge reviews/ratings', async () => {
 await assertSucceeds(getDoc(doc(db(), 'restaurants/r1')));
 await assertFails(updateDoc(doc(db(), 'restaurants/r1'), { name: 'Pwned' }));
 await assertFails(updateDoc(doc(db('bob'), 'restaurants/r1'), { ownerId: 'bob' }));
 await assertSucceeds(updateDoc(doc(db('owner'), 'restaurants/r1'), { name: 'Updated' }));
 for (const change of [{ ownerId: 'bob' }, { reviews: [review] }, { rwScore: 5 }]) {
  await assertFails(updateDoc(doc(db('owner'), 'restaurants/r1'), change));
 }
 await assertSucceeds(setDoc(doc(db('bob'), 'restaurants/r2'), { ...restaurant, id: 'r2', ownerId: 'bob' }));
 await assertFails(setDoc(doc(db('bob'), 'restaurants/r3'), { ...restaurant, id: 'r3' }));
});
test('reviews enforce author identity, ratings, ownership, and reply-only owner updates', async () => {
 const target = uid => doc(db(uid), 'restaurants/r1/reviews/review2');
 await assertFails(setDoc(target('bob'), { ...review, id: 'review2' }));
 await assertFails(setDoc(target('alice'), { ...review, id: 'review2', tasteRating: 99 }));
 await assertSucceeds(setDoc(target('alice'), { ...review, id: 'review2' }));
 await assertFails(updateDoc(target('bob'), { ownerReply: 'Forged' }));
 await assertFails(updateDoc(target('owner'), { comment: 'Changed review' }));
 await assertSucceeds(updateDoc(target('owner'), { ownerReply: 'Thank you' }));
 await assertFails(deleteDoc(target('bob')));
 await assertSucceeds(deleteDoc(target('alice')));
});
test('requests require the authenticated customer and only restaurant owner can complete', async () => {
 await assertFails(getDoc(doc(db(), 'requests/q1')));
 await assertFails(getDoc(doc(db('bob'), 'requests/q1')));
 await assertSucceeds(getDoc(doc(db('alice'), 'requests/q1')));
 await assertSucceeds(getDocs(query(collection(db('owner'), 'requests'), where('restaurantId', '==', 'r1'), where('status', '==', 'pending'))));
 await assertFails(setDoc(doc(db('bob'), 'requests/q2'), { ...request, id: 'q2' }));
 await assertSucceeds(setDoc(doc(db('bob'), 'requests/q2'), { ...request, id: 'q2', userId: 'bob' }));
 await assertFails(updateDoc(doc(db('alice'), 'requests/q1'), { status: 'completed' }));
 await assertSucceeds(updateDoc(doc(db('owner'), 'requests/q1'), { status: 'completed' }));
 await assertFails(updateDoc(doc(db('owner'), 'requests/q1'), { restaurantId: 'other' }));
});
test('cover uploads are isolated by user, validate type/size, and deny unknown paths', async () => {
 const storage = uid => env.authenticatedContext(uid).storage();
 const upload = (uid, name, bytes, contentType) => uploadBytes(ref(storage(uid), name), bytes, { contentType });
 await assertSucceeds(upload('owner', 'restaurant_images/owner/a.jpg', new Uint8Array([1, 2]), 'image/jpeg'));
 await assertFails(upload('bob', 'restaurant_images/owner/a.jpg', new Uint8Array([1]), 'image/jpeg'));
 await assertFails(deleteObject(ref(storage('bob'), 'restaurant_images/owner/a.jpg')));
 await assertFails(upload('owner', 'restaurant_images/owner/a.html', new Uint8Array([1]), 'text/html'));
 await assertFails(upload('owner', 'restaurant_images/owner/large.jpg', new Uint8Array(5 * 1024 * 1024 + 1), 'image/jpeg'));
 await assertFails(upload('owner', 'unrestricted/a.jpg', new Uint8Array([1]), 'image/jpeg'));
});
