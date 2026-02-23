package io.github.supernoobchallenge.nasserver.file.core.dto;

import java.util.List;

public record VirtualDirectoryTreeResponse(
        Long directoryId,
        Long parentDirectoryId,
        String name,
        int readLevel,
        int writeLevel,
        int depthLevel,
        List<VirtualDirectoryTreeResponse> children
) {
}
