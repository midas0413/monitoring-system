package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.dto.CheckRunCreateRequest;
import com.example.monitoring.web.service.CheckRunService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/check-runs")
public class CheckRunController {

    private final CheckRunService checkRunService;

    public CheckRunController(CheckRunService checkRunService) {
        this.checkRunService = checkRunService;
    }

    // POST /api/check-runs
    @PostMapping
    public CheckRunEntity create(@RequestBody CheckRunCreateRequest req) {
        return checkRunService.create(req);
    }

    // GET /api/check-runs/{id}
    @GetMapping("/{id}")
    public CheckRunEntity get(@PathVariable Long id) {
        return checkRunService.get(id);
    }

    @GetMapping
    public List<CheckRunEntity> list(@RequestParam(required = false) Long checkId) {
        return checkRunService.list(checkId, null);
    }
}