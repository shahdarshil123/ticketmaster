Core functional requirements (must-have)

Users discover and filter events by location, date, category and price range.
Users view an event, venue sections, seats and currently available inventory.
Users can temporarily hold one or multiple seats for a bounded time.
A seat cannot be simultaneously held or sold to competing customers.
Users can proceed to a simulated checkout and confirm an order.
Expired or cancelled holds are reclaimable; confirmed sold seats cannot be reserved again.
The backend supports duplicate requests, retries, service errors and event-driven notifications.

Entities per service (v1: start simple)

Scope for v1: core entities only. Idempotency, outbox and Kafka messaging are deferred and will be added gradually.
Each service owns its own database. Services refer to each other's data by ID only (no cross-service joins).

Common to every entity (BaseEntity)
- Attributes: id, created_at, updated_at
- Functions: getId(), getCreatedAt(), getUpdatedAt()
  (timestamps are set automatically on insert/update)


Event Service: venues, seating layout and events

Venue
- Attributes: name, address, city, latitude, longitude
- Functions:
  - addSection(name): creates a Section in this venue
  - getSections()

Section
- Attributes: venue_id, name
- Functions:
  - addSeat(row_label, seat_number): creates a Seat in this section
  - getSeats()

Seat (physical seat, same for every event at the venue)
- Attributes: section_id, row_label, seat_number
- Functions:
  - getLabel(): e.g. "Section A, Row 3, Seat 12"

Category (enum, not a table)
- Values: CONCERT, SPORTS, THEATRE, COMEDY, FAMILY
- Stored on Event as text; adding a category is a code change

Event
- Attributes: venue_id, category, name, description, start_time, status (DRAFT / PUBLISHED / CANCELLED)
- Functions:
  - publish(): DRAFT -> PUBLISHED; makes the event bookable and searchable
  - cancel(): DRAFT or PUBLISHED -> CANCELLED
  - updateDetails(name, description, start_time): only while not CANCELLED
  - isBookable(): true when PUBLISHED and start_time is in the future

PriceTier (price of one section for one event)
- Attributes: event_id, section_id, price
- Functions:
  - changePrice(new_price): price must be > 0


Booking Service: seat inventory, holds, orders and payments

Customer
- Attributes: name, email
- Functions: none beyond getters

EventSeat (one row per seat per event; the inventory)
- Attributes: event_id, seat_id, section_id, section_name, row_label, seat_number, price, status (AVAILABLE / HELD / SOLD), hold_id, version
  - unique (event_id, seat_id)
  - section_name, row_label, seat_number are copied from the Event Service so the seat map needs only one call
  - version: optimistic locking, so two customers cannot hold or buy the same seat at once
- Functions:
  - hold(hold_id): AVAILABLE -> HELD; fails if not AVAILABLE
  - release(): HELD -> AVAILABLE; clears hold_id
  - sell(): HELD -> SOLD; SOLD is final
  - isAvailable()

Hold (temporary reservation of one or more seats)
- Attributes: customer_id, event_id, status (ACTIVE / EXPIRED / RELEASED / CONVERTED), expires_at
- Functions:
  - addSeat(event_seat_id): creates a HoldItem
  - isExpired(now): true when ACTIVE and now >= expires_at
  - expire(): ACTIVE -> EXPIRED; its seats are released
  - release(): ACTIVE -> RELEASED (customer cancels); its seats are released
  - convert(): ACTIVE -> CONVERTED (an order was confirmed from this hold)

HoldItem
- Attributes: hold_id, event_seat_id
- Functions: none beyond getters

Order
- Attributes: customer_id, event_id, hold_id, status (PENDING / CONFIRMED / FAILED / CANCELLED), total_amount
- Functions:
  - createFromHold(hold): one OrderItem per held seat; total_amount = sum of prices
  - confirm(): PENDING -> CONFIRMED; seats become SOLD, hold becomes CONVERTED
  - fail(): PENDING -> FAILED; seats stay HELD until the hold expires
  - cancel(): PENDING -> CANCELLED

