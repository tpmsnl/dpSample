package com.ts.sr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nonnull;
import org.slf4j.*;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SR1 {

    private static final int ROOMS = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(SR1.class);
    private static final ObjectMapper om = createObjectMapper();

    private static final String WAITLIST = "WAITLIST";
    private static final String P1_ASSIGNED = "P1_ASSIGNED";
    private static final String P2_ROOM = "P2_ROOM";

    @Nonnull
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static void main(String[] args) throws IOException {
        List<Req> requests = getRequests();

        // 1. Define Predicates (with null safety)
        final Predicate<Req> hubTurnTime = req ->
                req.getArr() != null && req.getDep() != null &&
                Duration.between(req.getArr(), req.getDep()).toMinutes() <= 120;
        final Predicate<Req> hubTurn = req ->
                req.getOrigin() == null || req.getOrigin().trim().isEmpty();
        final Predicate<Req> isP2 = hubTurn.or(hubTurnTime);

        // 2. Efficiently split P1 and P2 in one pass (Fixes inefficient filtering)
        Map<Boolean, List<Req>> splitReqs = requests.stream()
                .collect(Collectors.partitioningBy(isP2));

        List<Req> p2Requests = splitReqs.get(true);
        List<Req> p1Requests = splitReqs.get(false);

        // 3. Set Priorities
        p2Requests.forEach(x -> x.setPrior(2));
        p1Requests.forEach(x -> x.setPrior(1));

        LOGGER.info("P1 Size: {}, P2 Size: {}", p1Requests.size(), p2Requests.size());

        // 4. Initialize Rooms
        Deque<Integer> freeRooms = new ArrayDeque<>();
        for (int i = 1; i <= ROOMS; i++) {
            freeRooms.add(i);
        }

        // 5. Assign P1
        Set<Assignment> assigned = assignP1(p1Requests, freeRooms);

        // 6. Handle P1 Rejections (Downgrade to P2)
        // This returns the P1 requests that were rejected so they can be added to the P2 queue
        List<Req> rejectedP1s = processP1Rejections(p1Requests, assigned, freeRooms);

        // 7. Merge original P2s with Rejected P1s (Fixes "Lost P2" bug)
        Deque<Req> finalP2Queue = new ArrayDeque<>(p2Requests);
        finalP2Queue.addAll(rejectedP1s); // Append rejected P1s to the end of P2 line

        // 8. Process P2
        processP2(finalP2Queue, assigned, freeRooms);

        // Output results
        LOGGER.info("Final Assignments:");
        assigned.forEach(x -> LOGGER.info(x.toString()));
    }

    public static Set<Assignment> assignP1(List<Req> requests, Deque<Integer> freeRooms) {
        Set<Assignment> assigned = new LinkedHashSet<>();

        for (Req r : requests) {
            if (!freeRooms.isEmpty()) {
                int room = freeRooms.removeFirst();
                // FIX: Standardized Constructor order (Status, ReqID, RoomID)
                assigned.add(new Assignment(P1_ASSIGNED, r.getId(), room));
            } else {
                assigned.add(new Assignment(WAITLIST, r.getId(), null));
            }
        }
        return assigned;
    }

    /**
     * Identifies P1 assignments that must be rejected/revoked based on business logic.
     * Frees up the rooms and returns the requests to be re-processed as P2.
     */
    public static List<Req> processP1Rejections(List<Req> requests, Set<Assignment> assigned, Deque<Integer> freeRooms) {
        List<Req> downgradedRequests = new ArrayList<>();

        // FIX: Removed freeRooms.clear(). We want to KEEP rooms that weren't used by P1.

        Map<Integer, Req> rejectedReqMap = requests.stream()
                .filter(Req::isRejectP1) // Assuming isRejectP1 is a boolean flag in Req
                .collect(Collectors.toMap(Req::getId, r -> r, (a, b) -> a));

        for (Assignment a : assigned) {
            if (P1_ASSIGNED.equals(a.getStatus())) {
                Req rejected = rejectedReqMap.get(a.getRequestID());

                if (rejected != null) {
                    // Revoke the room
                    freeRooms.add(a.getRoomID());
                    // Update status to waitlist (temporarily)
                    a.setStatus(WAITLIST);
                    a.setRoomID(null);
                    // Add to list for P2 processing
                    downgradedRequests.add(rejected);
                }
            }
        }
        return downgradedRequests;
    }

    public static void processP2(Deque<Req> p2List, Set<Assignment> assigned, Deque<Integer> freeRooms) {
        // Map for fast lookup of existing assignments
        Map<Integer, Assignment> assignmentByReqId = assigned.stream()
                .collect(Collectors.toMap(Assignment::getRequestID, a -> a, (a, b) -> a));

        // FIX: Removed the double-list initialization bug.
        // We iterate through the p2List directly.

        for (Req r : p2List) {
            Assignment existing = assignmentByReqId.get(r.getId());

            // Skip if already assigned a room (unless it was just revoked in previous step, which is handled by status check)
            boolean isAlreadyAssigned = existing != null && !WAITLIST.equals(existing.getStatus());
            if (isAlreadyAssigned) {
                continue;
            }

            if (!freeRooms.isEmpty()) {
                int room = freeRooms.removeFirst();

                if (existing != null) {
                    // Update existing WAITLIST assignment
                    existing.setStatus(P2_ROOM);
                    existing.setRoomID(room);
                } else {
                    // Create new assignment
                    Assignment newAssignment = new Assignment(P2_ROOM, r.getId(), room);
                    assigned.add(newAssignment);
                    assignmentByReqId.put(r.getId(), newAssignment);
                }
            } else {
                // No rooms left
                if (existing == null) {
                    Assignment newAssignment = new Assignment(WAITLIST, r.getId(), null);
                    assigned.add(newAssignment);
                    assignmentByReqId.put(r.getId(), newAssignment);
                }
                // If existing was WAITLIST, it stays WAITLIST
            }
        }
    }

    static List<Req> getRequests() throws IOException {
        ClassPathResource resource = new ClassPathResource("sample.json");
        try (InputStream is = resource.getInputStream()) {
            return om.readValue(is, new TypeReference<>() {});
        }
    }
}
