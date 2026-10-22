package dev.leaveassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * End-to-end test: the real application, a real PostgreSQL (Testcontainers) and real HTTP calls.
 * The tests are not transactional, so every test starts by resetting the leave data.
 * The dates are fixed weeks in December 2026 (Mon 7 to Fri 11 has no public holidays).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class LeaveApiTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        jdbc.update("DELETE FROM leave_request_event");
        jdbc.update("DELETE FROM leave_request");
        jdbc.update("UPDATE leave_balance SET used_days = 0");
    }

    // ---- helpers ----

    private ResponseEntity<String> get(String path) {
        return client.get().uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> { })
                .toEntity(String.class);
    }

    private ResponseEntity<String> post(String path, String json) {
        return client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> { })
                .toEntity(String.class);
    }

    private ResponseEntity<String> postWithoutBody(String path) {
        return client.post().uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> { })
                .toEntity(String.class);
    }

    private static String annualLeave(String start, String end) {
        return "{\"leaveType\":\"ANNUAL\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                + "\",\"reason\":\"test\"}";
    }

    private static long idOf(ResponseEntity<String> response) {
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(response.getBody());
        assertTrue(matcher.find(), "no id in " + response.getBody());
        return Long.parseLong(matcher.group(1));
    }

    private ResponseEntity<String> createWeekOfDecember7() {
        return post("/api/leave-requests", annualLeave("2026-12-07", "2026-12-11"));
    }

    // ---- tests ----

    @Test
    void createReturnsTheRequestAndReservesTheDays() {
        ResponseEntity<String> created = createWeekOfDecember7();
        long id = idOf(created);

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("/api/leave-requests/" + id, created.getHeaders().getLocation().toString());
        assertTrue(created.getBody().contains("\"status\":\"PENDING\""));
        assertTrue(created.getBody().contains("\"workingDays\":5"));

        assertTrue(get("/api/leave-requests").getBody().contains("\"id\":" + id));
        assertTrue(get("/api/leave-requests/" + id).getBody().contains("\"startDate\":\"2026-12-07\""));

        String balances = get("/api/balances").getBody();
        assertTrue(balances.contains("\"usedDays\":5"), balances);
        assertTrue(balances.contains("\"remainingDays\":21"), balances);
    }

    @Test
    void cancellingGivesTheDaysBackOnlyOnce() {
        long id = idOf(createWeekOfDecember7());

        ResponseEntity<String> cancelled = postWithoutBody("/api/leave-requests/" + id + "/cancel");
        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        assertTrue(cancelled.getBody().contains("\"status\":\"CANCELLED\""));
        assertTrue(get("/api/balances").getBody().contains("\"remainingDays\":26"));

        ResponseEntity<String> again = postWithoutBody("/api/leave-requests/" + id + "/cancel");
        assertEquals(HttpStatus.CONFLICT, again.getStatusCode());
        assertTrue(again.getBody().contains("final status"), again.getBody());
        assertTrue(get("/api/balances").getBody().contains("\"remainingDays\":26"));
    }

    @Test
    void approvingIsRecordedInTheHistory() {
        long id = idOf(createWeekOfDecember7());

        ResponseEntity<String> approved = postWithoutBody("/api/leave-requests/" + id + "/approve");
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertTrue(approved.getBody().contains("\"status\":\"APPROVED\""));

        String history = get("/api/leave-requests/" + id + "/history").getBody();
        assertTrue(history.contains("\"fromStatus\":null"), history);
        assertTrue(history.contains("\"toStatus\":\"PENDING\""), history);
        assertTrue(history.contains("\"fromStatus\":\"PENDING\""), history);
        assertTrue(history.contains("\"toStatus\":\"APPROVED\""), history);
    }

    @Test
    void anOverlappingRequestIsRejectedWithAConflict() {
        createWeekOfDecember7();

        ResponseEntity<String> overlapping = post("/api/leave-requests", annualLeave("2026-12-09", "2026-12-15"));

        assertEquals(HttpStatus.CONFLICT, overlapping.getStatusCode());
        assertTrue(overlapping.getBody().contains("Overlaps with an existing request"), overlapping.getBody());
    }

    @Test
    void aRequestLargerThanTheBalanceIsRejectedWithAConflict() {
        ResponseEntity<String> tooLong = post("/api/leave-requests", annualLeave("2026-12-01", "2027-01-29"));

        assertEquals(HttpStatus.CONFLICT, tooLong.getStatusCode());
        assertTrue(tooLong.getBody().contains("remaining"), tooLong.getBody());
    }

    @Test
    void missingFieldsAreABadRequest() {
        ResponseEntity<String> response = post("/api/leave-requests", "{\"leaveType\":\"ANNUAL\",\"endDate\":\"2026-12-11\"}");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void startAfterEndIsABadRequest() {
        ResponseEntity<String> response = post("/api/leave-requests", annualLeave("2026-12-11", "2026-12-07"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void anInvalidDateIsABadRequest() {
        ResponseEntity<String> response = post("/api/leave-requests", annualLeave("not-a-date", "2026-12-07"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void anUnknownRequestIsNotFound() {
        assertEquals(HttpStatus.NOT_FOUND, get("/api/leave-requests/999999").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                postWithoutBody("/api/leave-requests/999999/approve").getStatusCode());
    }
}
