package com.courses.server.services;

import com.courses.server.dto.response.DashboardAdminAndManager;
import com.courses.server.dto.response.DashboardSupporter;
import org.springframework.stereotype.Service;

@Service
public interface DashboardService {
    DashboardAdminAndManager getAll();

    DashboardSupporter getSupporter();
}
