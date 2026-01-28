package com.ts.sr;

public class Assignment {

    String status;
    Integer requestID;
    Integer roomID;

    /**
     * Creates a new Assignment.
     * @param status Assignment status (P1_ASSIGNED, P2_ROOM, WAITLIST)
     * @param requestID The request ID
     * @param roomID The room ID (null if waitlisted)
     */
    public Assignment(String status, Integer requestID, Integer roomID) {
        this.status = status;
        this.requestID = requestID;
        this.roomID = roomID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRequestID() {
        return requestID;
    }

    public void setRequestID(Integer requestID) {
        this.requestID = requestID;
    }

    public Integer getRoomID() {
        return roomID;
    }

    public void setRoomID(Integer roomID) {
        this.roomID = roomID;
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "status='" + status + '\'' +
                ", requestID=" + requestID +
                ", roomID=" + roomID +
                '}';
    }
}
