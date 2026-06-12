package com.paradissaveurs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockPatchRequest(
        @NotNull @Min(0) Integer stock
) {}
