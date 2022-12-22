package com.courses.server.services.impl;

import com.courses.server.dto.response.DashboardAdminAndManager;
import com.courses.server.dto.response.DashboardSupporter;
import com.courses.server.dto.response.PackageDTO;
import com.courses.server.dto.response.Turnover;
import com.courses.server.repositories.*;
import com.courses.server.services.DashboardService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private TraineeRepository traineeRepository;

    @Autowired
    private ExpertRepository expertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private OrderPackageRepository orderPackageRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public DashboardAdminAndManager getAll() {
        DashboardAdminAndManager res = new DashboardAdminAndManager();
        res.setTotalTraineeActive(traineeRepository.countAllByStatus(true));
        res.setTotalExpert(expertRepository.count());
        res.setTotalTrainer(userRepository.countTrainer());
        res.setTotalMarketer(userRepository.countMakerter());
        res.setTotalSupporter(userRepository.countSupporter());
        res.setTotalClass(classRepository.count());
        res.setTotalSubject(subjectRepository.count());
        res.setTotalSoldOut(orderPackageRepository.countSoldOut().size());
        res.setTurnovers(orderRepository.getAllTurnover());
        res.setAPackage(new PackageDTO(packageRepository.top1PackageBuy()));

        return res;
    }

    @Override
    public DashboardSupporter getSupporter() {
        DashboardSupporter res = new DashboardSupporter();
        res.setTotalSoldOut(orderPackageRepository.countSoldOut().size());
        res.setAPackage(new PackageDTO(packageRepository.top1PackageBuy()));
        return res;
    }
}
