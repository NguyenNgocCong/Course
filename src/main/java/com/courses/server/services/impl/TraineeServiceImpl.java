package com.courses.server.services.impl;

import com.courses.server.dto.request.Params;
import com.courses.server.dto.request.TraineeRequest;
import com.courses.server.entity.Class;
import com.courses.server.entity.Setting;
import com.courses.server.entity.Trainee;
import com.courses.server.entity.User;
import com.courses.server.entity.UserPackage;
import com.courses.server.exceptions.NotFoundException;
import com.courses.server.repositories.ClassRepository;
import com.courses.server.repositories.SettingRepository;
import com.courses.server.repositories.TraineeRepository;
import com.courses.server.repositories.UserPackageRepository;
import com.courses.server.repositories.UserRepository;
import com.courses.server.services.TraineeService;
import com.courses.server.utils.RandomCode;
import com.courses.server.utils.TemplateSendMail;
import com.courses.server.utils.Utility;
import io.jsonwebtoken.io.IOException;
import net.bytebuddy.utility.RandomString;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TraineeServiceImpl implements TraineeService {
    @Autowired
    private TraineeRepository traineeRepository;

    @Autowired
    private EmailSenderService senderService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserPackageRepository userPackageRepository;

    @Override
    public void create(TraineeRequest req, HttpServletRequest request) {

        if (req.getEmail() == null || req.getEmail().length() == 0) {
            throw new NotFoundException(404, "Vui l??ng nh???p email");
        }
        if (req.getFullname() == null || req.getFullname().length() == 0) {
            throw new NotFoundException(404, "Vui l??ng nh???p h??? v?? t??n");
        }
        if (req.getPhone() == null || req.getPhone().length() == 0) {
            throw new NotFoundException(404, "Vui l??ng nh???p s??? ??i???n tho???i");
        }
        if (req.getStatus() == null) {
            throw new NotFoundException(404, "Vui l??ng nh???p tr???ng th??i");
        }
        User user = null;
        try {
            user = userRepository.findByEmail(req.getEmail()).orElse(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (user == null) {
            String pass = RandomCode.getAlphaNumericString(8).toUpperCase();
            user.setPhoneNumber(req.getPhone());
            user.setPassword(encoder.encode(pass));
            user.setFullname(req.getFullname());
            Setting userRole = settingRepository.findByValueAndType("ROLE_GUEST", 1)
                    .orElseThrow(() -> new NotFoundException(404, "Error: Role Kh??ng t???n t???i"));

            user.setRole(userRole);

            String token = RandomString.make(30);

            user.setRegisterToken(token);
            user.setTimeRegisterToken(LocalDateTime.now());
            userRepository.save(user);
            String registerLink = Utility.getSiteURL(request) + "/api/account/verify?token=" + token;

            String subject = "????ng k?? kho?? h???c LRS education";

            String content = TemplateSendMail.getContent(registerLink, "Confirm Account",
                    "Your password: " + pass);
            senderService.sendEmail(req.getEmail(), subject, content);
        }
        if (traineeRepository.checkClassExistTraniee(req.getClassId(), user.getId()).size() > 0) {
            throw new NotFoundException(404, "L???p h???c ???? c?? h???c vi??n n??y");
        }
        Class aClass = null;
        try {
            aClass = classRepository.findById(req.getClassId()).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (aClass == null)
            throw new NotFoundException(404, "L???p h???c Kh??ng t???n t???i");
        Trainee trainee = new Trainee(user, req.getStatus(), req.getDropOutDate(), aClass);
        traineeRepository.save(trainee);
    }

    @Override
    public void update(Long id, TraineeRequest req) {

        Trainee trainee = null;
        try {
            trainee = traineeRepository.findById(id).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (trainee == null) {
            throw new NotFoundException(404, "H???c vi??n Kh??ng t???n t???i");
        }

        if (req.getStatus() != null)
            trainee.setStatus(req.getStatus());
        if (req.getDropOutDate() != null)
            trainee.setDropOutDate(req.getDropOutDate());
        if (req.getClassId() != null) {
            if (traineeRepository.checkClassExistTraniee(req.getClassId(), req.getUserId()).size() > 0) {
                throw new NotFoundException(404, "L???p h???c ???? t???n t???i");
            }
            Class aClass = null;
            try {
                aClass = classRepository.findById(req.getClassId()).get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (aClass == null)
                throw new NotFoundException(404, "L???p h???c Kh??ng t???n t???i");
            trainee.setAClass(aClass);
        }

        traineeRepository.save(trainee);
    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public Page<Trainee> getList(Pageable paging, Params params) throws IOException {
        System.out.println(params.getActive());
        if (params.getActive() == null && params.getCategory() == 0) {
            if (params.getKeyword() == null) {
                return traineeRepository.findAll(paging);
            } else {
                return traineeRepository.getListTraineeKeyword(params.getKeyword(), paging);
            }
        } else {
            if (params.getCategory() != 0 && params.getActive() == null) {
                if (params.getKeyword() == null) {
                    return traineeRepository.getListTraineeByClass(params.getCategory(), paging);
                } else {
                    return traineeRepository.getListTraineeKeywordAndClass(params.getKeyword(),
                            params.getCategory(), paging);
                }
            } else if (params.getCategory() == 0 && params.getActive() != null) {
                if (params.getKeyword() == null) {
                    return traineeRepository.getListTraineeByStatus(params.getActive(), paging);
                } else {
                    return traineeRepository.getListTraineeKeywordAndStatus(params.getKeyword(),
                            params.getActive(), paging);
                }
            } else {
                if (params.getKeyword() == null) {
                    return traineeRepository.getListTraineeByStatusAndClass(params.getActive(), params.getCategory(),
                            paging);
                } else {
                    return traineeRepository.getListTraineeKeywordAndStatusAndClass(params.getKeyword(),
                            params.getActive(), params.getCategory(), paging);
                }
            }
        }
    }

    @Override
    public Page<UserPackage> getListOnline(Pageable paging, Params params) throws IOException {
        System.out.println(params.getActive());
        if (params.getActive() == null && params.getCategory() == 0) {
            if (params.getKeyword() == null) {
                return userPackageRepository.findAll(paging);
            } else {
                return userPackageRepository.getListUserPackageKeyword(params.getKeyword(), paging);
            }
        } else {
            if (params.getCategory() != 0 && params.getActive() == null) {
                if (params.getKeyword() == null) {
                    return userPackageRepository.getListUserPackageByPackage(params.getCategory(), paging);
                } else {
                    return userPackageRepository.getListUserPackageKeywordAndPackage(params.getKeyword(),
                            params.getCategory(), paging);
                }
            } else if (params.getCategory() == 0 && params.getActive() != null) {
                if (params.getKeyword() == null) {
                    return userPackageRepository.getListUserPackageByStatus(params.getActive(), paging);
                } else {
                    return userPackageRepository.getListUserPackageKeywordAndStatus(params.getKeyword(),
                            params.getActive(), paging);
                }
            } else {
                if (params.getKeyword() == null) {
                    return userPackageRepository.getListUserPackageByStatusAndPackage(params.getActive(), params.getCategory(),
                            paging);
                } else {
                    return userPackageRepository.getListUserPackageKeywordAndStatusAndPackage(params.getKeyword(),
                            params.getActive(), params.getCategory(), paging);
                }
            }
        }
    }

    @Override
    public Trainee getDetail(Long id) {
        Trainee trainee = null;
        try {
            trainee = traineeRepository.findById(id).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (trainee == null) {
            throw new NotFoundException(404, "H???c vi??n Kh??ng t???n t???i");
        }

        return trainee;
    }

    @Override
    public Page<Trainee> getListTraineDetail(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        return traineeRepository.findAllByUser(user, pageable);
    }
}
