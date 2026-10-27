package com.certimate.manager.user.dto;

import java.time.LocalDate;

public record AddAcquiredCertRequest(
        Long certId,
        String certName,
        LocalDate acquiredDate,
        Long fromScheduleId
) {}
