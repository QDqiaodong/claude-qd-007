package com.nursing.home.controller;

import com.nursing.home.dto.TakeoutMedicineRequest;
import com.nursing.home.entity.MedicalEscort;
import com.nursing.home.entity.MedicineIssue;
import com.nursing.home.service.MedicalEscortService;
import com.nursing.home.service.MedicineService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/medical-escorts", "/api/medical-escort"})
public class MedicalEscortController {

    private final MedicalEscortService escortService;
    private final MedicineService medicineService;

    public MedicalEscortController(MedicalEscortService escortService, MedicineService medicineService) {
        this.escortService = escortService;
        this.medicineService = medicineService;
    }

    @GetMapping
    public List<MedicalEscort> list(@RequestParam(required = false) Long residentId,
                                    @RequestParam(required = false) String status) {
        return escortService.list(residentId, status);
    }

    @PostMapping
    public MedicalEscort create(@RequestBody MedicalEscort input) {
        return escortService.create(input);
    }

    @PostMapping({"/{id}/close", "/{id}/complete"})
    public MedicalEscort close(@PathVariable Long id,
                               @RequestParam(required = false) String action,
                               @RequestParam(required = false) String confirmer,
                               @RequestBody(required = false) CloseRequest body) {
        String useAction = action != null ? action : (body == null ? null : body.action);
        String useConfirmer = confirmer != null ? confirmer : (body == null ? null : body.confirmer);
        return escortService.close(id, useAction, useConfirmer);
    }

    @PostMapping({"/{id}/takeout-medicines", "/{id}/takeout-medicine", "/{id}/medicines/takeout"})
    public List<MedicineIssue> addTakeoutMedicines(@PathVariable Long id,
                                                   @RequestBody(required = false) TakeoutMedicineRequest request) {
        return medicineService.issueTakeout(id, request);
    }

    public static class CloseRequest {
        public String action;
        public String confirmer;
    }
}
