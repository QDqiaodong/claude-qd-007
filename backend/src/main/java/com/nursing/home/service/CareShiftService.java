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

    /** 还占着房间与护理员的状态 */
    private static final List<String> OPEN = List.of("待接班", "值班中");

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
        Room room = rooms.findById(input.roomId).orElseThrow(() -> new BizException("房间不存在"));
        if (!"在用".equals(room.status)) {
            throw new BizException("房间 " + room.name + " 现在是" + room.status + "，不排班次");
        }
        String nurse = input.nurse.trim();
        for (CareShift other : shifts.findByRoomIdAndShiftDateAndStatusNotIn(
                room.id, input.shiftDate, List.of("已取消"))) {
            if (overlap(other, input)) {
                throw new BizException("房间 " + room.name + " 这个时段已经排了班次 "
                        + other.shiftNo);
            }
        }
        for (CareShift other : shifts.findByNurseAndShiftDateAndStatusNotIn(
                nurse, input.shiftDate, List.of("已取消"))) {
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
        CareShift s = shifts.findById(id).orElseThrow(() -> new BizException("班次不存在"));
        if ("start".equals(action)) {
            if (!"待接班".equals(s.status)) {
                throw new BizException("只有待接班的班次能接班，这条现在是 " + s.status);
            }
            s.status = "值班中";
        } else if ("handover".equals(action)) {
            if (!"值班中".equals(s.status)) {
                throw new BizException("只有值班中的班次能交班，这条现在是 " + s.status);
            }
            if (handoverNote != null && !handoverNote.isBlank()) {
                s.handoverNote = handoverNote;
            }
            if (s.handoverNote == null || s.handoverNote.isBlank()) {
                throw new BizException("交班要写下注意事项，接班的才知道老人情况");
            }
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
