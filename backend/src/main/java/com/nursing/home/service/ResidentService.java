package com.nursing.home.service;

import com.nursing.home.dto.BizException;
import com.nursing.home.entity.Bed;
import com.nursing.home.entity.Resident;
import com.nursing.home.entity.Room;
import com.nursing.home.repository.BedRepository;
import com.nursing.home.repository.ResidentRepository;
import com.nursing.home.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResidentService {

    private final ResidentRepository residents;
    private final RoomRepository rooms;
    private final BedRepository beds;

    public ResidentService(ResidentRepository residents, RoomRepository rooms, BedRepository beds) {
        this.residents = residents;
        this.rooms = rooms;
        this.beds = beds;
    }

    public List<Resident> list(String status, String careLevel, String keyword) {
        return residents.findAllByOrderByUpdatedAtDesc().stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.status))
                .filter(r -> careLevel == null || careLevel.isEmpty() || careLevel.equals(r.careLevel))
                .filter(r -> keyword == null || keyword.isEmpty()
                        || r.name.contains(keyword) || r.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Resident create(Resident input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("档案号不能为空");
        }
        if (input.name == null || input.name.isBlank()) {
            throw new BizException("老人姓名不能为空");
        }
        if (residents.existsByCode(input.code)) {
            throw new BizException("档案号 " + input.code + " 已经存在");
        }
        Resident saved = new Resident();
        saved.code = input.code.trim();
        saved.name = input.name.trim();
        saved.gender = (input.gender == null || input.gender.isBlank()) ? "女" : input.gender;
        saved.age = (input.age == null || input.age <= 0) ? 60 : input.age;
        saved.careLevel = (input.careLevel == null || input.careLevel.isBlank()) ? "自理" : input.careLevel;
        saved.familyPhone = input.familyPhone;
        saved.status = (input.status == null || input.status.isBlank()) ? "在住" : input.status;
        if (input.bedId != null) {
            occupy(saved, input.roomId, input.bedId);
        } else if ("在住".equals(saved.status)) {
            throw new BizException("办入住要指定床位");
        }
        saved.checkInDate = input.checkInDate == null ? LocalDate.now() : input.checkInDate;
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        return residents.save(saved);
    }

    /** 把老人落到某张床上，顺带校验房间与容量。 */
    private void occupy(Resident r, Long roomId, Long bedId) {
        Bed bed = beds.findById(bedId).orElseThrow(() -> new BizException("床位不存在"));
        if (!"空闲".equals(bed.status)) {
            throw new BizException("床位 " + bed.code + " 现在是" + bed.status + "，安排不了");
        }
        Long useRoom = roomId != null ? roomId : bed.roomId;
        if (useRoom == null) {
            throw new BizException("床位 " + bed.code + " 还没归到房间，先把它挂到一间房上");
        }
        if (bed.roomId != null && !bed.roomId.equals(useRoom)) {
            throw new BizException("床位 " + bed.code + " 不在选的那间房里");
        }
        Room room = rooms.findById(useRoom).orElseThrow(() -> new BizException("房间不存在"));
        if (!"在用".equals(room.status)) {
            throw new BizException("房间 " + room.name + " 现在是" + room.status + "，不能安排入住");
        }
        long living = residents.countByRoomIdAndStatus(room.id, "在住");
        if (living + 1 > room.capacity) {
            throw new BizException("房间 " + room.name + " 最多住 " + room.capacity + " 位老人，"
                    + "现在已经住了 " + living + " 位，安排不下了");
        }
        if (!residents.findByBedIdAndStatusIn(bed.id, List.of("在住")).isEmpty()) {
            throw new BizException("床位 " + bed.code + " 上已经住了人");
        }
        r.roomId = room.id;
        r.bedId = bed.id;
        bed.status = "占用";
        beds.save(bed);
    }

    private void release(Long bedId) {
        if (bedId == null) {
            return;
        }
        beds.findById(bedId).ifPresent(bed -> {
            bed.status = "空闲";
            beds.save(bed);
        });
    }

    @Transactional
    public Resident update(Long id, Resident input) {
        Resident r = residents.findById(id).orElseThrow(() -> new BizException("老人档案不存在"));
        if (input.name != null && !input.name.isBlank()) {
            r.name = input.name.trim();
        }
        if (input.gender != null && !input.gender.isBlank()) {
            r.gender = input.gender;
        }
        if (input.age != null && input.age > 0) {
            r.age = input.age;
        }
        if (input.careLevel != null && !input.careLevel.isBlank()) {
            r.careLevel = input.careLevel;
        }
        if (input.familyPhone != null) {
            r.familyPhone = input.familyPhone;
        }

        boolean bedChanged = input.bedId != null && !input.bedId.equals(r.bedId);
        if (bedChanged) {
            if ("已退住".equals(r.status)) {
                throw new BizException("老人 " + r.name + " 已经退住，要先恢复在住才能重新安排床位");
            }
            Long oldBed = r.bedId;
            occupy(r, input.roomId, input.bedId);
            release(oldBed);
        }

        if (input.status != null && !input.status.isBlank() && !input.status.equals(r.status)) {
            if ("已退住".equals(r.status)) {
                throw new BizException("老人 " + r.name + " 已经退住，改不回去了");
            }
            if ("在住".equals(input.status)) {
                if (r.bedId == null) {
                    throw new BizException("要恢复在住得先指定床位");
                }
            }
            if ("已退住".equals(input.status)) {
                release(r.bedId);
                r.checkOutDate = input.checkOutDate == null ? LocalDate.now() : input.checkOutDate;
            }
            r.status = input.status;
        }
        r.updatedAt = LocalDateTime.now();
        return residents.save(r);
    }
}
