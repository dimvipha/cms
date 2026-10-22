package com.vipha.cms.common.dto;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** A rendered PDF and the file name it should be served under. */
public record PdfFile(
        String fileName,
        byte[] content
) {

    /** 200 response that opens in the browser's PDF viewer rather than downloading. */
    public ResponseEntity<byte[]> toInlineResponse() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName).build().toString())
                .body(content);
    }
}
