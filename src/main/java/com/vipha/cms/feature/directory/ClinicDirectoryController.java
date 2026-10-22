package com.vipha.cms.feature.directory;

import com.vipha.cms.feature.directory.dto.ClinicDirectoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Open to everyone, signed in or not: what the clinic's landing page lists. */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class ClinicDirectoryController {

    private final ClinicDirectoryService directoryService;

    @GetMapping("/clinic")
    public ClinicDirectoryResponse clinic() {
        return directoryService.directory();
    }
}
