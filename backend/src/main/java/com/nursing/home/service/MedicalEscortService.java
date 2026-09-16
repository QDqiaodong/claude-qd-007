package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.entity.Bed;
import com.nursing.home.entity.CareShift;
import com.nursing.home.entity.MedicalEscort;
import com.nursing.home.entity.Resident;
import com.nursing.home.repository.BedRepository;
import com.nursing.home.repository.CareShiftRepository;
import com.nursing.home.repository.MedicalEscortRepository;
import com.nursing.home.repository.ResidentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalEscortService {

    public static final String ACTIVE = "护送中";
    public static final String OVERDUE = "滞留";
    public static final String CLOSED = "已销单";
    public static final List<String> OPEN_STATUSES = List.of(ACTIVE, OVERDUE);
    private static final List<String> NEED_NURSE_CARE_LEVELS = List.of("半自理", "不能自理");
    private static final List<String> PAST_SHIFT_STATUSES = List.of("已交班", "已取消");

    private final MedicalEscortRepository escorts;
    private final ResidentRepository residents;
    private final BedRepository beds;
    private final CareShiftRepository shifts;

    public MedicalEscortService(MedicalEscortRepository escorts, ResidentRepository residents,
                                BedRepository beds, CareShiftRepository shifts) {
        this.escorts = escorts;
        this.residents = residents;
        this.beds = beds;
        this.shifts = shifts;
    }

    @Transactional
    public List<MedicalEscort> list(Long residentId, String status) {
        touchOverdue();
        return escorts.findAllByOrderByCreatedAtDesc().stream()
                .filter(e -> residentId == null || residentId.equals(e.residentId))
                .filter(e -> status == null || status.isBlank() || status.equals(e.status))
                .toList();
    }

    /** 超过预计返回时间但还没销单的单子改成滞留；不改动档案和床位。 */
    @Transactional
    public void touchOverdue() {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (MedicalEscort escort : escorts.findByStatusInOrderByExpectedReturnAtAsc(List.of(ACTIVE))) {
            if (escort.expectedReturnAt != null && escort.expectedReturnAt.isBefore(now)) {
                escort.status = OVERDUE;
                escort.updatedAt = now;
                escorts.save(escort);
                changed = true;
            }
        }
        if (changed) {
            escorts.flush();
        }
    }

    @Transactional
    public MedicalEscort findActiveForMedicine(Long residentId) {
        touchOverdue();
        return escorts.findFirstByResidentIdAndStatusInOrderByCreatedAtDesc(residentId, OPEN_STATUSES)
                .orElse(null);
    }

    @Transactional
    public MedicalEscort getOpenForTakeout(Long id) {
        touchOverdue();
        MedicalEscort escort = escorts.findById(id)
                .orElseThrow(() -> new BizException("护送单不存在"));
        if (CLOSED.equals(escort.status)) {
            throw new BizException("护送单 " + escort.escortNo + " 已经销掉，不能再勾外带药");
        }
        if (OVERDUE.equals(escort.status)) {
            throw new BizException("护送单 " + escort.escortNo + " 已超过预计返回时间并标记滞留，外带发药停止");
        }
        return escort;
    }

    private String nextEscortNo() {
        long n = escorts.count() + 1;
        String no;
        do {
            no = "ME-" + String.format("%04d", n++);
        } while (escorts.existsByEscortNo(no));
        return no;
    }

    private CareShift currentDutyShift(Long roomId) {
        LocalDate today = LocalDateTime.now().toLocalDate();
        return shifts.findByRoomIdAndShiftDateAndStatusNotIn(roomId, today, PAST_SHIFT_STATUSES)
                .stream()
                .filter(s -> "值班中".equals(s.status))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public MedicalEscort create(MedicalEscort input) {
        touchOverdue();
        if (input.residentId == null) {
            throw new BizException("请选择外出就医的老人");
        }
        if (input.hospitalName == null || input.hospitalName.isBlank()) {
            throw new BizException("医院名称必须填写");
        }
        if (input.expectedLeaveAt == null) {
            throw new BizException("预计离院时刻必须填写");
        }
        if (input.expectedReturnAt == null) {
            throw new BizException("预计返回时刻必须填写");
        }
        if (input.expectedReturnAt.isBefore(LocalDateTime.now())) {
            throw new BizException("预计返回时刻不能早于当前时间");
        }
        if (!input.expectedReturnAt.isAfter(input.expectedLeaveAt)) {
            throw new BizException("预计返回时刻必须晚于预计离院时刻");
        }
        if (input.escortName == null || input.escortName.isBlank()) {
            throw new BizException("护送人必须填写");
        }

        Resident resident = residents.findById(input.residentId)
                .orElseThrow(() -> new BizException("老人档案不存在"));
        if ("已退住".equals(resident.status)) {
            throw new BizException("老人 " + resident.name + " 已退住，不能开外出就医护送单");
        }
        if (!"在住".equals(resident.status) && !"请假外出".equals(resident.status)) {
            throw new BizException("老人 " + resident.name + " 当前是" + resident.status + "，不能开护送单");
        }

        MedicalEscort existing = escorts
                .findFirstByResidentIdAndStatusInOrderByCreatedAtDesc(resident.id, OPEN_STATUSES)
                .orElse(null);
        if (existing != null) {
            throw new BizException("已有未结束的护送单 " + existing.escortNo
                    + "：医院 " + existing.hospitalName + "，预计离院 " + existing.expectedLeaveAt
                    + "，先销单后才能再开");
        }

        if (resident.roomId == null || resident.bedId == null) {
            throw new BizException("老人 " + resident.name + " 还没有安排房间床位，不能开护送单");
        }
        Bed bed = beds.findById(resident.bedId).orElseThrow(() -> new BizException("老人的床位不存在"));
        if (!"占用".equals(bed.status)) {
            throw new BizException("床位 " + bed.code + " 不是占用状态，先补齐床位台账");
        }
        if (!resident.roomId.equals(bed.roomId)) {
            throw new BizException("档案床位和所属房间对不上，先处理床位台账");
        }

        String escortName = input.escortName.trim();
        CareShift duty = currentDutyShift(resident.roomId);
        String escortType;
        if (NEED_NURSE_CARE_LEVELS.contains(resident.careLevel)) {
            if (duty == null) {
                throw new BizException(resident.careLevel + "老人外出必须由本房值班中的护理员护送，"
                        + "本房此刻没有值班中的班次，护送单开不成");
            }
            if (!duty.nurse.trim().equals(escortName)) {
                throw new BizException(resident.careLevel + "老人不能由家属护送，护送人必须是本房此刻值班中的护理员 "
                        + duty.nurse);
            }
            escortType = "护理员";
        } else if (duty != null && duty.nurse.trim().equals(escortName)) {
            escortType = "护理员";
        } else {
            escortType = "家属";
        }

        LocalDateTime now = LocalDateTime.now();
        MedicalEscort saved = new MedicalEscort();
        saved.escortNo = nextEscortNo();
        saved.residentId = resident.id;
        saved.hospitalName = input.hospitalName.trim();
        saved.expectedLeaveAt = input.expectedLeaveAt;
        saved.expectedReturnAt = input.expectedReturnAt;
        saved.escortName = escortName;
        saved.escortType = escortType;
        saved.status = saved.expectedReturnAt.isBefore(now) ? OVERDUE : ACTIVE;
        saved.createdAt = now;
        saved.updatedAt = now;
        escorts.save(saved);

        // 档案和床位在同一事务里落账：任一步失败都不会留下“人走了、单没成”的半成品。
        resident.status = "请假外出";
        resident.updatedAt = now;
        residents.save(resident);
        bed.status = "占用";
        beds.save(bed);
        return saved;
    }

    @Transactional
    public MedicalEscort close(Long id, String action, String confirmer) {
        MedicalEscort escort = escorts.findById(id)
                .orElseThrow(() -> new BizException("护送单不存在"));
        touchOverdue();
        if (CLOSED.equals(escort.status)) {
            throw new BizException("护送单 " + escort.escortNo + " 已经销掉了");
        }
        String closeAction = switch (action == null ? "" : action.trim()) {
            case "return", "returned", "confirm-return", "人已回院" -> "人已回院";
            case "takeover", "confirm-takeover", "接手护送" -> "接手护送";
            default -> throw new BizException("销单动作只能是确认人已回院或确认接手护送");
        };
        if (confirmer == null || confirmer.isBlank()) {
            throw new BizException("必须由本房此刻值班中的护理员确认销单");
        }
        Resident resident = residents.findById(escort.residentId)
                .orElseThrow(() -> new BizException("老人档案不存在"));
        if (resident.roomId == null || resident.bedId == null) {
            throw new BizException("老人档案缺少房间床位信息，无法核对销单");
        }
        Bed bed = beds.findById(resident.bedId).orElseThrow(() -> new BizException("老人的床位不存在"));
        if (!resident.roomId.equals(bed.roomId)) {
            throw new BizException("档案床位和所属房间对不上，不能销单");
        }
        CareShift duty = currentDutyShift(resident.roomId);
        String name = confirmer.trim();
        if (duty == null || !duty.nurse.trim().equals(name)) {
            throw new BizException("销单必须由本房此刻值班中的护理员确认；当前本房值班护理员为 "
                    + (duty == null ? "无" : duty.nurse));
        }

        LocalDateTime now = LocalDateTime.now();
        escort.status = CLOSED;
        escort.closedAt = now;
        escort.closeAction = closeAction;
        escort.confirmer = name;
        escort.updatedAt = now;
        escorts.save(escort);

        resident.status = "在住";
        resident.updatedAt = now;
        residents.save(resident);
        bed.status = "占用";
        beds.save(bed);
        return escort;
    }
}
