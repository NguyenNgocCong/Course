package com.courses.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DashboardAdminAndManager {
    private long totalTraineeActive;
    private long totalExpert;
    private long totalTrainer;
    private long totalMarketer;
    private long totalSupporter;
    private long totalClass;
    private long totalSubject;
    private long totalSoldOut;
    List<Object> turnovers;
    private PackageDTO aPackage;
}
