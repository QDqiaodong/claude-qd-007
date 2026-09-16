package com.nursing.home.controller;

import com.nursing.home.entity.Resident;
import com.nursing.home.service.ResidentService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private final ResidentService service;

    public ResidentController(ResidentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Resident> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String careLevel,
                               @RequestParam(required = false) String keyword) {
        return service.list(status, careLevel, keyword);
    }

    @PostMapping
    public Resident create(@RequestBody Resident input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public Resident update(@PathVariable Long id, @RequestBody Resident input) {
        return service.update(id, input);
    }
}
