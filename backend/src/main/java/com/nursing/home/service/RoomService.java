package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.entity.Bed;
import com.nursing.home.entity.Room;
import com.nursing.home.repository.BedRepository;
import com.nursing.home.repository.CareShiftRepository;
import com.nursing.home.repository.ResidentRepository;
import com.nursing.home.repository.RoomRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

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
        Room r = rooms.findById(id).orElseThrow(() -> new BizException("房间不存在"));
        if (input.name != null) {
            r.name = input.name;
        }
        if (input.floor != null && input.floor > 0) {
            r.floor = input.floor;
        }
        if (input.kind != null && !input.kind.isBlank()) {
            r.kind = input.kind;
        }
        long living = residents.countByRoomIdAndStatus(r.id, "在住");
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
                if (!shifts.findByRoomIdAndShiftDateAndStatusNotIn(
                        r.id, LocalDate.now(), List.of("已交班", "已取消")).isEmpty()) {
                    throw new BizException("房间 " + r.name + " 今天还有没交接完的护理班次，先处理完再改成"
                            + input.status);
                }
            }
            r.status = input.status;
        }
        return rooms.save(r);
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
        Room room = rooms.findById(input.roomId).orElseThrow(() -> new BizException("房间不存在"));
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
        Bed b = beds.findById(id).orElseThrow(() -> new BizException("床位不存在"));
        if (input.position != null && !input.position.isBlank()) {
            b.position = input.position;
        }
        if (input.roomId != null && !input.roomId.equals(b.roomId)) {
            Room target = rooms.findById(input.roomId).orElseThrow(() -> new BizException("要挪过去的房间不存在"));
            if (!"在用".equals(target.status)) {
                throw new BizException("房间 " + target.name + " 现在是" + target.status + "，床位挪不进去");
            }
            if (!residents.findByBedIdAndStatusIn(b.id, List.of("在住")).isEmpty()) {
                throw new BizException("床位 " + b.code + " 上还住着老人，先转床再挪房间");
            }
            b.roomId = target.id;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(b.status)) {
            if (!"空闲".equals(input.status)
                    && !residents.findByBedIdAndStatusIn(b.id, List.of("在住")).isEmpty()) {
                throw new BizException("床位 " + b.code + " 上还住着老人，不能改成" + input.status);
            }
            b.status = input.status;
        }
        return beds.save(b);
    }
}
