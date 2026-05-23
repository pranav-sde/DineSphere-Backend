package com.festora.hotelservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddFloorRequest {
    private String floorLabel;          // "Ground Floor", "Floor 1"
    private int floorNumber;            // 0, 1, 2...
    private String roomPrefix;
    private Integer startRoom;
    private Integer endRoom;
    private List<String> roomNumbers;   // ["101","102","103"] or generated from range
}
