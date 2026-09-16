package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.entity.Bed;
import com.nursing.home.entity.CareShift;
import com.nursing.home.entity.Room;
import com.nursing.home.repository.BedRepository;
import com.nursing.home.repository.CareShiftRepository;
import com.nursing.home.repository.ResidentRepository;
import com.nursing.home.repository.RoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    /** 还会占着房间与护理员的班次，必须交班或取消后才能维修/停用。 */
    private static final List<String> OPEN_SHIFT_STATUSES = List.of("待接班", "值班中");
    private static final List<String> LIVING_STATUSES = List.of("在住", "请假外出");

    private final RoomRepository rooms;
    private final BedRepository beds;
    private final ResidentRepository residents;
    private final CareShiftRepository shifts;

    public RoomService(RoomRepository rooms, BedRepository beds, ResidentRepository residents,
                       CareShiftRepository shifts) {
        this.rooms = rooms;
        this.beds = beds;
        this.residents = residents;
        this.shifts = shifts;
    }

    public List<Room> listRooms(String status, String keyword) {
        return rooms.findAll().stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.status))
                .filter(r -> keyword == null || keyword.isEmpty()
                        || r.name.contains(keyword) || r.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Room createRoom(Room input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("房间编号不能为空");
        }
        if (rooms.existsByCode(input.code)) {
            throw new BizException("房间编号 " + input.code + " 已经存在");
        }
        Room saved = new Room();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.floor = (input.floor == null || input.floor <= 0) ? 1 : input.floor;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "双人间" : input.kind;
        saved.capacity = (input.capacity == null || input.capacity <= 0) ? 2 : input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return rooms.save(saved);
    }

    @Transactional
    public Room updateRoom(Long id, Room input) {
        Room r = rooms.findByIdForUpdate(id).orElseThrow(() -> new BizException("房间不存在"));
        if (input.name != null) {
            r.name = input.name;
        }
        if (input.floor != null && input.floor > 0) {
            r.floor = input.floor;
        }
        if (input.kind != null && !input.kind.isBlank()) {
            r.kind = input.kind;
        }
        long living = residents.countByRoomIdAndStatusIn(r.id, LIVING_STATUSES);
        if (input.capacity != null && input.capacity > 0 && !input.capacity.equals(r.capacity)) {
            if (input.capacity < living) {
                throw new BizException("房间 " + r.name + " 现在住着 " + living + " 位老人，"
                        + "可住人数不能改到比它小");
            }
            r.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(r.status)) {
            if (!"在用".equals(input.status)) {
                if (living > 0) {
                    throw new BizException("房间 " + r.name + " 里还住着 " + living + " 位老人，"
                            + "先安排转房或者办退住再改成" + input.status);
                }
                if (!shifts.findByRoomIdAndStatusIn(r.id, OPEN_SHIFT_STATUSES).isEmpty()) {
                    throw new BizException("房间 " + r.name + " 还有待接班或值班中的护理班次，"
                            + "先交班或取消再改成" + input.status);
                }
            }
            r.status = input.status;
        }
        return rooms.save(r);
    }

    /**
     * 关闭房间：先在同一事务、同一房间行锁内完成全部未结束班次，再提交房间维修/停用状态。
     * 任何一步失败都整体回滚，不会留下班次已交班但房间仍被占用的半成品。
     */
    @Transactional
    public Room closeRoom(Long id, String targetStatus, List<CareShiftAction> shiftActions) {
        if (!"维修".equals(targetStatus) && !"停用".equals(targetStatus)) {
            throw new BizException("这个接口只能把房间改成维修或停用");
        }
        Room r = rooms.findByIdForUpdate(id).orElseThrow(() -> new BizException("房间不存在"));
        if (targetStatus.equals(r.status)) {
            throw new BizException("房间 " + r.name + " 已经是" + targetStatus);
        }
        long living = residents.countByRoomIdAndStatusIn(r.id, LIVING_STATUSES);
        if (living > 0) {
            throw new BizException("房间 " + r.name + " 里还住着 " + living + " 位老人，"
                    + "先安排转房或者办退住再改成" + targetStatus);
        }

        List<CareShift> openShifts = shifts.findByRoomIdAndStatusIn(r.id, OPEN_SHIFT_STATUSES);
        for (CareShift latestShift : openShifts) {
            CareShift openShift = shifts.findByIdForUpdate(latestShift.id)
                    .orElseThrow(() -> new BizException("班次不存在"));
            if (!OPEN_SHIFT_STATUSES.contains(openShift.status)) {
                throw new BizException("班次 " + openShift.shiftNo + " 状态刚发生变化，请刷新后重试");
            }
            CareShiftAction action = shiftActions == null ? null
                    : shiftActions.stream()
                            .filter(item -> openShift.id.equals(item.shiftId()))
                            .findFirst()
                            .orElse(null);
            if (action == null) {
                throw new BizException("班次 " + openShift.shiftNo + " 还没交班或取消，"
                        + "不能把房间改成" + targetStatus);
            }
            changeShift(openShift, action.action(), action.handoverNote());
        }
        r.status = targetStatus;
        return rooms.save(r);
    }

    private void changeShift(CareShift s, String action, String handoverNote) {
        if ("handover".equals(action)) {
            if (!"值班中".equals(s.status)) {
                throw new BizException("班次 " + s.shiftNo + " 现在是 " + s.status + "，不能交班");
            }
            String note = handoverNote == null ? s.handoverNote : handoverNote.trim();
            if (note == null || note.isBlank()) {
                throw new BizException("班次 " + s.shiftNo + " 交班要写下注意事项");
            }
            s.handoverNote = note;
            s.status = "已交班";
        } else if ("cancel".equals(action)) {
            if (!OPEN_SHIFT_STATUSES.contains(s.status)) {
                throw new BizException("班次 " + s.shiftNo + " 现在是 " + s.status + "，取消不了");
            }
            s.status = "已取消";
        } else {
            throw new BizException("班次 " + s.shiftNo + " 的处理动作只能是交班或取消");
        }
        s.updatedAt = LocalDateTime.now();
        shifts.save(s);
    }

    public record CareShiftAction(Long shiftId, String action, String handoverNote) {
    }

    public List<Bed> listBeds(Long roomId, String status, String keyword) {
        return beds.findAll().stream()
                .filter(b -> roomId == null || roomId.equals(b.roomId))
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .filter(b -> keyword == null || keyword.isEmpty() || b.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Bed createBed(Bed input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("床位编号不能为空");
        }
        if (beds.existsByCode(input.code)) {
            throw new BizException("床位编号 " + input.code + " 已经存在");
        }
        if (input.roomId == null) {
            throw new BizException("请先选这个床位在哪个房间");
        }
        Room room = rooms.findByIdForUpdate(input.roomId).orElseThrow(() -> new BizException("房间不存在"));
        if (!"在用".equals(room.status)) {
            throw new BizException("房间 " + room.name + " 现在是" + room.status + "，不能再往里加床位");
        }
        Bed saved = new Bed();
        saved.code = input.code.trim();
        saved.roomId = room.id;
        saved.position = (input.position == null || input.position.isBlank()) ? "中间" : input.position;
        saved.status = (input.status == null || input.status.isBlank()) ? "空闲" : input.status;
        return beds.save(saved);
    }

    @Transactional
    public Bed updateBed(Long id, Bed input) {
        Bed unlocked = beds.findById(id).orElseThrow(() -> new BizException("床位不存在"));
        if (input.roomId != null && !input.roomId.equals(unlocked.roomId)) {
            // 全局按“房间行 → 床位行”的顺序加锁，避免转床事务和老人转床形成死锁。
            if (unlocked.roomId != null && unlocked.roomId < input.roomId) {
                rooms.findByIdForUpdate(unlocked.roomId).orElseThrow(() -> new BizException("房间不存在"));
                rooms.findByIdForUpdate(input.roomId).orElseThrow(() -> new BizException("房间不存在"));
            } else if (!input.roomId.equals(unlocked.roomId)) {
                rooms.findByIdForUpdate(input.roomId).orElseThrow(() -> new BizException("房间不存在"));
                if (unlocked.roomId != null) {
                    rooms.findByIdForUpdate(unlocked.roomId).orElseThrow(() -> new BizException("房间不存在"));
                }
            }
        }
        Bed b = beds.findByIdForUpdate(id).orElseThrow(() -> new BizException("床位不存在"));
        if (input.position != null && !input.position.isBlank()) {
            b.position = input.position;
        }
        if (input.roomId != null && !input.roomId.equals(b.roomId)) {
            Room target = rooms.findByIdForUpdate(input.roomId)
                    .orElseThrow(() -> new BizException("要挪过去的房间不存在"));
            if (!"在用".equals(target.status)) {
                throw new BizException("房间 " + target.name + " 现在是" + target.status + "，床位挪不进去");
            }
            if (!residents.findByBedIdAndStatusIn(b.id, LIVING_STATUSES).isEmpty()) {
                throw new BizException("床位 " + b.code + " 上还住着老人，先转床再挪房间");
            }
            b.roomId = target.id;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(b.status)) {
            if (!"空闲".equals(input.status)
                    && !residents.findByBedIdAndStatusIn(b.id, LIVING_STATUSES).isEmpty()) {
                throw new BizException("床位 " + b.code + " 上还住着老人，不能改成" + input.status);
            }
            b.status = input.status;
        }
        return beds.save(b);
    }
}
