package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.entity.CareShift;
import com.nursing.home.entity.Room;
import com.nursing.home.repository.CareShiftRepository;
import com.nursing.home.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareShiftService {

    /** 待接班和值班中仍占着房间与护理员；已交班、已取消都释放占用。 */
    public static final List<String> OPEN = List.of("待接班", "值班中");

    private final CareShiftRepository shifts;
    private final RoomRepository rooms;

    public CareShiftService(CareShiftRepository shifts, RoomRepository rooms) {
        this.shifts = shifts;
        this.rooms = rooms;
    }

    public List<CareShift> list(LocalDate date, String status, Long roomId, String nurse) {
        return shifts.findAllByOrderByUpdatedAtDesc().stream()
                .filter(s -> date == null || date.equals(s.shiftDate))
                .filter(s -> status == null || status.isEmpty() || status.equals(s.status))
                .filter(s -> roomId == null || roomId.equals(s.roomId))
                .filter(s -> nurse == null || nurse.isEmpty() || nurse.equals(s.nurse))
                .toList();
    }

    private String nextShiftNo() {
        long n = shifts.count() + 1;
        String no;
        do {
            no = "HS-" + String.format("%04d", n++);
        } while (shifts.existsByShiftNo(no));
        return no;
    }

    private boolean overlap(CareShift other, CareShift input) {
        if (other.startMin == null || other.endMin == null) {
            return false;
        }
        return other.startMin < input.endMin && input.startMin < other.endMin;
    }

    @Transactional
    public CareShift open(CareShift input) {
        if (input.roomId == null) {
            throw new BizException("请选一间房");
        }
        if (input.shiftDate == null) {
            throw new BizException("请选班次日期");
        }
        if (input.nurse == null || input.nurse.isBlank()) {
            throw new BizException("要写清楚这一班是谁值");
        }
        if (input.startMin == null || input.endMin == null || input.endMin <= input.startMin) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        Room room = rooms.findByIdForUpdate(input.roomId)
                .orElseThrow(() -> new BizException("房间不存在"));
        if (!"在用".equals(room.status)) {
            throw new BizException("房间 " + room.name + " 现在是" + room.status + "，不排班次");
        }
        String nurse = input.nurse.trim();
        for (CareShift other : shifts.findByRoomIdAndShiftDateAndStatusIn(
                room.id, input.shiftDate, OPEN)) {
            if (overlap(other, input)) {
                throw new BizException("房间 " + room.name + " 这个时段已经排了班次 "
                        + other.shiftNo);
            }
        }
        for (CareShift other : shifts.findByNurseAndShiftDateAndStatusIn(
                nurse, input.shiftDate, OPEN)) {
            if (overlap(other, input)) {
                throw new BizException("护理员 " + nurse + " 这个时段已经排了班次 "
                        + other.shiftNo + "，一个人不能同时守两间房");
            }
        }

        CareShift saved = new CareShift();
        saved.shiftNo = nextShiftNo();
        saved.roomId = room.id;
        saved.shiftDate = input.shiftDate;
        saved.period = (input.period == null || input.period.isBlank()) ? "早班" : input.period;
        saved.nurse = nurse;
        saved.startMin = input.startMin;
        saved.endMin = input.endMin;
        saved.status = "待接班";
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        return shifts.save(saved);
    }

    @Transactional
    public CareShift advance(Long id, String action, String handoverNote) {
        // 先查房间号，再按“房间行 → 班次行”的全局顺序加锁，和房间维修/停用互斥。
        CareShift shift = shifts.findById(id).orElseThrow(() -> new BizException("班次不存在"));
        Room room = rooms.findByIdForUpdate(shift.roomId)
                .orElseThrow(() -> new BizException("班次对应的房间不存在"));
        CareShift s = shifts.findByIdForUpdate(id).orElseThrow(() -> new BizException("班次不存在"));

        if ("start".equals(action)) {
            if (!"待接班".equals(s.status)) {
                throw new BizException("只有待接班的班次能接班，这条现在是 " + s.status);
            }
            if (!"在用".equals(room.status)) {
                throw new BizException("房间 " + room.name + " 现在是" + room.status
                        + "，班次不能接班");
            }
            s.status = "值班中";
        } else if ("handover".equals(action)) {
            if (!"值班中".equals(s.status)) {
                throw new BizException("只有值班中的班次能交班，这条现在是 " + s.status);
            }
            if (!"在用".equals(room.status)) {
                throw new BizException("房间 " + room.name + " 现在是" + room.status
                        + "，不能在维修或停用态交班；先把房间恢复在用或取消班次");
            }
            String note = handoverNote == null ? s.handoverNote : handoverNote.trim();
            if (note == null || note.isBlank()) {
                throw new BizException("交班要写下注意事项，接班的才知道老人情况");
            }
            s.handoverNote = note;
            s.status = "已交班";
        } else if ("cancel".equals(action)) {
            if (!OPEN.contains(s.status)) {
                throw new BizException("这条班次现在是 " + s.status + "，取消不了");
            }
            s.status = "已取消";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        s.updatedAt = LocalDateTime.now();
        return shifts.save(s);
    }
}
