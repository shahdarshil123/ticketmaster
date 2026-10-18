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
- Attributes: event_id, seat_id, section_id, price, status (AVAILABLE / HELD / SOLD), hold_id, version
  - unique (event_id, seat_id)
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
  - how it is kept in sync with Event and Booking services is decided later


Deferred (add gradually)
- Idempotency for duplicate requests and retries
- Outbox + Kafka messages between services (EventPublished, SeatsHeld, OrderConfirmed, ...)
- Event-driven notifications
- Redis for hold expiry
