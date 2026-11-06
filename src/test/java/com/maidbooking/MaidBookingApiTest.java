package com.maidbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Each test is one full customer journey over HTTP. "Now" is fixed at Monday 7 Jan 2030, 09:00 IST. */
@SpringBootTest
@AutoConfigureMockMvc
class MaidBookingApiTest {
    private static final String TODAY = "2030-01-07";
    private static final String NEXT_MONDAY = "2030-01-14";
    private static final String NEXT_TUESDAY = "2030-01-15";
    private static final String THREE_MONDAYS = """
            {"type":"RECURRING","startDate":"2030-01-14","endDate":"2030-01-28","daysOfWeek":["MONDAY"],
             "dailyStartTime":"09:00","dailyEndTime":"10:30"}""";

    private static final Instant START_OF_TESTS = Instant.parse("2030-01-07T03:30:00Z");

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock(START_OF_TESTS);
        }
    }

    /** A clock the rating journey can move forward, to rate visits after they end. */
    static class TestClock extends Clock {
        private final ZoneId zone = ZoneId.of("Asia/Kolkata");
        private volatile Instant now;

        TestClock(Instant now) {
            this.now = now;
        }

        void setTo(String localDateTime) {
            now = LocalDateTime.parse(localDateTime).atZone(zone).toInstant();
        }

        void reset() {
            now = START_OF_TESTS;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Autowired
    private TestClock clock;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scheduledBookingJourney() throws Exception {
        String locality = uniqueLocality();

        // Add a maid: invalid requests are rejected, a valid one is saved.
        assertThat(call(addMaidRequest("{\"name\":\"Asha\"}"), 400).get("detail").asText()).contains("locality");
        call(addMaidRequest(maidJson("Asha", locality).replace("240", "240.555")), 400);
        call(addMaidRequest(maidJson("Asha", locality).replace("240", "0.50")), 400);
        call(addMaidRequest(maidJson("Asha", locality).replace("240", "1000000")), 400);
        assertThat(call(addMaidRequest(maidJson("Asha", locality).replace("COOKING", "PAINTING")), 400)
                .get("detail").asText()).contains("skills");
        assertThat(call(addMaidRequest(maidJson("Asha", locality).replace("\"name\"", "\"nickname\"")), 400)
                .get("detail").asText()).contains("nickname");
        call(addMaidRequest(maidJson("Asha", locality).replace("\"to\":\"18:00\"}",
                "\"to\":\"18:00\"},{\"day\":\"MONDAY\",\"from\":\"08:00\",\"to\":\"18:00\"}")), 400);
        String maidId = addMaid(maidJson("Asha", locality));

        // Fetch the maid.
        JsonNode maid = call(get("/maids/" + maidId), 200);
        assertThat(maid.get("name").asText()).isEqualTo("Asha");
        assertThat(maid.get("hourlyRate").decimalValue()).isEqualByComparingTo("240.00");
        call(get("/maids/" + UUID.randomUUID()), 404);

        // She shows up in search for 10:00-12:00 next Monday.
        String tenToTwelve = "locality=" + locality + "&startTime=" + NEXT_MONDAY + "T10:00&endTime=" + NEXT_MONDAY + "T12:00";
        assertThat(search(tenToTwelve)).hasSize(1);

        // Book 10:00-12:00.
        JsonNode booking = call(book(maidId, scheduled(NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T12:00")), 201);
        assertThat(booking.get("type").asText()).isEqualTo("SCHEDULED");
        assertThat(booking.at("/visits/0/startTime").asText()).isEqualTo(NEXT_MONDAY + "T10:00:00");
        assertThat(booking.at("/visits/0/endTime").asText()).isEqualTo(NEXT_MONDAY + "T12:00:00");
        assertThat(booking.at("/visits/0/price").decimalValue()).isEqualByComparingTo("480.00");
        assertThat(booking.at("/visits/0/status").asText()).isEqualTo("BOOKED");
        assertThat(call(get(bookingPath(booking)), 200).get("id")).isEqualTo(booking.get("id"));
        call(get("/bookings/" + UUID.randomUUID()), 404);

        // She no longer shows up for that time, and her day shows the booked and free time.
        assertThat(search(tenToTwelve)).isEmpty();
        JsonNode day = call(get("/maids/" + maidId + "/availability?date=" + NEXT_MONDAY), 200);
        assertThat(day.at("/booked/0/startTime").asText()).isEqualTo(NEXT_MONDAY + "T10:00:00");
        assertThat(day.get("free")).extracting(range -> range.get("startTime").asText() + "-" + range.get("endTime").asText())
                .containsExactly(NEXT_MONDAY + "T08:00:00-" + NEXT_MONDAY + "T10:00:00",
                        NEXT_MONDAY + "T12:00:00-" + NEXT_MONDAY + "T18:00:00");

        // The same slot, an overlapping slot, or another service at the same time cannot be booked.
        call(book(maidId, scheduled(NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T12:00")), 409);
        call(book(maidId, scheduled(NEXT_MONDAY + "T11:00", NEXT_MONDAY + "T13:00")), 409);
        call(book(maidId, "CLEANING", scheduled(NEXT_MONDAY + "T11:30", NEXT_MONDAY + "T12:30")), 409);

        // Back-to-back visits are fine.
        call(book(maidId, scheduled(NEXT_MONDAY + "T09:00", NEXT_MONDAY + "T10:00")), 201);
        call(book(maidId, scheduled(NEXT_MONDAY + "T12:00", NEXT_MONDAY + "T13:00")), 201);

        // Outside working hours, a day she does not work, a service she does not offer, bad times, unknown maid.
        call(book(maidId, scheduled(NEXT_MONDAY + "T17:00", NEXT_MONDAY + "T19:00")), 409);
        call(book(maidId, scheduled(NEXT_TUESDAY + "T10:00", NEXT_TUESDAY + "T11:00")), 409);
        call(book(maidId, "LAUNDRY", scheduled(NEXT_MONDAY + "T14:00", NEXT_MONDAY + "T15:00")), 400);
        call(book(maidId, scheduled("2020-01-01T10:00", "2020-01-01T11:00")), 400);
        call(book(maidId, scheduled(NEXT_MONDAY + "T15:00", NEXT_MONDAY + "T14:00")), 400);
        call(book(maidId, "{\"type\":\"SCHEDULED\",\"startTime\":\"" + NEXT_MONDAY + "T14:00\"}"), 400);
        call(book(maidId, "{\"type\":\"TELEPORT\"}"), 400);
        call(book(maidId, "{\"type\":\"SCHEDULED\",\"startTime\":\"" + NEXT_MONDAY + "T14:00\",\"endTime\":\""
                + NEXT_MONDAY + "T15:00\",\"durationMinutes\":30}"), 400);
        call(book(maidId, "{\"type\":\"INSTANT\",\"durationMinutes\":60,\"startTime\":\"" + NEXT_MONDAY + "T14:00\"}"), 400);
        call(book(UUID.randomUUID().toString(), scheduled(NEXT_MONDAY + "T14:00", NEXT_MONDAY + "T15:00")), 404);

        // Pay: invalid requests are rejected, a retry with the same key returns the same receipt.
        String visit = visitPath(booking, 0);
        call(pay(visit, "BITCOIN", "key-1"), 400);
        call(post(visit + "/payments").contentType("application/json").content("{\"mode\":\"UPI\"}"), 400);
        call(pay(bookingPath(booking) + "/visits/" + UUID.randomUUID(), "UPI", "key-1"), 404);
        JsonNode payment = call(pay(visit, "UPI", "key-1"), 200);
        assertThat(payment.get("amount").decimalValue()).isEqualByComparingTo("480.00");
        assertThat(call(pay(visit, "UPI", "key-1"), 200).get("reference")).isEqualTo(payment.get("reference"));
        call(pay(visit, "CARD", "key-2"), 409);
        assertThat(call(get(bookingPath(booking)), 200).at("/visits/0/status").asText()).isEqualTo("PAID");

        // Cancel a week ahead: full refund. Cancelling or paying again is rejected.
        JsonNode cancelled = call(delete(visit), 200);
        assertThat(cancelled.at("/visits/0/status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.at("/visits/0/refund/amount").decimalValue()).isEqualByComparingTo("480.00");
        call(delete(visit), 409);
        call(pay(visit, "UPI", "key-3"), 409);

        // The slot is free again: she shows up in search and another customer can book it.
        assertThat(search(tenToTwelve)).hasSize(1);
        call(book(maidId, scheduled(NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T12:00")), 201);
        assertThat(search(tenToTwelve)).isEmpty();
    }

    @Test
    void recurringBookingJourney() throws Exception {
        String locality = uniqueLocality();
        String maidId = addMaid(maidJson("Asha", locality));
        String firstMonday = "locality=" + locality + "&startTime=" + NEXT_MONDAY + "T09:00&endTime=" + NEXT_MONDAY + "T10:30";

        // A series starting today skips today, because 08:00 has already passed.
        JsonNode fromToday = call(book(maidId, """
                {"type":"RECURRING","startDate":"2030-01-07","endDate":"2030-01-14","daysOfWeek":["MONDAY"],
                 "dailyStartTime":"08:00","dailyEndTime":"08:30"}"""), 201);
        assertThat(fromToday.get("visits")).hasSize(1);
        assertThat(fromToday.at("/visits/0/startTime").asText()).isEqualTo(NEXT_MONDAY + "T08:00:00");
        call(delete(bookingPath(fromToday)), 200);

        // Invalid series are rejected.
        call(book(maidId, THREE_MONDAYS.replace("\"10:30\"", "\"08:30\"")), 400);
        call(book(maidId, THREE_MONDAYS.replace("2030-01-28", "2030-01-01")), 400);
        call(book(maidId, "{\"type\":\"RECURRING\",\"startDate\":\"2030-01-14\"}"), 400);

        // Someone already has the second Monday, so the whole series is rejected and the first Monday stays free.
        JsonNode blocker = call(book(maidId, scheduled("2030-01-21T10:00", "2030-01-21T11:00")), 201);
        call(book(maidId, THREE_MONDAYS), 409);
        assertThat(search(firstMonday)).hasSize(1);

        // Once that booking is cancelled, the series goes through with one visit per Monday.
        call(delete(bookingPath(blocker)), 200);
        JsonNode series = call(book(maidId, THREE_MONDAYS), 201);
        assertThat(series.get("type").asText()).isEqualTo("RECURRING");
        assertThat(series.get("visits")).extracting(v -> v.get("startTime").asText())
                .containsExactly("2030-01-14T09:00:00", "2030-01-21T09:00:00", "2030-01-28T09:00:00");
        assertThat(series.get("visits")).allMatch(v -> v.get("endTime").asText().endsWith("T10:30:00"));
        assertThat(series.at("/visits/0/price").decimalValue()).isEqualByComparingTo("360.00");
        assertThat(search(firstMonday)).isEmpty();

        // Each visit is paid on its own.
        call(pay(visitPath(series, 0), "CARD", "visit-1"), 200);
        call(pay(visitPath(series, 1), "WALLET", "visit-2"), 200);

        // Cancel just the second visit: its slot opens up, the others stay booked.
        JsonNode afterOne = call(delete(visitPath(series, 1)), 200);
        assertThat(afterOne.get("visits")).extracting(v -> v.get("status").asText())
                .containsExactly("PAID", "CANCELLED", "BOOKED");
        call(book(maidId, scheduled("2030-01-21T09:00", "2030-01-21T10:00")), 201);
        call(book(maidId, scheduled("2030-01-28T09:00", "2030-01-28T10:00")), 409);

        // Cancel the rest of the series: paid visit refunded, unpaid visit has no refund.
        JsonNode all = call(delete(bookingPath(series)), 200);
        assertThat(all.get("visits")).allMatch(v -> v.get("status").asText().equals("CANCELLED"));
        assertThat(all.at("/visits/0/refund/amount").decimalValue()).isEqualByComparingTo("360.00");
        assertThat(all.at("/visits/2/refund").isNull()).isTrue();
        call(delete(bookingPath(series)), 409);
        call(book(maidId, scheduled("2030-01-28T09:00", "2030-01-28T10:00")), 201);
    }

    @Test
    void instantBookingJourney() throws Exception {
        String locality = uniqueLocality();
        String maidId = addMaid(maidJson("Asha", locality));
        String availableNow = "locality=" + locality + "&availableNow=true&durationMinutes=60";

        assertThat(search(availableNow)).hasSize(1);
        call(book(maidId, "{\"type\":\"INSTANT\"}"), 400);

        JsonNode booking = call(book(maidId, "{\"type\":\"INSTANT\",\"durationMinutes\":90}"), 201);
        assertThat(booking.at("/visits/0/startTime").asText()).isEqualTo(TODAY + "T09:00:00");
        assertThat(booking.at("/visits/0/endTime").asText()).isEqualTo(TODAY + "T10:30:00");

        assertThat(search(availableNow)).isEmpty();
        call(book(maidId, "{\"type\":\"INSTANT\",\"durationMinutes\":30}"), 409);

        // The visit has already started, so it cannot be cancelled.
        call(delete(bookingPath(booking)), 409);
    }

    @Test
    void searchJourney() throws Exception {
        String locality = uniqueLocality();
        addMaid(maidJson("Asha", locality));
        addMaid(maidJson("Ravi", locality).replace("FEMALE", "MALE").replace("240", "180")
                .replace("\"COOKING\",\"CLEANING\"", "\"CLEANING\",\"DISHWASHING\""));
        addMaid(maidJson("Meena", locality).replace("240", "350")
                .replace("\"COOKING\",\"CLEANING\"", "\"COOKING\",\"BABYSITTING\""));

        // Profile filters. Nobody is rated yet, so the cheapest comes first.
        assertThat(names(search("locality=" + locality))).containsExactly("Ravi", "Asha", "Meena");
        assertThat(names(search("locality=" + locality.toUpperCase() + "&service=COOKING"))).containsExactly("Asha", "Meena");
        assertThat(names(search("locality=" + locality + "&skills=COOKING,CLEANING"))).containsExactly("Asha");
        assertThat(names(search("locality=" + locality + "&gender=MALE"))).containsExactly("Ravi");
        assertThat(names(search("locality=" + locality + "&minRating=1"))).isEmpty();
        assertThat(names(search("locality=" + locality + "&minRating=0"))).hasSize(3);
        assertThat(names(search("locality=" + locality + "&maxHourlyRate=240"))).containsExactly("Ravi", "Asha");
        assertThat(names(search("locality=" + locality + "&gender=FEMALE&maxHourlyRate=300&service=COOKING")))
                .containsExactly("Asha");

        // Availability filters.
        assertThat(search("locality=" + locality + "&date=" + NEXT_MONDAY + "&durationMinutes=120")).hasSize(3);
        assertThat(search("locality=" + locality + "&date=" + NEXT_TUESDAY)).isEmpty();
        assertThat(search("locality=" + locality + "&startTime=" + NEXT_MONDAY + "T17:00&endTime=" + NEXT_MONDAY + "T19:00")).isEmpty();

        // Invalid searches.
        call(get("/maids?startTime=" + NEXT_MONDAY + "T10:00"), 400);
        call(get("/maids?date=" + NEXT_MONDAY + "&availableNow=true"), 400);
        call(get("/maids?date=2020-01-01"), 400);
        call(get("/maids?startTime=" + NEXT_MONDAY + "T10:00&endTime=" + NEXT_MONDAY + "T11:00&durationMinutes=30"), 400);
        assertThat(call(get("/maids?gender=ROBOT"), 400).get("detail").asText()).contains("gender");
    }

    @Test
    void paymentAndRefundJourney() throws Exception {
        String maidId = addMaid(maidJson("Asha", uniqueLocality()));

        // Refund depends on notice: under 2 hours none, 2-24 hours half, 24 hours or more full.
        assertThat(payThenCancel(maidId, TODAY + "T10:00", TODAY + "T11:00")).isEqualByComparingTo("0.00");
        assertThat(payThenCancel(maidId, TODAY + "T12:00", TODAY + "T13:00")).isEqualByComparingTo("120.00");
        assertThat(payThenCancel(maidId, NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T11:00")).isEqualByComparingTo("240.00");

        // A cancelled unpaid visit has no refund.
        JsonNode unpaid = call(book(maidId, scheduled(NEXT_MONDAY + "T14:00", NEXT_MONDAY + "T15:00")), 201);
        assertThat(call(delete(visitPath(unpaid, 0)), 200).at("/visits/0/refund").isNull()).isTrue();

        // An Idempotency-Key belongs to one visit only.
        JsonNode first = call(book(maidId, scheduled(NEXT_MONDAY + "T15:00", NEXT_MONDAY + "T16:00")), 201);
        JsonNode second = call(book(maidId, scheduled(NEXT_MONDAY + "T16:00", NEXT_MONDAY + "T17:00")), 201);
        String sharedKey = UUID.randomUUID().toString();
        call(payWithKey(visitPath(first, 0), sharedKey), 200);
        call(payWithKey(visitPath(second, 0), sharedKey), 409);
    }

    @Test
    void ratingJourney() throws Exception {
        String locality = uniqueLocality();
        String maidId = addMaid(maidJson("Asha", locality));
        String otherMaidId = addMaid(maidJson("Ravi", locality).replace("240", "180"));
        assertThat(call(get("/maids/" + maidId), 200).get("rating").isNull()).isTrue();

        JsonNode morning = call(book(maidId, scheduled(NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T11:00")), 201);
        JsonNode noon = call(book(maidId, scheduled(NEXT_MONDAY + "T12:00", NEXT_MONDAY + "T13:00")), 201);
        JsonNode cancelled = call(book(maidId, scheduled(NEXT_MONDAY + "T14:00", NEXT_MONDAY + "T15:00")), 201);
        call(delete(bookingPath(cancelled)), 200);

        // Too early: the visit has not happened yet.
        call(rate(visitPath(morning, 0), 4), 409);

        try {
            clock.setTo(NEXT_MONDAY + "T16:00");

            // Invalid stars, unknown visit, cancelled visit.
            call(rate(visitPath(morning, 0), 0), 400);
            call(rate(visitPath(morning, 0), 6), 400);
            call(rate(bookingPath(morning) + "/visits/" + UUID.randomUUID(), 4), 404);
            call(rate(visitPath(cancelled, 0), 4), 409);

            // Rate both finished visits once each. Her rating is the average.
            assertThat(call(rate(visitPath(morning, 0), 4), 200).at("/visits/0/rating").asInt()).isEqualTo(4);
            call(rate(visitPath(morning, 0), 5), 409);
            call(rate(visitPath(noon, 0), 5), 200);

            // Paying after the visit is allowed, so a finished visit is never stuck unpaid.
            call(pay(visitPath(noon, 0), "UPI", "after-visit"), 200);

            JsonNode maid = call(get("/maids/" + maidId), 200);
            assertThat(maid.get("rating").decimalValue()).isEqualByComparingTo("4.5");
            assertThat(maid.get("ratingCount").asInt()).isEqualTo(2);

            // Rated maids come first in search, and the rating filter now finds her.
            assertThat(names(search("locality=" + locality))).containsExactly("Asha", "Ravi");
            assertThat(names(search("locality=" + locality + "&minRating=4.5"))).containsExactly("Asha");
            assertThat(names(search("locality=" + locality + "&minRating=4.6"))).isEmpty();
            assertThat(call(get("/maids/" + otherMaidId), 200).get("ratingCount").asInt()).isZero();
        } finally {
            clock.reset();
        }
    }

    @RepeatedTest(10)
    void concurrentCustomersJourney() throws Exception {
        String locality = uniqueLocality();
        String maidId = addMaid(maidJson("Asha", locality));

        // Eight customers try the same slot at once: exactly one gets it.
        List<Integer> sameSlot = runTogether(8,
                () -> statusOf(book(maidId, scheduled(NEXT_MONDAY + "T10:00", NEXT_MONDAY + "T11:00"))));
        assertThat(sameSlot).containsOnlyOnce(201).containsOnly(201, 409);

        // A recurring booking and a single booking race for the series' last Monday: one wins, never half a series.
        String series = """
                {"type":"RECURRING","startDate":"2030-01-14","endDate":"2030-01-28","daysOfWeek":["MONDAY"],
                 "dailyStartTime":"08:00","dailyEndTime":"09:00"}""";
        List<Integer> race = runTogetherEach(List.of(
                () -> statusOf(book(maidId, series)),
                () -> statusOf(book(maidId, scheduled("2030-01-28T08:00", "2030-01-28T09:00")))));
        assertThat(race).containsExactlyInAnyOrder(201, 409);
        boolean seriesWon = race.get(0) == 201;
        assertThat(search("locality=" + locality + "&startTime=2030-01-21T08:00&endTime=2030-01-21T09:00"))
                .hasSize(seriesWon ? 0 : 1);

        // The same payment sent eight times at once is charged once.
        JsonNode booking = call(book(maidId, scheduled(NEXT_MONDAY + "T12:00", NEXT_MONDAY + "T13:00")), 201);
        String visit = visitPath(booking, 0);
        List<String> receipts = runTogether(8, () -> call(pay(visit, "UPI", "same-key"), 200).get("reference").asText());
        assertThat(new HashSet<>(receipts)).hasSize(1);

        // Payment and cancellation at the same time: the visit ends cancelled, and any payment is refunded.
        JsonNode other = call(book(maidId, scheduled(NEXT_MONDAY + "T14:00", NEXT_MONDAY + "T15:00")), 201);
        runTogetherEach(List.of(
                () -> statusOf(pay(visitPath(other, 0), "UPI", "key")),
                () -> statusOf(delete(bookingPath(other)))));
        JsonNode result = call(get(bookingPath(other)), 200).at("/visits/0");
        assertThat(result.get("status").asText()).isEqualTo("CANCELLED");
        if (!result.get("payment").isNull()) {
            assertThat(result.at("/refund/amount").decimalValue()).isEqualByComparingTo("240.00");
        }
    }

    private BigDecimal payThenCancel(String maidId, String start, String end) throws Exception {
        JsonNode booking = call(book(maidId, scheduled(start, end)), 201);
        call(pay(visitPath(booking, 0), "UPI", "key"), 200);
        return call(delete(visitPath(booking, 0)), 200).at("/visits/0/refund/amount").decimalValue();
    }

    private static MockHttpServletRequestBuilder rate(String visitPath, int stars) {
        return post(visitPath + "/rating").contentType("application/json").content("{\"stars\":" + stars + "}");
    }

    private static String uniqueLocality() {
        return "area-" + UUID.randomUUID();
    }

    private String addMaid(String json) throws Exception {
        return call(addMaidRequest(json), 201).get("id").asText();
    }

    private static MockHttpServletRequestBuilder addMaidRequest(String json) {
        return post("/maids").contentType("application/json").content(json);
    }

    private static String maidJson(String name, String locality) {
        return """
                {"name":"%s","locality":"%s","skills":["COOKING","CLEANING"],"gender":"FEMALE",
                 "hourlyRate":240,
                 "workingHours":[{"day":"MONDAY","from":"08:00","to":"18:00"}]}""".formatted(name, locality);
    }

    private static String scheduled(String start, String end) {
        return "{\"type\":\"SCHEDULED\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    private static MockHttpServletRequestBuilder book(String maidId, String schedule) {
        return book(maidId, "COOKING", schedule);
    }

    private static MockHttpServletRequestBuilder book(String maidId, String service, String schedule) {
        String body = "{\"customerId\":\"customer-1\",\"service\":\"" + service + "\"," + schedule.strip().substring(1);
        return post("/maids/" + maidId + "/bookings").contentType("application/json").content(body);
    }

    // Keys are global, so each visit gets its own key by prefixing the visit path.
    private static MockHttpServletRequestBuilder pay(String visitPath, String mode, String key) {
        return post(visitPath + "/payments").header("Idempotency-Key", visitPath + "-" + key)
                .contentType("application/json").content("{\"mode\":\"" + mode + "\"}");
    }

    private static MockHttpServletRequestBuilder payWithKey(String visitPath, String key) {
        return post(visitPath + "/payments").header("Idempotency-Key", key)
                .contentType("application/json").content("{\"mode\":\"UPI\"}");
    }

    private static String bookingPath(JsonNode booking) {
        return "/bookings/" + booking.get("id").asText();
    }

    private static String visitPath(JsonNode booking, int index) {
        return bookingPath(booking) + "/visits/" + booking.at("/visits/" + index + "/id").asText();
    }

    private static List<String> names(JsonNode maids) {
        List<String> names = new ArrayList<>();
        maids.forEach(maid -> names.add(maid.get("name").asText()));
        return names;
    }

    private JsonNode search(String query) throws Exception {
        return call(get("/maids?" + query), 200);
    }

    private int statusOf(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private JsonNode call(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        byte[] body = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readTree(body);
    }

    private static <T> List<T> runTogether(int times, Callable<T> task) throws Exception {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            tasks.add(task);
        }
        return runTogetherEach(tasks);
    }

    private static <T> List<T> runTogetherEach(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CyclicBarrier startLine = new CyclicBarrier(tasks.size());
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    startLine.await(5, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
