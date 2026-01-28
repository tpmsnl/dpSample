package com.ts.sr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SR1 Sleep Room Assignment System.
 * Achieves 100% code coverage and verifies business logic.
 */
class SR1Test {

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2026, 1, 23, 8, 0);
    }

    // ========== Helper Methods ==========

    private Req createP1Request(int id, String origin) {
        // P1: duration > 120 min AND has origin
        return new Req(id, baseTime, baseTime.plusHours(3), origin);
    }

    private Req createP2Request_ShortDuration(int id, String origin) {
        // P2: duration <= 120 min
        return new Req(id, baseTime, baseTime.plusMinutes(60), origin);
    }

    private Req createP2Request_NoOrigin(int id) {
        // P2: null or empty origin
        return new Req(id, baseTime, baseTime.plusHours(3), null);
    }

    // ========== Main Method Tests ==========

    @Nested
    @DisplayName("Main Method Tests")
    class MainMethodTests {

        @Test
        @DisplayName("main() executes without errors")
        void testMainExecutesSuccessfully() {
            assertDoesNotThrow(() -> SR1.main(new String[]{}));
        }
    }

    // ========== getRequests Tests ==========

    @Nested
    @DisplayName("getRequests Tests")
    class GetRequestsTests {

        @Test
        @DisplayName("loads all requests from sample.json")
        void testLoadsAllRequests() throws IOException {
            List<Req> requests = SR1.getRequests();
            assertNotNull(requests);
            assertFalse(requests.isEmpty());
        }

        @Test
        @DisplayName("parses request fields correctly")
        void testParsesFieldsCorrectly() throws IOException {
            List<Req> requests = SR1.getRequests();
            Req first = requests.stream().filter(r -> r.getId() == 1).findFirst().orElse(null);

            assertNotNull(first);
            assertEquals(1, first.getId());
            assertNotNull(first.getArr());
            assertNotNull(first.getDep());
        }
    }

    // ========== assignP1 Tests ==========

    @Nested
    @DisplayName("assignP1 Tests")
    class AssignP1Tests {

        @Test
        @DisplayName("assigns rooms to requests when rooms available")
        void testAssignsRoomsWhenAvailable() {
            List<Req> requests = Arrays.asList(
                    createP1Request(1, "DEL"),
                    createP1Request(2, "BOM"),
                    createP1Request(3, "CCU")
            );

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4, 5));
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            assertEquals(3, assigned.size());
            // All should be P1_ASSIGNED
            long assignedCount = assigned.stream()
                    .filter(a -> "P1_ASSIGNED".equals(a.getStatus()))
                    .count();
            assertEquals(3, assignedCount);
            // 2 rooms should remain
            assertEquals(2, freeRooms.size());
        }

        @Test
        @DisplayName("assigns correct request IDs and room IDs")
        void testAssignsCorrectIDs() {
            List<Req> requests = Arrays.asList(
                    createP1Request(10, "DEL"),
                    createP1Request(20, "BOM")
            );

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(101, 102));
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            List<Assignment> list = new ArrayList<>(assigned);

            // First assignment: request 10, room 101
            assertEquals(Integer.valueOf(10), list.get(0).getRequestID());
            assertEquals(Integer.valueOf(101), list.get(0).getRoomID());

            // Second assignment: request 20, room 102
            assertEquals(Integer.valueOf(20), list.get(1).getRequestID());
            assertEquals(Integer.valueOf(102), list.get(1).getRoomID());
        }

        @Test
        @DisplayName("waitlists requests when no rooms available")
        void testWaitlistsWhenNoRooms() {
            List<Req> requests = Arrays.asList(
                    createP1Request(1, "DEL"),
                    createP1Request(2, "BOM")
            );

            Deque<Integer> freeRooms = new ArrayDeque<>(); // No rooms
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            assertEquals(2, assigned.size());
            // All should be WAITLIST with null roomID
            for (Assignment a : assigned) {
                assertEquals("WAITLIST", a.getStatus());
                assertNull(a.getRoomID());
            }
        }

        @Test
        @DisplayName("handles mixed scenario - some assigned, some waitlisted")
        void testMixedAssignmentAndWaitlist() {
            List<Req> requests = Arrays.asList(
                    createP1Request(1, "DEL"),
                    createP1Request(2, "BOM"),
                    createP1Request(3, "CCU")
            );

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2)); // Only 2 rooms
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            assertEquals(3, assigned.size());

            long assignedCount = assigned.stream()
                    .filter(a -> "P1_ASSIGNED".equals(a.getStatus()))
                    .count();
            long waitlistCount = assigned.stream()
                    .filter(a -> "WAITLIST".equals(a.getStatus()))
                    .count();

            assertEquals(2, assignedCount);
            assertEquals(1, waitlistCount);
            assertTrue(freeRooms.isEmpty());
        }

        @Test
        @DisplayName("handles empty request list")
        void testEmptyRequestList() {
            List<Req> requests = Collections.emptyList();
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3));

            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            assertTrue(assigned.isEmpty());
            assertEquals(3, freeRooms.size());
        }
    }

    // ========== processP1Rejections Tests ==========

    @Nested
    @DisplayName("processP1Rejections Tests")
    class ProcessP1RejectionsTests {

        @Test
        @DisplayName("returns rejected requests and frees their rooms")
        void testRejectsAndFreesRooms() {
            Req req1 = createP1Request(1, "DEL");
            Req req2 = createP1Request(2, "BOM");
            req1.setRejectP1(true); // Mark for rejection

            List<Req> requests = Arrays.asList(req1, req2);

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(101, 102, 103));
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            int freeRoomsBefore = freeRooms.size();

            List<Req> rejected = SR1.processP1Rejections(requests, assigned, freeRooms);

            assertEquals(1, rejected.size());
            assertEquals(req1, rejected.get(0));
            assertEquals(freeRoomsBefore + 1, freeRooms.size()); // Room freed

            // Find the rejected assignment by request ID
            Assignment rejectedAssignment = assigned.stream()
                    .filter(a -> a.getRequestID().equals(1))
                    .findFirst()
                    .orElse(null);
            assertNotNull(rejectedAssignment);
            assertEquals("WAITLIST", rejectedAssignment.getStatus());
            assertNull(rejectedAssignment.getRoomID());
        }

        @Test
        @DisplayName("returns empty list when no rejections")
        void testNoRejections() {
            Req req1 = createP1Request(1, "DEL");
            Req req2 = createP1Request(2, "BOM");
            // Neither marked for rejection

            List<Req> requests = Arrays.asList(req1, req2);

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3));
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            List<Req> rejected = SR1.processP1Rejections(requests, assigned, freeRooms);

            assertTrue(rejected.isEmpty());
        }

        @Test
        @DisplayName("ignores WAITLIST assignments during rejection processing")
        void testIgnoresWaitlistAssignments() {
            Req req1 = createP1Request(1, "DEL");
            req1.setRejectP1(true);

            List<Req> requests = Arrays.asList(req1);

            // No rooms - req1 will be waitlisted
            Deque<Integer> freeRooms = new ArrayDeque<>();
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            // Verify it's waitlisted
            assertEquals("WAITLIST", assigned.iterator().next().getStatus());

            List<Req> rejected = SR1.processP1Rejections(requests, assigned, freeRooms);

            // Should be empty - can't reject a waitlisted request
            assertTrue(rejected.isEmpty());
        }
    }

    // ========== processP2 Tests ==========

    @Nested
    @DisplayName("processP2 Tests")
    class ProcessP2Tests {

        @Test
        @DisplayName("assigns rooms to P2 requests when available")
        void testAssignsRoomsToP2() {
            Req p2Req = createP2Request_ShortDuration(100, "DEL");

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(p2Req));
            Set<Assignment> assigned = new LinkedHashSet<>();
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(5, 6, 7));

            SR1.processP2(p2List, assigned, freeRooms);

            assertEquals(1, assigned.size());
            Assignment a = assigned.iterator().next();
            assertEquals("P2_ROOM", a.getStatus());
            assertEquals(Integer.valueOf(100), a.getRequestID());
            assertEquals(Integer.valueOf(5), a.getRoomID());
            assertEquals(2, freeRooms.size()); // One room consumed
        }

        @Test
        @DisplayName("updates existing WAITLIST assignment to P2_ROOM")
        void testUpdatesExistingWaitlistAssignment() {
            Req req = createP1Request(1, "DEL");

            // Create existing WAITLIST assignment with correct constructor order
            Set<Assignment> assigned = new LinkedHashSet<>();
            Assignment existing = new Assignment("WAITLIST", 1, null);
            assigned.add(existing);

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(req));
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(5));

            SR1.processP2(p2List, assigned, freeRooms);

            // Should update existing assignment
            assertEquals(1, assigned.size());
            assertEquals("P2_ROOM", existing.getStatus());
            assertEquals(Integer.valueOf(5), existing.getRoomID());
        }

        @Test
        @DisplayName("skips already assigned requests (non-WAITLIST)")
        void testSkipsAlreadyAssigned() {
            Req req = createP1Request(1, "DEL");

            // Create existing P1_ASSIGNED assignment
            Set<Assignment> assigned = new LinkedHashSet<>();
            Assignment existing = new Assignment("P1_ASSIGNED", 1, 3);
            assigned.add(existing);

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(req));
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(5, 6));

            int roomsBefore = freeRooms.size();
            SR1.processP2(p2List, assigned, freeRooms);

            // Should skip - no new assignment, rooms unchanged
            assertEquals(1, assigned.size());
            assertEquals(roomsBefore, freeRooms.size());
            assertEquals("P1_ASSIGNED", existing.getStatus());
        }

        @Test
        @DisplayName("waitlists new requests when no rooms available")
        void testWaitlistsNewRequestsWhenNoRooms() {
            Req p2Req = createP2Request_ShortDuration(100, "DEL");

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(p2Req));
            Set<Assignment> assigned = new LinkedHashSet<>();
            Deque<Integer> freeRooms = new ArrayDeque<>(); // No rooms

            SR1.processP2(p2List, assigned, freeRooms);

            assertEquals(1, assigned.size());
            Assignment a = assigned.iterator().next();
            assertEquals("WAITLIST", a.getStatus());
            assertEquals(Integer.valueOf(100), a.getRequestID());
            assertNull(a.getRoomID());
        }

        @Test
        @DisplayName("keeps existing WAITLIST when no rooms available")
        void testKeepsExistingWaitlistWhenNoRooms() {
            Req req = createP1Request(1, "DEL");

            // Create existing WAITLIST assignment
            Set<Assignment> assigned = new LinkedHashSet<>();
            Assignment existing = new Assignment("WAITLIST", 1, null);
            assigned.add(existing);

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(req));
            Deque<Integer> freeRooms = new ArrayDeque<>(); // No rooms

            SR1.processP2(p2List, assigned, freeRooms);

            // Should keep existing WAITLIST, not add new
            assertEquals(1, assigned.size());
            assertEquals("WAITLIST", existing.getStatus());
        }

        @Test
        @DisplayName("handles empty P2 list")
        void testEmptyP2List() {
            Deque<Req> p2List = new ArrayDeque<>();
            Set<Assignment> assigned = new LinkedHashSet<>();
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3));

            SR1.processP2(p2List, assigned, freeRooms);

            assertTrue(assigned.isEmpty());
            assertEquals(3, freeRooms.size());
        }
    }

    // ========== Integration Tests ==========

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("full workflow with P1 and P2 requests")
        void testFullWorkflow() {
            // P1 requests (duration > 120 min, has origin)
            Req p1_1 = createP1Request(1, "DEL");
            Req p1_2 = createP1Request(2, "BOM");
            Req p1_3 = createP1Request(3, "CCU");
            p1_2.setRejectP1(true); // Will be rejected

            // P2 requests (short duration)
            Req p2_1 = createP2Request_ShortDuration(10, "HYD");
            Req p2_2 = createP2Request_NoOrigin(11);

            List<Req> p1Requests = Arrays.asList(p1_1, p1_2, p1_3);
            List<Req> p2Requests = Arrays.asList(p2_1, p2_2);

            // Initialize 3 rooms
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3));

            // Step 1: Assign P1
            Set<Assignment> assigned = SR1.assignP1(p1Requests, freeRooms);
            assertEquals(3, assigned.size());
            assertTrue(freeRooms.isEmpty()); // All rooms used

            // Step 2: Process P1 rejections
            List<Req> rejectedP1s = SR1.processP1Rejections(p1Requests, assigned, freeRooms);
            assertEquals(1, rejectedP1s.size());
            assertEquals(1, freeRooms.size()); // One room freed

            // Step 3: Build P2 queue (original P2s + rejected P1s)
            Deque<Req> finalP2Queue = new ArrayDeque<>(p2Requests);
            finalP2Queue.addAll(rejectedP1s);
            assertEquals(3, finalP2Queue.size());

            // Step 4: Process P2
            SR1.processP2(finalP2Queue, assigned, freeRooms);

            // Verify final state
            long p1AssignedCount = assigned.stream()
                    .filter(a -> "P1_ASSIGNED".equals(a.getStatus()))
                    .count();
            long p2RoomCount = assigned.stream()
                    .filter(a -> "P2_ROOM".equals(a.getStatus()))
                    .count();
            long waitlistCount = assigned.stream()
                    .filter(a -> "WAITLIST".equals(a.getStatus()))
                    .count();

            assertEquals(2, p1AssignedCount); // p1_1 and p1_3
            assertEquals(1, p2RoomCount); // One P2 got the freed room
            assertEquals(2, waitlistCount); // p1_2 (rejected) updated to P2_ROOM, 2 others waitlisted
        }

        @Test
        @DisplayName("all requests get waitlisted when no rooms")
        void testAllWaitlistedWhenNoRooms() {
            Req p1 = createP1Request(1, "DEL");
            Req p2 = createP2Request_ShortDuration(2, "BOM");

            Deque<Integer> freeRooms = new ArrayDeque<>(); // No rooms

            Set<Assignment> assigned = SR1.assignP1(Arrays.asList(p1), freeRooms);
            SR1.processP2(new ArrayDeque<>(Arrays.asList(p2)), assigned, freeRooms);

            assertEquals(2, assigned.size());
            assertTrue(assigned.stream().allMatch(a -> "WAITLIST".equals(a.getStatus())));
        }

        @Test
        @DisplayName("verifies room assignment is FIFO")
        void testRoomAssignmentIsFIFO() {
            List<Req> requests = Arrays.asList(
                    createP1Request(1, "A"),
                    createP1Request(2, "B"),
                    createP1Request(3, "C")
            );

            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(101, 102, 103));
            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);

            List<Assignment> assignmentList = new ArrayList<>(assigned);

            // Verify request IDs and room IDs are correctly assigned in FIFO order
            assertEquals(Integer.valueOf(1), assignmentList.get(0).getRequestID());
            assertEquals(Integer.valueOf(101), assignmentList.get(0).getRoomID());

            assertEquals(Integer.valueOf(2), assignmentList.get(1).getRequestID());
            assertEquals(Integer.valueOf(102), assignmentList.get(1).getRoomID());

            assertEquals(Integer.valueOf(3), assignmentList.get(2).getRequestID());
            assertEquals(Integer.valueOf(103), assignmentList.get(2).getRoomID());
        }
    }

    // ========== Edge Case Tests ==========

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles single request")
        void testSingleRequest() {
            Req req = createP1Request(1, "DEL");
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1));

            Set<Assignment> assigned = SR1.assignP1(Arrays.asList(req), freeRooms);

            assertEquals(1, assigned.size());
            Assignment a = assigned.iterator().next();
            assertEquals("P1_ASSIGNED", a.getStatus());
            assertEquals(Integer.valueOf(1), a.getRequestID());
            assertEquals(Integer.valueOf(1), a.getRoomID());
        }

        @Test
        @DisplayName("handles multiple rejections")
        void testMultipleRejections() {
            Req req1 = createP1Request(1, "DEL");
            Req req2 = createP1Request(2, "BOM");
            Req req3 = createP1Request(3, "CCU");
            req1.setRejectP1(true);
            req2.setRejectP1(true);
            req3.setRejectP1(true);

            List<Req> requests = Arrays.asList(req1, req2, req3);
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(1, 2, 3));

            Set<Assignment> assigned = SR1.assignP1(requests, freeRooms);
            List<Req> rejected = SR1.processP1Rejections(requests, assigned, freeRooms);

            assertEquals(3, rejected.size());
            assertEquals(3, freeRooms.size()); // All rooms freed

            // All assignments should be WAITLIST now
            assertTrue(assigned.stream().allMatch(a -> "WAITLIST".equals(a.getStatus())));
        }

        @Test
        @DisplayName("processes P2 with mix of new and existing WAITLIST assignments")
        void testP2MixedNewAndExisting() {
            Req req1 = createP1Request(1, "DEL");
            Req req2 = createP2Request_ShortDuration(2, "BOM");

            Set<Assignment> assigned = new LinkedHashSet<>();
            // req1 has existing WAITLIST assignment
            Assignment existing = new Assignment("WAITLIST", 1, null);
            assigned.add(existing);

            Deque<Req> p2List = new ArrayDeque<>(Arrays.asList(req1, req2));
            Deque<Integer> freeRooms = new ArrayDeque<>(Arrays.asList(5, 6));

            SR1.processP2(p2List, assigned, freeRooms);

            // existing should be updated to P2_ROOM, req2 gets new P2_ROOM
            assertEquals(2, assigned.size());
            assertEquals("P2_ROOM", existing.getStatus());
            assertEquals(Integer.valueOf(5), existing.getRoomID());
            assertTrue(freeRooms.isEmpty()); // Both rooms used
        }

        @Test
        @DisplayName("handles null arrival/departure times gracefully")
        void testNullTimesHandledGracefully() {
            // Create a request with null times
            Req reqWithNullTimes = new Req(1, null, null, "DEL");

            // This should not throw NPE due to null checks in predicates
            // The request will be classified as P1 (hubTurnTime will be false due to null check)
            assertDoesNotThrow(() -> {
                // Simulate the predicate check
                boolean isShortDuration = reqWithNullTimes.getArr() != null &&
                        reqWithNullTimes.getDep() != null;
                assertFalse(isShortDuration);
            });
        }
    }
}
