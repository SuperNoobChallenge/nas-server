package io.github.supernoobchallenge.nasserver.file.core.dto;

public record VirtualDirectoryChildResponse(
        Long directoryId,
        Long parentDirectoryId,
        String name,
        int readLevel,
        int writeLevel,
        int depthLevel
) {
}
