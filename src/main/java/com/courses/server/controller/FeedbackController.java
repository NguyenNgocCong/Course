package com.courses.server.controller;

import com.courses.server.dto.MessageResponse;
import com.courses.server.dto.request.FeedbackRequest;
import com.courses.server.dto.response.FeedbackDTO;
import com.courses.server.entity.Feedback;
import com.courses.server.services.FeedbackService;
import com.courses.server.utils.Authen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody FeedbackRequest req) {
        Authen.check();
        feedbackService.create(req);

        return ResponseEntity.ok(new MessageResponse("Create feedback is success"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestParam("id") Long id,
            @RequestBody FeedbackRequest req) {
        Authen.check();
        feedbackService.update(id, req);

        return ResponseEntity.ok(new MessageResponse("Update feedback is success"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam("id") Long id) {
        Authen.check();
        feedbackService.delete(id);

        return ResponseEntity.ok(new MessageResponse("Delete feedback is success"));
    }

    @GetMapping("/list-expert")
    public ResponseEntity<?> listExpert(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") long expertId,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(1, expertId, pageable);

        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-package")
    public ResponseEntity<?> listPackage(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") long packageId,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(2, packageId, pageable);
        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-combo")
    public ResponseEntity<?> listCombo(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") long comboId,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(3, comboId, pageable);
        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-class")
    public ResponseEntity<?> listClass(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") long classId,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(4, classId, pageable);
        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-blog")
    public ResponseEntity<?> listBlog(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") long BlogId,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(5, BlogId, pageable);
        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-website")
    public ResponseEntity<?> listWebsite(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> pageFeedback = feedbackService.getList(0, 0, pageable);
        List<FeedbackDTO> feedbackDTOS = pageFeedback.getContent().stream().map(f -> new FeedbackDTO(f))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", feedbackDTOS);
        response.put("currentPage", pageFeedback.getNumber());
        response.put("totalItems", pageFeedback.getTotalElements());
        response.put("totalPages", pageFeedback.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
