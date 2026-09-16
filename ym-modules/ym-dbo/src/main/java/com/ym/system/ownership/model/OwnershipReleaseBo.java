package com.ym.system.ownership.model;
import jakarta.validation.constraints.*;
public record OwnershipReleaseBo(@NotNull @Min(0) Long expectedAssignmentVersion,
    @NotBlank @Size(max=500) String reason) { }
