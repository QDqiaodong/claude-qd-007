package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.dto.TakeoutMedicineRequest;
import com.nursing.home.entity.MedicalEscort;
import com.nursing.home.entity.Medicine;
import com.nursing.home.entity.MedicineIssue;
import com.nursing.home.entity.Resident;
import com.nursing.home.repository.MedicineIssueRepository;
import com.nursing.home.repository.MedicineRepository;
import com.nursing.home.repository.ResidentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineService {

    private static final List<String> DOSE = List.of("早", "中", "晚");

    private final MedicineRepository medicines;
    private final MedicineIssueRepository issues;
    private final ResidentRepository residents;
    private final MedicalEscortService escorts;

    public MedicineService(MedicineRepository medicines, MedicineIssueRepository issues,
                           ResidentRepository residents, MedicalEscortService escorts) {
        this.medicines = medicines;
        this.issues = issues;
        this.residents = residents;
        this.escorts = escorts;
    }

    public List<Medicine> listMedicines(String status, String kind, String keyword) {
        return medicines.findAll().stream()
                .filter(m -> status == null || status.isEmpty() || status.equals(m.status))
                .filter(m -> kind == null || kind.isEmpty() || kind.equals(m.kind))
                .filter(m -> keyword == null || keyword.isEmpty()
                        || m.name.contains(keyword) || m.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Medicine createMedicine(Medicine input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("药品编号不能为空");
        }
        if (medicines.existsByCode(input.code)) {
            throw new BizException("药品编号 " + input.code + " 已经存在");
        }
        Medicine saved = new Medicine();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.unit = (input.unit == null || input.unit.isBlank()) ? "片" : input.unit;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "非处方" : input.kind;
        saved.stock = (input.stock == null || input.stock < 0) ? 0 : input.stock;
        saved.warnStock = (input.warnStock == null || input.warnStock < 0) ? 0 : input.warnStock;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return medicines.save(saved);
    }

    @Transactional
    public Medicine updateMedicine(Long id, Medicine input) {
        Medicine m = medicines.findById(id).orElseThrow(() -> new BizException("药品不存在"));
        if (input.name != null) {
            m.name = input.name;
        }
        if (input.unit != null && !input.unit.isBlank()) {
            m.unit = input.unit;
        }
        if (input.kind != null && !input.kind.isBlank()) {
            m.kind = input.kind;
        }
        if (input.stock != null && input.stock >= 0) {
            m.stock = input.stock;
        }
        if (input.warnStock != null && input.warnStock >= 0) {
            m.warnStock = input.warnStock;
        }
        if (input.status != null && !input.status.isBlank()) {
            m.status = input.status;
        }
        return medicines.save(m);
    }

    public List<MedicineIssue> listIssues(Long residentId, String kind, LocalDate date) {
        List<MedicineIssue> all = (residentId == null)
                ? issues.findAllByOrderByCreatedAtDesc()
                : issues.findByResidentIdOrderByCreatedAtAsc(residentId);
        return all.stream()
                .filter(i -> kind == null || kind.isEmpty() || kind.equals(i.kind))
                .filter(i -> date == null || date.equals(i.issueDate))
                .toList();
    }

    /** 该老人在这种药上净发出去多少（发放 - 退回）。 */
    private int netIssued(Long residentId, Long medicineId) {
        int net = 0;
        for (MedicineIssue i : issues.findByResidentIdOrderByCreatedAtAsc(residentId)) {
            if (!medicineId.equals(i.medicineId)) {
                continue;
            }
            net += "退回".equals(i.kind) ? -i.qty : i.qty;
        }
        return net;
    }

    @Transactional
    public MedicineIssue issue(MedicineIssue input) {
        if (input.residentId == null) {
            throw new BizException("请选一位老人");
        }
        if (input.medicineId == null) {
            throw new BizException("请选一种药");
        }
        if (input.qty == null || input.qty <= 0) {
            throw new BizException("数量要大于 0");
        }
        String dose = (input.doseTime == null || input.doseTime.isBlank()) ? "早" : input.doseTime.trim();
        if (!DOSE.contains(dose)) {
            throw new BizException("剂次只能是早、中、晚");
        }
        String kind = (input.kind == null || input.kind.isBlank()) ? "发放" : input.kind.trim();
        if (!"发放".equals(kind) && !"退回".equals(kind) && !"外带".equals(kind)) {
            throw new BizException("只支持发放、退回和外带三种");
        }
        if ("外带".equals(kind)) {
            throw new BizException("外带药品必须在外出就医护送单上勾选，请走外带发药");
        }
        LocalDate date = input.issueDate == null ? LocalDate.now() : input.issueDate;

        Resident r = residents.findById(input.residentId)
                .orElseThrow(() -> new BizException("老人档案不存在"));
        Medicine m = medicines.findById(input.medicineId)
                .orElseThrow(() -> new BizException("药品不存在"));

        if ("发放".equals(kind)) {
            if ("已退住".equals(r.status)) {
                throw new BizException("老人 " + r.name + " 已经退住，不能再发药了");
            }
            MedicalEscort activeEscort = escorts.findActiveForMedicine(r.id);
            if (activeEscort != null) {
                throw new BizException("老人 " + r.name + " 的护送单 " + activeEscort.escortNo
                        + " 还没销，发药台不能做在房早中晚常规发放，外出用药只能勾成外带");
            }
            if (!"在用".equals(m.status)) {
                throw new BizException("药品 " + m.name + " 已经停用，不能发放");
            }
            if (m.stock < input.qty) {
                throw new BizException("药品 " + m.name + " 库存只剩 " + m.stock + " " + m.unit
                        + "，发不了 " + input.qty + " " + m.unit);
            }
            if (!issues.findByResidentIdAndMedicineIdAndIssueDateAndDoseTimeAndKind(
                    r.id, m.id, date, dose, "发放").isEmpty()) {
                throw new BizException("老人 " + r.name + " 在 " + date + " 的" + dose
                        + "剂已经发过 " + m.name + " 了，别重复发");
            }
            m.stock -= input.qty;
        } else {
            int net = netIssued(r.id, m.id);
            if (input.qty > net) {
                throw new BizException("老人 " + r.name + " 在 " + m.name + " 上只净发了 " + net
                        + " " + m.unit + "，退不了 " + input.qty + " " + m.unit);
            }
            m.stock += input.qty;
        }
        medicines.save(m);

        MedicineIssue saved = new MedicineIssue();
        saved.residentId = r.id;
        saved.medicineId = m.id;
        saved.qty = input.qty;
        saved.kind = kind;
        saved.doseTime = dose;
        saved.issueDate = date;
        saved.operator = input.operator;
        saved.escortId = null;
        saved.createdAt = LocalDateTime.now();
        return issues.save(saved);
    }

    @Transactional
    public List<MedicineIssue> issueTakeout(Long escortId, TakeoutMedicineRequest request) {
        MedicalEscort escort = escorts.getOpenForTakeout(escortId);
        if (request == null) {
            throw new BizException("请勾选要外带的药品");
        }
        List<TakeoutMedicineRequest.TakeoutItem> rawItems = new ArrayList<>();
        if (request.items != null && !request.items.isEmpty()) {
            rawItems.addAll(request.items);
        } else {
            TakeoutMedicineRequest.TakeoutItem one = new TakeoutMedicineRequest.TakeoutItem();
            one.medicineId = request.medicineId;
            one.qty = request.qty;
            one.doseTime = request.doseTime;
            one.operator = request.operator;
            rawItems.add(one);
        }
        if (rawItems.isEmpty()) {
            throw new BizException("请勾选至少一种外带药品");
        }

        record PreparedItem(Medicine medicine, int qty, String dose, String operator) {
        }
        List<PreparedItem> prepared = new ArrayList<>();
        Map<Long, Integer> stockNeed = new HashMap<>();
        Set<String> batchDoses = new HashSet<>();
        LocalDate date = LocalDate.now();
        Resident resident = residents.findById(escort.residentId)
                .orElseThrow(() -> new BizException("老人档案不存在"));

        for (TakeoutMedicineRequest.TakeoutItem item : rawItems) {
            if (item == null || item.medicineId == null) {
                throw new BizException("请选择要外带的药品");
            }
            if (item.qty == null || item.qty <= 0) {
                throw new BizException("外带数量要大于 0");
            }
            String dose = (item.doseTime == null || item.doseTime.isBlank()) ? "早" : item.doseTime.trim();
            if (!DOSE.contains(dose)) {
                throw new BizException("剂次只能是早、中、晚");
            }
            String doseKey = item.medicineId + "#" + dose;
            if (!batchDoses.add(doseKey)) {
                throw new BizException("同一护送单同一种药的同一剂次只能勾一次外带");
            }
            Medicine medicine = medicines.findById(item.medicineId)
                    .orElseThrow(() -> new BizException("药品不存在"));
            if (!"在用".equals(medicine.status)) {
                throw new BizException("药品 " + medicine.name + " 已经停用，不能外带");
            }
            if (!issues.findByResidentIdAndMedicineIdAndIssueDateAndDoseTimeAndKind(
                    resident.id, medicine.id, date, dose, "外带").isEmpty()) {
                throw new BizException("老人 " + resident.name + " 今天的" + dose + "剂已经勾过 "
                        + medicine.name + " 外带了");
            }
            String operator = (item.operator == null || item.operator.isBlank()) ? request.operator : item.operator;
            prepared.add(new PreparedItem(medicine, item.qty, dose, operator));
            stockNeed.merge(medicine.id, item.qty, Integer::sum);
        }

        // 先把整批库存和重复项都核完，再统一扣库存；任何一项失败整批回滚。
        Map<Long, Medicine> medicineMap = new HashMap<>();
        for (PreparedItem item : prepared) {
            medicineMap.put(item.medicine().id, item.medicine());
        }
        for (Map.Entry<Long, Integer> need : stockNeed.entrySet()) {
            Medicine medicine = medicineMap.get(need.getKey());
            if (medicine.stock < need.getValue()) {
                throw new BizException("药品 " + medicine.name + " 库存只剩 " + medicine.stock + " "
                        + medicine.unit + "，外带不了 " + need.getValue() + " " + medicine.unit);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<MedicineIssue> savedIssues = new ArrayList<>();
        for (PreparedItem item : prepared) {
            Medicine medicine = item.medicine();
            medicine.stock -= item.qty();
            medicines.save(medicine);

            MedicineIssue saved = new MedicineIssue();
            saved.residentId = resident.id;
            saved.medicineId = medicine.id;
            saved.qty = item.qty();
            saved.kind = "外带";
            saved.doseTime = item.dose();
            saved.issueDate = date;
            saved.operator = item.operator();
            saved.escortId = escort.id;
            saved.createdAt = now;
            savedIssues.add(issues.save(saved));
        }
        return savedIssues;
    }
}
