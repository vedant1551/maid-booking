# Maid Booking Service

A Spring Boot backend for booking home-service maids. Customers can find maids, book them now, at a set time or every week, pay per visit, cancel and rate. Data is kept in memory and payments are mocked.

## Running it

You need **JDK 17 or newer**. Gradle is not required; the wrapper downloads it on the first run.

**macOS / Linux**

```bash
./gradlew build
java -jar build/libs/maid-booking-1.0.0.jar
```

**Windows**

```bat
gradlew.bat build
java -jar build\libs\maid-booking-1.0.0.jar
```

The service starts on http://localhost:8080. All endpoints are documented and can be tried in Swagger UI at http://localhost:8080/swagger-ui/index.html.

## Design

* **Maid calendar.** Each maid has one calendar for all services, so she can't be double booked. Bookings lock only the days they touch, which keeps concurrent requests safe without blocking unrelated ones.
* **Visits.** A booking is made of visits. Each visit is paid, cancelled and rated on its own.
* **Plug-in points.** Booking types, payment modes, search filters, pricing and refund rules are interfaces with one class per option. Adding a new one doesn't change existing code.
* **Storage.** Kept behind repository interfaces, so the in-memory store can be replaced by a database.

## Tests

```bash
./gradlew test
```

End-to-end tests drive the API through full customer journeys:
* add a maid, search, and book;
* pay, cancel, and rate;
* concurrent requests for the same slot or payment.

Unit tests cover the core booking, pricing and refund rules.
