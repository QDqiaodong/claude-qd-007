package com.nursing.home.config;

import com.nursing.home.entity.Bed;
import com.nursing.home.entity.Resident;
import com.nursing.home.repository.BedRepository;
import com.nursing.home.repository.MedicalEscortRepository;
import com.nursing.home.repository.ResidentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 修复早期种子数据里“档案勾选请假外出，但没有护送单”的不合规台账。
 * 正常业务只能通过护送单改变请假状态，这里只处理预置旧数据。
 */
@Component
public class LegacyResidentDataMigration implements ApplicationRunner {

    private final ResidentRepository residents;
    private final BedRepository beds;
    private final MedicalEscortRepository escorts;

    public LegacyResidentDataMigration(ResidentRepository residents, BedRepository beds,
                                       MedicalEscortRepository escorts) {
        this.residents = residents;
        this.beds = beds;
        this.escorts = escorts;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();
        for (Resident resident : residents.findAll()) {
            if (!"请假外出".equals(resident.status) || resident.bedId == null) {
                continue;
            }
            boolean hasOpenEscort = escorts
                    .findFirstByResidentIdAndStatusInOrderByCreatedAtDesc(
                            resident.id, List.of("护送中", "滞留"))
                    .isPresent();
            if (hasOpenEscort) {
                continue;
            }

            Bed bed = beds.findById(resident.bedId).orElse(null);
            if (bed == null) {
                continue;
            }
            bed.status = "占用";
            beds.save(bed);
            resident.status = "在住";
            resident.updatedAt = now;
            residents.save(resident);
        }
    }
}
