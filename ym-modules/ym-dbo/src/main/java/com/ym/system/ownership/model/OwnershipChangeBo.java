package com.ym.system.ownership.model;
import jakarta.validation.constraints.*;
public record OwnershipChangeBo(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,20}") String targetTenantId,
    @NotNull @Min(0) Long expectedAssignmentVersion, @NotBlank @Size(max=500) String reason) { }
