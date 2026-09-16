package com.nursing.home.controller;

import com.nursing.home.entity.CareShift;
import com.nursing.home.service.CareShiftService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shifts")
public class CareShiftController {

    private final CareShiftService service;

    public CareShiftController(CareShiftService service) {
        this.service = service;
    }

    @GetMapping
    public List<CareShift> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String nurse) {
        return service.list(date, status, roomId, nurse);
    }

    @PostMapping
    public CareShift open(@RequestBody CareShift input) {
        return service.open(input);
    }

    @PostMapping("/{id}/advance")
    public CareShift advance(@PathVariable Long id,
                             @RequestParam String action,
                             @RequestParam(required = false) String handoverNote) {
        return service.advance(id, action, handoverNote);
    }
}
