package com.workoutsmart.exercise.controller;

import com.workoutsmart.exercise.dto.MediaUploadResponse;
import com.workoutsmart.exercise.service.MediaUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload media cho bài tập cá nhân — FR-008 (011). */
@RestController
@RequestMapping("/api/v1/media")
public class MediaUploadController {

    private final MediaUploadService mediaUploadService;

    public MediaUploadController(MediaUploadService mediaUploadService) {
        this.mediaUploadService = mediaUploadService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new MediaUploadResponse(mediaUploadService.store(file));
    }
}
