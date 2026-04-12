package com.festora.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemPageResponse {
    private List<MenuItemDto> items;
    private long totalElements;
}