OrderItem
- Attributes: order_id, event_seat_id, price (price at time of purchase)
- Functions: none beyond getters

Payment (simulated)
- Attributes: order_id, amount, status (PENDING / SUCCEEDED / FAILED)
- Functions:
  - markSucceeded(): PENDING -> SUCCEEDED; triggers order.confirm()
  - markFailed(): PENDING -> FAILED; triggers order.fail()

EventSeat state transitions:
- AVAILABLE -> HELD (hold placed)
- HELD -> AVAILABLE (hold expired or released)
- HELD -> SOLD (order confirmed)
- SOLD is final


Search Service: read-only view for discovery

EventDocument (copy of event data, optimised for filtering)
- Attributes: event_id, name, category, venue_name, city, location (latitude/longitude), start_time, min_price, max_price, available_seat_count, status
- Functions:
  - matches(filters): filter by location, date range, category and price range
  - v1: written by the Event Service over HTTP when an event is published, updated or cancelled (see APIs)


Deferred (add gradually)
- Idempotency for duplicate requests and retries
- Outbox + Kafka messages between services (EventPublished, SeatsHeld, OrderConfirmed, ...), replacing the v1 HTTP calls between services
- Event-driven notifications
- Redis for hold expiry
- Keeping available_seat_count in the search index up to date as seats are held and sold


APIs (v1)

Conventions
- JSON request and response bodies; all paths start with /api
- Timestamps are ISO-8601 UTC (e.g. "2026-12-31T20:00:00Z"); money is a decimal with 2 places (e.g. 79.50)
- List endpoints are paged with ?page=0&size=20 and return:
  { "content": [ ... ], "page": 0, "size": 20, "totalElements": 135, "totalPages": 7 }
- Every error uses the same body:
  { "status": 409, "error": "Conflict", "message": "Seat 12 is already held", "path": "/api/holds", "timestamp": "2026-10-18T22:10:00Z" }
- Status codes
  - 200 OK, 201 Created (with a Location header), 204 No Content
  - 400 Bad Request: validation failed (missing field, bad value)
  - 404 Not Found: the id does not exist
  - 409 Conflict: the request clashes with current state (seat already held, event already published, hold expired)


Event Service (port 8081)

POST /api/venues: create a venue
- Request:
  { "name": "Madison Square Garden", "address": "4 Pennsylvania Plaza", "city": "New York", "latitude": 40.7505, "longitude": -73.9934 }
- Response 201:
  { "id": 1, "name": "Madison Square Garden", "address": "4 Pennsylvania Plaza", "city": "New York", "latitude": 40.7505, "longitude": -73.9934, "sections": [] }
- Errors: 400

GET /api/venues/{venueId}: venue with its sections (no seats)
- Response 200:
  { "id": 1, "name": "Madison Square Garden", "address": "4 Pennsylvania Plaza", "city": "New York", "latitude": 40.7505, "longitude": -73.9934,
    "sections": [ { "id": 10, "name": "Floor" }, { "id": 11, "name": "Section 101" } ] }
- Errors: 404

POST /api/venues/{venueId}/sections: add a section
- Request:
  { "name": "Section 101" }
- Response 201:
  { "id": 11, "venueId": 1, "name": "Section 101" }
- Errors: 400, 404 (venue), 409 (name already used in this venue)

POST /api/sections/{sectionId}/seats: add seats in bulk
- Request: each row gets seats numbered 1..seatsPerRow
  { "rows": [ { "rowLabel": "A", "seatsPerRow": 20 }, { "rowLabel": "B", "seatsPerRow": 22 } ] }
- Response 201:
  { "sectionId": 11, "created": 42 }
- Errors: 400, 404 (section), 409 (a seat already exists)

GET /api/venues/{venueId}/layout: full layout, sections and their seats
- Response 200:
  { "venueId": 1, "sections": [
      { "id": 11, "name": "Section 101", "seats": [ { "id": 500, "rowLabel": "A", "seatNumber": 1 }, { "id": 501, "rowLabel": "A", "seatNumber": 2 } ] } ] }
