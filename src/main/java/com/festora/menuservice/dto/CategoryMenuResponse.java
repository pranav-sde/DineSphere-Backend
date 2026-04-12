package com.festora.menuservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryMenuResponse {

    private Long restaurantId;
    private List<CategoryDto> categories;
}
