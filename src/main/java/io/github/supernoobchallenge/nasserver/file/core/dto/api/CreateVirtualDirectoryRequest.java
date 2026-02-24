package io.github.supernoobchallenge.nasserver.file.core.dto.api;

public record CreateVirtualDirectoryRequest(
        Long parentDirectoryId,
        String name,
        Integer readLevel,
        Integer writeLevel
) {
}