- Errors: 404

POST /api/events: create an event (status DRAFT)
- Request:
  { "venueId": 1, "category": "CONCERT", "name": "Taylor Swift | The Eras Tour", "description": "...", "startTime": "2026-12-31T20:00:00Z" }
- Response 201: the Event response (below)
- Errors: 400 (unknown category, startTime in the past), 404 (venue)

GET /api/events/{eventId}: event details
- Response 200 (Event response):
  { "id": 7, "name": "Taylor Swift | The Eras Tour", "description": "...", "category": "CONCERT", "startTime": "2026-12-31T20:00:00Z", "status": "DRAFT",
    "venue": { "id": 1, "name": "Madison Square Garden", "city": "New York" } }
- Errors: 404

PUT /api/events/{eventId}: update details
- Request:
  { "category": "CONCERT", "name": "...", "description": "...", "startTime": "2026-12-31T21:00:00Z" }
- Response 200: the Event response
- Errors: 400, 404, 409 (event is CANCELLED)

POST /api/events/{eventId}/publish: DRAFT -> PUBLISHED
- No request body
- Requires a price for every section of the venue
- Calls Booking Service POST /api/events/{eventId}/inventory and Search Service PUT /api/search/events/{eventId}
- Response 200: the Event response
- Errors: 404, 409 (not DRAFT, or a section has no price)

POST /api/events/{eventId}/cancel: DRAFT or PUBLISHED -> CANCELLED
- No request body
- Calls Search Service PUT /api/search/events/{eventId} with status CANCELLED
- Response 200: the Event response
- Errors: 404, 409 (already CANCELLED)

PUT /api/events/{eventId}/prices: set the price of each section (replaces existing prices)
- Request:
  { "prices": [ { "sectionId": 10, "price": 250.00 }, { "sectionId": 11, "price": 79.50 } ] }
- Response 200: same body as GET prices
- Errors: 400 (price <= 0, section not in this venue), 404, 409 (event is not DRAFT)

GET /api/events/{eventId}/prices: prices per section
- Response 200:
  { "eventId": 7, "prices": [ { "sectionId": 10, "sectionName": "Floor", "price": 250.00 }, { "sectionId": 11, "sectionName": "Section 101", "price": 79.50 } ] }
- Errors: 404


Booking Service (port 8082)

POST /api/customers: create a customer
- Request:
  { "name": "Jane Doe", "email": "jane@example.com" }
- Response 201:
  { "id": 3, "name": "Jane Doe", "email": "jane@example.com" }
- Errors: 400, 409 (email already registered)

GET /api/customers/{customerId}: get a customer
- Response 200: same body as above
- Errors: 404

POST /api/events/{eventId}/inventory: create the EventSeats for an event (called by the Event Service on publish)
- Request: one entry per seat at the venue, with its price
  { "seats": [ { "seatId": 500, "sectionId": 11, "sectionName": "Section 101", "rowLabel": "A", "seatNumber": 1, "price": 79.50 } ] }
- Response 201:
  { "eventId": 7, "created": 18000 }
- Errors: 400, 409 (inventory already exists for this event)

GET /api/events/{eventId}/seats: seat map with live status (optional ?status=AVAILABLE)
- Response 200:
  { "eventId": 7, "seats": [
      { "eventSeatId": 9001, "sectionName": "Section 101", "rowLabel": "A", "seatNumber": 1, "price": 79.50, "status": "AVAILABLE" },
      { "eventSeatId": 9002, "sectionName": "Section 101", "rowLabel": "A", "seatNumber": 2, "price": 79.50, "status": "HELD" } ] }
- Errors: 404 (no inventory for this event)

POST /api/holds: hold one or more seats
- Request:
  { "customerId": 3, "eventId": 7, "eventSeatIds": [ 9001, 9003 ] }
