package com.nursing.home.controller;

import com.nursing.home.entity.Bed;
import com.nursing.home.entity.Room;
import com.nursing.home.service.RoomService;
import com.nursing.home.service.RoomService.CareShiftAction;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping("/rooms")
    public List<Room> listRooms(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String keyword) {
        return service.listRooms(status, keyword);
    }

    @PostMapping("/rooms")
    public Room createRoom(@RequestBody Room input) {
        return service.createRoom(input);
    }

    @PutMapping("/rooms/{id}")
    public Room updateRoom(@PathVariable Long id, @RequestBody Room input) {
        return service.updateRoom(id, input);
    }

    @PostMapping("/rooms/{id}/close")
    public Room closeRoom(@PathVariable Long id, @RequestBody CloseRoomRequest input) {
        return service.closeRoom(id, input.targetStatus(), input.shiftActions());
    }

    public record CloseRoomRequest(String targetStatus, List<CareShiftAction> shiftActions) {
    }

    @GetMapping("/beds")
    public List<Bed> listBeds(@RequestParam(required = false) Long roomId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword) {
        return service.listBeds(roomId, status, keyword);
    }

    @PostMapping("/beds")
    public Bed createBed(@RequestBody Bed input) {
        return service.createBed(input);
    }

    @PutMapping("/beds/{id}")
    public Bed updateBed(@PathVariable Long id, @RequestBody Bed input) {
        return service.updateBed(id, input);
    }
}
