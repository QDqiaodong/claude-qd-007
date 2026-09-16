package com.nursing.home.controller;

import com.nursing.home.entity.Medicine;
import com.nursing.home.entity.MedicineIssue;
import com.nursing.home.service.MedicineService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MedicineController {

    private final MedicineService service;

    public MedicineController(MedicineService service) {
        this.service = service;
    }

    @GetMapping("/medicines")
    public List<Medicine> listMedicines(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String kind,
                                        @RequestParam(required = false) String keyword) {
        return service.listMedicines(status, kind, keyword);
    }

    @PostMapping("/medicines")
    public Medicine createMedicine(@RequestBody Medicine input) {
        return service.createMedicine(input);
    }

    @PutMapping("/medicines/{id}")
    public Medicine updateMedicine(@PathVariable Long id, @RequestBody Medicine input) {
        return service.updateMedicine(id, input);
    }

    @GetMapping("/medicine-issues")
    public List<MedicineIssue> listIssues(
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.listIssues(residentId, kind, date);
    }

    @PostMapping("/medicine-issues")
    public MedicineIssue issue(@RequestBody MedicineIssue input) {
        return service.issue(input);
    }
}