- All or nothing: if any seat is not AVAILABLE, nothing is held
- Response 201 (Hold response):
  { "id": 40, "customerId": 3, "eventId": 7, "status": "ACTIVE", "expiresAt": "2026-10-18T22:20:00Z",
    "seats": [ { "eventSeatId": 9001, "sectionName": "Section 101", "rowLabel": "A", "seatNumber": 1, "price": 79.50 } ],
    "totalAmount": 159.00 }
- Errors: 400 (empty list, seats from another event), 404 (customer or seat), 409 (a seat is not AVAILABLE)

GET /api/holds/{holdId}: hold status and expiry
- Response 200: the Hold response
- Errors: 404

DELETE /api/holds/{holdId}: release a hold (ACTIVE -> RELEASED, seats back to AVAILABLE)
- Response 204
- Errors: 404, 409 (hold is not ACTIVE)

POST /api/orders: create an order from an active hold (status PENDING)
- Request:
  { "holdId": 40 }
- Response 201 (Order response):
  { "id": 55, "customerId": 3, "eventId": 7, "holdId": 40, "status": "PENDING", "totalAmount": 159.00,
    "items": [ { "eventSeatId": 9001, "sectionName": "Section 101", "rowLabel": "A", "seatNumber": 1, "price": 79.50 } ],
    "payment": null }
- Errors: 400, 404 (hold), 409 (hold is expired or not ACTIVE, or already has an order)

GET /api/orders/{orderId}: get an order
- Response 200: the Order response
- Errors: 404

POST /api/orders/{orderId}/pay: simulated payment
- Request: simulateFailure lets you test the failure path
  { "simulateFailure": false }
- On success: payment SUCCEEDED, order CONFIRMED, seats SOLD, hold CONVERTED
- On failure: payment FAILED, order FAILED, seats stay HELD until the hold expires
- Response 200: the Order response, with
  "payment": { "id": 70, "amount": 159.00, "status": "SUCCEEDED" }
- Errors: 404, 409 (order is not PENDING, or the hold expired before payment)

GET /api/customers/{customerId}/orders: a customer's orders (paged)
- Response 200: page of Order responses
- Errors: 404


Search Service (port 8083)

GET /api/search/events: discover and filter events (all parameters optional, paged)
- Query parameters:
  - city=New York
  - lat=40.75&lon=-73.99&radiusKm=25 (all three together)
  - from=2026-11-01T00:00:00Z&to=2026-12-31T23:59:59Z
  - category=CONCERT
  - minPrice=50&maxPrice=150 (matches events with any price in that range)
  - q=taylor (text match on name)
- Only PUBLISHED events with start_time in the future are returned; sorted by startTime
- Response 200: page of
  { "eventId": 7, "name": "Taylor Swift | The Eras Tour", "category": "CONCERT", "venueName": "Madison Square Garden", "city": "New York",
    "startTime": "2026-12-31T20:00:00Z", "minPrice": 79.50, "maxPrice": 250.00 }
- Errors: 400 (lat/lon/radiusKm incomplete, from after to, minPrice > maxPrice)

PUT /api/search/events/{eventId}: create or replace an event in the index (called by the Event Service)
- Request:
  { "name": "Taylor Swift | The Eras Tour", "category": "CONCERT", "venueName": "Madison Square Garden", "city": "New York",
    "latitude": 40.7505, "longitude": -73.9934, "startTime": "2026-12-31T20:00:00Z", "minPrice": 79.50, "maxPrice": 250.00, "status": "PUBLISHED" }
- Response 204
- Errors: 400


Background jobs (not APIs)
- Hold expiry (Booking Service): every 30 seconds, finds ACTIVE holds past expires_at, marks them EXPIRED and sets their seats back to AVAILABLE


Typical client flow
1. GET  /api/search/events?city=New York&category=CONCERT       (Search)
2. GET  /api/events/7                                           (Event)
3. GET  /api/events/7/seats?status=AVAILABLE                    (Booking)
4. POST /api/holds          { customerId, eventId, eventSeatIds } (Booking)
5. POST /api/orders         { holdId }                          (Booking)
6. POST /api/orders/55/pay  { simulateFailure: false }          (Booking)
