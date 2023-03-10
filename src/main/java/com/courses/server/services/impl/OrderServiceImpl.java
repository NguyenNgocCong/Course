package com.courses.server.services.impl;

import com.courses.server.dto.request.OrderRequest;
import com.courses.server.dto.request.OrderRequestAdmin;
import com.courses.server.dto.request.ProductOrderRequest;
import com.courses.server.entity.*;
import com.courses.server.entity.Class;
import com.courses.server.entity.Package;
import com.courses.server.exceptions.BadRequestException;
import com.courses.server.exceptions.NotFoundException;
import com.courses.server.repositories.*;
import com.courses.server.services.OrderService;
import com.courses.server.utils.AutoSetSuporter;
import com.courses.server.utils.RandomCode;
import com.courses.server.utils.TemplateSendMail;
import com.courses.server.utils.Utility;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private OrderPackageRepository orderPackageRepository;

    @Autowired
    private EmailSenderService senderService;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private TraineeRepository traineeRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public void addProductOrder(ProductOrderRequest req) {
        if (req.getComboId() == null && req.getPackageId() == null)
            throw new BadRequestException(400, "Vui l??ng ch???n s???n ph???m");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        Order order = null;
        try {
            order = orderRepository.getMyCart(user.getId()).orElse(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (order == null) {
            order = new Order();
            Random rand = new Random();
            String date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
            String code = "OD" + date + String.format("%04d", rand.nextInt(10000));
            order.setCode(code);
            order.setUser(user);
            order.setSupporter(AutoSetSuporter.setSupporter(settingRepository, userRepository));
            order.setTotalCost(0);
            order.setTotalDiscount(0);
        }

        if (req.getPackageId() != null) {
            Package _package = null;
            try {
                _package = packageRepository.findById(req.getPackageId()).get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (_package == null) {
                throw new NotFoundException(404, "Package  Kh??ng t???n t???i");
            }

            OrderPackage item = new OrderPackage();
            item.set_package(_package);
            item.setOrder(order);
            item.setPackageCost(_package.getSalePrice());
            item.setActivated(false);
            order.getOrderPackages().add(item);
            order.increaseTotalCost(item.getPackageCost());
            order.increaseDiscount(0);
        }

        if (req.getComboId() != null) {
            Combo combo = null;
            try {
                combo = comboRepository.findById(req.getComboId()).get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (combo == null)
                throw new NotFoundException(404, "Combo  Kh??ng t???n t???i");
            OrderPackage item = new OrderPackage();
            item.set_combo(combo);
            item.setOrder(order);
            double sumCost = 0;
            for (ComboPackage element : combo.getComboPackages()) {
                sumCost += element.getSalePrice();
            }
            ;
            item.setPackageCost(sumCost);
            // item.setActivationKey(RandomCode.getAlphaNumericString(8).toUpperCase());
            item.setActivated(false);
            order.getOrderPackages().add(item);
            order.increaseTotalCost(item.getPackageCost());
            order.increaseDiscount(0);
        }

        orderRepository.save(order);
    }

    @Override
    public void deleteProductOrders(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        if (id == null) {
            throw new NotFoundException(404, "Vui l??ng nh???p th??ng tin s???n ph???m!");
        }
        OrderPackage orderPackage = orderPackageRepository.findById(id).orElse(null);
        if (orderPackage == null) {
            throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i!");
        }
        if (orderPackage.getOrder().getUser() != user)
            throw new NotFoundException(404, "L???i x??c th???c ng?????i d??ng!");
        if (orderPackageRepository.deleteProduct(id) == 0) {
            throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i!");
        }
        orderPackage.getOrder().decreaseTotalCost(orderPackage.getPackageCost());
        orderRepository.save(orderPackage.getOrder());
    }

    @Override
    public void deleteProductOrders(Long id, Boolean isOrder) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        if (id == null) {
            throw new NotFoundException(404, "Vui l??ng nh???p th??ng tin s???n ph???m!");
        }
        if (isOrder == null) {
            throw new NotFoundException(404, "Vui l??ng nh???p th??ng tin lo???i ????n h??ng!");
        }
        if (isOrder) {
            Order order = orderRepository.findById(id).orElse(null);
            if (order == null) {
                throw new NotFoundException(404, "????n h??ng kh??ng t???n t???i!");
            }
            if (!user.getRole().getSetting_value().equals("ROLE_SUPPORTER")
                    && !user.getRole().getSetting_value().equals("ROLE_ADMIN") && order.getUser() != user)
                throw new NotFoundException(404, "L???i x??c th???c ng?????i d??ng!");
            order.setStatus(4);
            orderRepository.save(order);
        } else {
            OrderPackage orderPackage = orderPackageRepository.findById(id).orElse(null);
            if (orderPackage == null) {
                throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i!");
            }
            if (!user.getRole().getSetting_value().equals("ROLE_SUPPORTER")
                    && !user.getRole().getSetting_value().equals("ROLE_ADMIN")
                    && orderPackage.getOrder().getUser() != user)
                throw new NotFoundException(404, "L???i x??c th???c ng?????i d??ng!");
            if (orderPackageRepository.countProduct(orderPackage.getOrder().getId()).size() > 1) {
                if (orderPackageRepository.deleteProduct(id) == 0)
                    throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i!");
                if (orderPackage.getOrder().getCoupon() != null) {
                    double total = orderPackage.getOrder().getTotalCost() + orderPackage.getOrder().getTotalDiscount()
                            - orderPackage.getPackageCost();
                    double discount = total * orderPackage.getOrder().getCoupon().getDiscountRate() / 100;
                    orderPackage.getOrder().setTotalDiscount(discount);
                    orderPackage.getOrder().setTotalCost(total - discount);
                    orderRepository.save(orderPackage.getOrder());
                } else {
                    orderPackage.getOrder().decreaseTotalCost(orderPackage.getPackageCost());
                    orderRepository.save(orderPackage.getOrder());
                }
            } else {
                orderPackage.getOrder().setStatus(4);
                orderRepository.save(orderPackage.getOrder());
            }
        }
    }

    @Override
    public List<OrderPackage> store() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Order order = orderRepository.getMyCart(user.getId()).orElse(null);

        if (order == null) {
            return new ArrayList<OrderPackage>();
        } else {
            List<OrderPackage> pagePackage = orderPackageRepository.findAllByOrder(order);
            return pagePackage;
        }
    }

    @Override
    public void createNoLogin(OrderRequest req, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Order order = new Order();
        if (auth != null) {
            User user = userRepository.findByUsername(auth.getName());
            order.setUser(user);
        } else {
            if (req.getFullName() == null || req.getEmail() == null || req.getMobile() == null) {
                throw new BadRequestException(400, "Vui l??ng nh???p ????? th??ng tin");
            }
            User user = userRepository.findByEmail(req.getEmail()).orElse(null);
            if (user == null) {
                if (req.getPackages().size() == 0 && req.getCombos().size() == 0
                        && (req.getClassId() == null || req.getClassId() == 0)) {
                    throw new BadRequestException(400, "Vui l??ng th??m s???n ph???m");
                }
                Customer customer = customerRepository.findByEmail(req.getEmail()).orElse(null);
                if (customer == null) {
                    customer = customerRepository
                            .save(new Customer(req.getFullName(), req.getEmail(), req.getMobile()));
                }
                order.setCustomer(customer);
            } else {
                order.setUser(user);
            }
        }
        Random rand = new Random();
        String date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
        String code = "OD" + date + String.format("%04d", rand.nextInt(10000));
        order.setSupporter(AutoSetSuporter.setSupporter(settingRepository, userRepository));
        order.setCode(code);
        order.setStatus(1);

        if (req.getClassId() != null && req.getClassId() != 0) {
            if (order.getUser() != null) {
                if (orderPackageRepository.checkClassExistUser(req.getClassId(), order.getUser().getId()).size() > 0) {
                    throw new NotFoundException(404, "B???n ???? ????ng k?? l???p n??y r???i!");
                }
                if (traineeRepository.checkClassExistTraniee(req.getClassId(), order.getUser().getId()).size() > 0) {
                    throw new NotFoundException(404, "B???n ???? ????ng k?? l???p n??y r???i!");
                }
            } else if (order.getCustomer() != null) {
                if (orderPackageRepository.checkClassExistCustomer(req.getClassId(), order.getCustomer().getId())
                        .size() > 0) {
                    throw new NotFoundException(404, "B???n ???? ????ng k?? l???p n??y r???i!");
                }
            }

            Class classes = classRepository.findById(req.getClassId()).orElse(null);

            if (classes == null) {
                throw new NotFoundException(404, "L???p h???c kh??ng t???n t???i");
            }

            if (order.getUser() != null) {
                List<Trainee> trainees = null;
                try {
                    trainees = traineeRepository.findByUser(order.getUser());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                if (trainees != null && trainees.size() > 0) {
                    for (Trainee trainee : trainees) {
                        if (trainee.getAClass().getId() == classes.getId()) {
                            throw new BadRequestException(400, "B???n ???? ????ng k?? l???p n??y r???i");
                        }
                    }
                }
            }

            OrderPackage item = new OrderPackage();
            item.set_package(classes.getAPackage());
            item.setOrder(order);
            item.setPackageCost(classes.getAPackage().getSalePrice());
            item.setActivated(false);
            order.setAClass(classes);
            order.setTotalDiscount(0);
            order.getOrderPackages().add(item);
            order.increaseTotalCost(item.getPackageCost());

            if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                Coupon coupon = null;
                try {
                    coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (coupon != null) {
                    int countUseCoupon = 0;
                    if (order.getUser() != null) {
                        countUseCoupon = orderRepository
                                .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                .size();
                    } else {
                        countUseCoupon = orderRepository
                                .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                    }
                    if (countUseCoupon < coupon.getQuantity()) {
                        double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                        order.setTotalDiscount(discount);
                        order.setCoupon(coupon);
                        order.decreaseTotalCost(discount);
                    } else {
                        throw new BadRequestException(400, "M?? gi???m gi?? ???? h???t l?????t s??? d???ng");
                    }
                } else {
                    throw new BadRequestException(400, "M?? gi???m gi?? kh??ng ????ng ho???c ???? h???t h???n");
                }
            }
            String subject = "????ng k?? l???p h???c LRS education th??nh c??ng";
            String des = "M?? l???p h???c: " + classes.getCode() + "<br> T??n kh??a h???c"
                    + classes.getAPackage().getTitle() +
                    "<br> Th???i gian: t???" + classes.getDateFrom() + "<br> ?????n" + classes.getDateTo() +
                    "<br> L???ch h???c: " + classes.getTime() + "<br> v??o c??c ng??y" + classes.getSchedule() +
                    "<br> Th??ng tin thanh to??n: <br>     T???ng ti???n: " + order.getTotalCost()
                    + " <br>     S??? t??i kho???n: 0586986918888<br>     Ng??n h??ng: qu??n ?????i MB bank<br>     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";

            String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                    "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                    des, "L???p h???c", "????ng k??");
            senderService.sendEmail(req.getEmail(), subject, content);

        } else {
            String des = "Danh s??ch s???n ph???m:<br>";
            for (Long id : req.getPackages()) {
                Package _package = null;
                try {
                    _package = packageRepository.findById(id).get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (_package == null) {
                    throw new NotFoundException(404, "S???n ph???m Kh??ng t???n t???i");
                }
                des += "<br>     Kh??a h???c: " + _package.getTitle() + ", gi??: " + _package.getSalePrice() + " VN??";

                OrderPackage item = new OrderPackage();
                item.set_package(_package);
                item.setOrder(order);
                item.setPackageCost(_package.getSalePrice());
                item.setActivated(false);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
            }

            for (Long comboId : req.getCombos()) {
                Combo combo = null;
                try {
                    combo = comboRepository.findById(comboId).get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (combo == null)
                    throw new NotFoundException(404, "Combo  Kh??ng t???n t???i");
                int sumCost = 0;
                for (ComboPackage element : combo.getComboPackages()) {
                    sumCost += element.getSalePrice();
                }
                des += "<br>     Combo: " + combo.getTitle() + ", gi??: " + sumCost + " VN??";
                OrderPackage item = new OrderPackage();
                item.set_combo(combo);
                item.setOrder(order);
                item.setPackageCost(sumCost);
                item.setActivated(false);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
            }
            if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                Coupon coupon = null;
                try {
                    coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (coupon != null) {
                    int countUseCoupon = 0;
                    if (order.getUser() != null) {
                        countUseCoupon = orderRepository
                                .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                .size();
                    } else {
                        countUseCoupon = orderRepository
                                .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                    }
                    if (countUseCoupon < coupon.getQuantity()) {
                        double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                        order.setTotalDiscount(discount);
                        order.setCoupon(coupon);
                        order.decreaseTotalCost(discount);
                    } else {
                        throw new BadRequestException(400, "M?? gi???m gi?? ???? h???t l?????t s??? d???ng");
                    }
                } else {
                    throw new BadRequestException(400, "M?? gi???m gi?? kh??ng ????ng ho???c ???? h???t h???n");
                }
            }
            String subject = "????ng k?? kh??a h???c LRS education th??nh c??ng";
            des += "<br> Th??ng tin thanh to??n: <br>     T???ng ti???n: " + order.getTotalCost()
                    + " <br>     S??? t??i kho???n: 0586986918888<br>     Ng??n h??ng: qu??n ?????i MB bank<br>     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";
            String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                    "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                    des, "Kh??a h???c", "????ng k??");
            senderService.sendEmail(req.getEmail(), subject, content);
        }
        if (order.getOrderPackages().size() == 0) {
            throw new NotFoundException(404, "S???n ph???m Kh??ng t???n t???i");
        }
        orderRepository.save(order);
    }

    @Override
    public void createAdmin(OrderRequestAdmin req, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Order order = new Order();

        if (req.getEmail() == null || req.getEmail().length() == 0) {
            throw new BadRequestException(400, "Vui l??ng nh???p email");
        }

        if (req.getFullName() == null || req.getFullName().trim().length() == 0) {
            throw new BadRequestException(400, "Vui l??ng nh???p t??n khach h??ng");
        }
        User userAdd = userRepository.findByUsername(auth.getName());

        if (userAdd.getRole().getSetting_value().equals("ROLE_SUPPORTER")) {
            order.setSupporter(userAdd);
        } else {
            order.setSupporter(AutoSetSuporter.setSupporter(settingRepository, userRepository));
        }
        if (req.getNote() != null) {
            order.setNote(req.getNote());
        }

        Random rand = new Random();
        String date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
        String code = "OD" + date + String.format("%04d", rand.nextInt(10000));
        order.setCode(code);
        if (req.getStatus() >= 1 && req.getStatus() <= 3) {
            order.setStatus(req.getStatus());
        } else {
            throw new NotFoundException(404, "Tr???ng th??i kh??ng ch??nh x??c!");
        }
        if (req.getClassId() != null && req.getClassId() != 0) {
            Class classes = classRepository.findById(req.getClassId()).orElse(null);
            if (classes == null) {
                throw new NotFoundException(404, "L???p h???c kh??ng t???n t???i");
            }
            if (req.getStatus() != 3) {
                User user = userRepository.findByEmail(req.getEmail()).orElse(null);
                if (user != null) {
                    user.setFullname(req.getFullName());
                    user.setPhoneNumber(req.getMobile());
                    userRepository.save(user);
                    order.setUser(user);
                } else {
                    if ((req.getClassId() == null || req.getClassId() == 0)) {
                        throw new BadRequestException(400, "Vui l??ng ch???n ??t nh???t 1 s???n ph???m ho???c combo");
                    }
                    Customer customer = customerRepository.findByEmail(req.getEmail()).orElse(null);
                    if (customer == null) {
                        customer = customerRepository
                                .save(new Customer(req.getFullName(), req.getEmail(), req.getMobile()));
                    }
                    customer.setFullName(req.getFullName());
                    customer.setMobile(req.getMobile());
                    customerRepository.save(customer);
                    order.setCustomer(customer);
                }
                if (order.getUser() != null) {
                    if (orderPackageRepository.checkClassExistUser(req.getClassId(), order.getUser().getId())
                            .size() > 0) {
                        throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                    }
                    if (traineeRepository.checkClassExistTraniee(req.getClassId(), order.getUser().getId())
                            .size() > 0) {
                        throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                    }
                } else if (order.getCustomer() != null) {
                    if (orderPackageRepository.checkClassExistCustomer(req.getClassId(), order.getCustomer().getId())
                            .size() > 0) {
                        throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                    }
                }
                OrderPackage item = new OrderPackage();
                item.set_package(classes.getAPackage());
                item.setOrder(order);
                item.setPackageCost(classes.getAPackage().getSalePrice());
                item.setActivated(false);
                order.setAClass(classes);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
                if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                    Coupon coupon = null;
                    try {
                        coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (coupon != null) {
                        int countUseCoupon = 0;
                        if (order.getUser() != null) {
                            countUseCoupon = orderRepository
                                    .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                    .size();
                        } else {
                            countUseCoupon = orderRepository
                                    .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                        }
                        if (countUseCoupon < coupon.getQuantity()) {
                            double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                            order.setTotalDiscount(discount);
                            order.setCoupon(coupon);
                            order.decreaseTotalCost(discount);
                        } else {
                            throw new BadRequestException(400,
                                    "M?? gi???m gi?? ???? h???t l?????t s??? d???ng ?????i v???i kh??ch h??ng n??y");
                        }
                    } else {
                        throw new BadRequestException(400, "Sai m?? gi???m gi??");
                    }
                }
                String subject = req.getStatus() == 1 ? "????ng k??" : "X??c nh???n" + " l???p h???c LRS education th??nh c??ng";
                String des = "M?? l???p h???c: " + classes.getCode() + "\n T??n kh??a h???c"
                        + classes.getAPackage().getTitle() +
                        "\n Th???i gian: t???" + classes.getDateFrom() + "\n ?????n" + classes.getDateTo() +
                        "\n L???ch h???c: " + classes.getTime() + "\n v??o c??c ng??y" + classes.getSchedule() +
                        "\n Th??ng tin thanh to??n: \n     T???ng ti???n: " + order.getTotalCost()
                        + " \n     S??? t??i kho???n: 0586986918888\n     Ng??n h??ng: qu??n ?????i MB bank\n     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";

                String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                        "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                        des, "L???p h???c", req.getStatus() == 1 ? "????ng k??" : "X??c nh???n");
                senderService.sendEmail(req.getEmail(), subject, content);
            } else {
                User user = userRepository.findByEmail(req.getEmail()).orElse(null);
                if (user == null) {
                    String pass = RandomCode.getAlphaNumericString(8).toUpperCase();
                    user = new User(req.getEmail(), "",
                            encoder.encode(encoder.encode(pass)), req.getMobile(), false);
                    user.setFullname(req.getFullName());
                    // N???u user bth kh??ng c?? set role th?? set th??nh ROLE_USER
                    Setting userRole = settingRepository.findByValueAndType("ROLE_GUEST", 1)
                            .orElseThrow(() -> new NotFoundException(404, "L???i: Role  Kh??ng t???n t???i"));

                    user.setRole(userRole);

                    String token = RandomString.make(30);
                    user.setRegisterToken(token);
                    user.setTimeRegisterToken(LocalDateTime.now());
                    userRepository.save(user);

                    String registerLink = Utility.getSiteURL(request) + "/api/account/verify?token=" + token;

                    String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";

                    String content = TemplateSendMail.getContent(registerLink, "Thanh to??n l???p h???c" + classes.getCode()
                            + " LRS education th??nh c??ng vui l??ng x??c minh t??i kho???n v?? ????ng nh???p ????? xem th??ng tin l???p h???c",
                            "Your password: " + pass);

                    senderService.sendEmail(user.getEmail(), subject, content);
                } else {
                    if (orderPackageRepository.checkClassExistUser(req.getClassId(), user.getId())
                            .size() > 0) {
                        throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                    }
                    if (traineeRepository.checkClassExistTraniee(req.getClassId(), user.getId())
                            .size() > 0) {
                        throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                    }
                    String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";
                    String des = "M?? l???p h???c: " + classes.getCode() + "\n T??n kh??a h???c"
                            + classes.getAPackage().getTitle() +
                            "\n Th???i gian: t???" + classes.getDateFrom() + "\n ?????n" + classes.getDateTo() +
                            "\n L???ch h???c: " + classes.getTime() + "\n v??o c??c ng??y" + classes.getSchedule();

                    String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                            "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                            des, "L???p h???c", "Thanh to??n");
                    user.setFullname(req.getFullName());
                    user.setPhoneNumber(req.getMobile());
                    userRepository.save(user);
                    senderService.sendEmail(user.getEmail(), subject, content);
                }
                order.setUser(user);
                OrderPackage item = new OrderPackage();
                item.set_package(classes.getAPackage());
                item.setOrder(order);
                item.setPackageCost(classes.getAPackage().getSalePrice());
                item.setActivated(false);
                order.setAClass(classes);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
                if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                    Coupon coupon = null;
                    try {
                        coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (coupon != null) {
                        int countUseCoupon = 0;
                        if (order.getUser() != null) {
                            countUseCoupon = orderRepository
                                    .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                    .size();
                        } else {
                            countUseCoupon = orderRepository
                                    .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                        }
                        if (countUseCoupon < coupon.getQuantity()) {
                            double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                            order.setTotalDiscount(discount);
                            order.setCoupon(coupon);
                            order.decreaseTotalCost(discount);
                        } else {
                            throw new BadRequestException(400,
                                    "M?? gi???m gi?? ???? h???t l?????t s??? d???ng ?????i v???i kh??ch h??ng n??y");
                        }
                    } else {
                        throw new BadRequestException(400, "Sai m?? gi???m gi??");
                    }
                }
                Trainee trainee = new Trainee();
                trainee.setUser(user);
                trainee.setStatus(false);
                trainee.setAClass(classes);
                trainee.setDropOutDate(classes.getDateTo());
                traineeRepository.save(trainee);
            }
        } else {
            User user = userRepository.findByEmail(req.getEmail()).orElse(null);
            if (user != null) {
                user.setFullname(req.getFullName());
                user.setPhoneNumber(req.getMobile());
                userRepository.save(user);
                order.setUser(user);
                order.setUser(user);
            } else {
                if (req.getPackages().size() == 0 && req.getCombos().size() == 0
                        && (req.getClassId() == null || req.getClassId() == 0)) {
                    throw new BadRequestException(400, "Vui l??ng ch???n s???n ph???m");
                }
                Customer customer = customerRepository.findByEmail(req.getEmail()).orElse(null);
                if (customer == null) {
                    customer = customerRepository
                            .save(new Customer(req.getFullName(), req.getEmail(), req.getMobile()));
                }
                order.setCustomer(customer);
            }
            String des = "Danh s??ch s???n ph???m:\n";

            for (Long id : req.getPackages()) {
                Package _package = null;
                try {
                    _package = packageRepository.findById(id).get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (_package == null) {
                    throw new NotFoundException(404, "S???n ph???m Kh??ng t???n t???i");
                }
                OrderPackage item = new OrderPackage();
                item.set_package(_package);
                item.setOrder(order);
                item.setPackageCost(_package.getSalePrice());
                if (req.getStatus() == 3) {
                    item.setActivationKey(RandomCode.getAlphaNumericString(8).toUpperCase());
                }

                if (req.getStatus() != 3) {
                    des += "\n     Kh??a h???c: " + _package.getTitle() + ", gi??: " + _package.getSalePrice() + " VN??";
                } else {
                    des += "\n     Kh??a h???c: " + _package.getTitle() + ", m?? k??ch ho???t: " + item.getActivationKey();
                }
                item.setActivated(false);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
            }

            for (Long comboId : req.getCombos()) {
                Combo combo = null;
                try {
                    combo = comboRepository.findById(comboId).get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (combo == null)
                    throw new NotFoundException(404, "Combo Kh??ng t???n t???i");
                int sumCost = 0;
                for (ComboPackage element : combo.getComboPackages()) {
                    sumCost += element.getSalePrice();
                }
                OrderPackage item = new OrderPackage();
                item.set_combo(combo);
                item.setOrder(order);
                item.setPackageCost(sumCost);
                if (req.getStatus() == 3) {
                    item.setActivationKey(RandomCode.getAlphaNumericString(8).toUpperCase());
                }
                if (req.getStatus() != 3) {
                    des += "\n     Combo: " + combo.getTitle() + ", gi??: " + sumCost + " VN??";
                } else {
                    des += "\n     Combo: " + combo.getTitle() + ", m?? k??ch ho???t: " + item.getActivationKey();
                }
                item.setActivated(false);
                order.getOrderPackages().add(item);
                order.increaseTotalCost(item.getPackageCost());
            }
            if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                Coupon coupon = null;
                try {
                    coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (coupon != null) {
                    int countUseCoupon = 0;
                    if (order.getUser() != null) {
                        countUseCoupon = orderRepository
                                .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                .size();
                    } else {
                        countUseCoupon = orderRepository
                                .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                    }
                    if (countUseCoupon < coupon.getQuantity()) {
                        double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                        order.setTotalDiscount(discount);
                        order.setCoupon(coupon);
                        order.decreaseTotalCost(discount);
                    } else {
                        throw new BadRequestException(400, "M?? gi???m gi?? ???? h???t l?????t s??? d???ng ?????i v???i kh??ch h??ng n??y");
                    }
                } else {
                    throw new BadRequestException(400, "Sai m?? gi???m gi??");
                }
            }
            if (req.getStatus() != 3) {
                String subject = req.getStatus() == 1 ? "????ng k??" : "X??c nh???n" + " kh??a h???c LRS education th??nh c??ng";
                des += "\n Th??ng tin thanh to??n: \n     T???ng ti???n: " + order.getTotalCost()
                        + " \n     S??? t??i kho???n: 0586986918888\n     Ng??n h??ng: qu??n ?????i MB bank\n     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";
                String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                        "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                        des, "Kh??a h???c", req.getStatus() == 1 ? "????ng k??" : "X??c nh???n");
                senderService.sendEmail(req.getEmail(), subject, content);
            } else {
                String subject = "Thanh to??n kh??a h???c LRS education th??nh c??ng";
                String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                        "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                        des, "Kh??a h???c", "Thanh to??n");
                senderService.sendEmail(req.getEmail(), subject, content);
            }
        }

        if (order.getOrderPackages().size() == 0) {
            throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i");
        }
        orderRepository.save(order);
    }

    @Override
    public void UpdateOrderAdmin(OrderRequestAdmin req, HttpServletRequest request, Long id) {
        if (id == null) {
            throw new NotFoundException(404, "????n h??ng kh??ng t???n t???i!");
        }
        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            throw new NotFoundException(404, "????n h??ng kh??ng t???n t???i!");
        }

        if (req.getNote() != null && req.getNote().trim().length() != 0) {
            order.setNote(req.getNote());
        }

        if (req.getStatus() >= 1 && req.getStatus() <= 4) {
            order.setStatus(req.getStatus());
        }

        if (req.getClassId() != null && req.getClassId() != 0) {
            Class classes = classRepository.findById(req.getClassId()).orElse(null);
            if (classes == null) {
                throw new NotFoundException(404, "L???p h???c kh??ng t???n t???i");
            }
            OrderPackage item = new OrderPackage();
            item.set_package(classes.getAPackage());
            item.setOrder(order);
            item.setPackageCost(classes.getAPackage().getSalePrice());
            item.setActivated(false);
            order.setAClass(classes);
            order.getOrderPackages().add(item);
            order.setTotalCost(item.getPackageCost());

            if (req.getCodeCoupon() != null && req.getCodeCoupon().trim().length() > 0) {
                Coupon coupon = null;
                try {
                    coupon = couponRepository.findByCodeAccess(req.getCodeCoupon()).orElse(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (coupon != null) {
                    int countUseCoupon = 0;
                    if (order.getUser() != null) {
                        countUseCoupon = orderRepository
                                .getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                                .size();
                    } else {
                        countUseCoupon = orderRepository
                                .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                    }
                    if (countUseCoupon < coupon.getQuantity()) {
                        double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                        order.setTotalDiscount(discount);
                        order.setCoupon(coupon);
                        order.decreaseTotalCost(discount);
                    } else {
                        throw new BadRequestException(400, "M?? gi???m gi?? ???? h???t l?????t s??? d???ng ?????i v???i kh??ch h??ng n??y");
                    }
                } else {
                    throw new BadRequestException(400, "Sai m?? gi???m gi??");
                }
            } else if (order.getCoupon() != null) {
                int countUseCoupon = 0;
                if (order.getUser() != null) {
                    countUseCoupon = orderRepository
                            .getCountCouponUserOrder(order.getCoupon().getId(), order.getUser().getId())
                            .size();
                } else {
                    countUseCoupon = orderRepository
                            .getCountCouponCustomerOrder(order.getCoupon().getId(), order.getCustomer().getId()).size();
                }
                if (countUseCoupon < order.getCoupon().getQuantity()) {
                    double discount = order.getTotalCost() * order.getCoupon().getDiscountRate() / 100;
                    order.setTotalDiscount(discount);
                    order.setCoupon(order.getCoupon());
                    order.decreaseTotalCost(discount);
                } else {
                    throw new BadRequestException(400, "M?? gi???m gi?? ???? h???t l?????t s??? d???ng ?????i v???i kh??ch h??ng n??y");
                }
            }
            if (req.getStatus() != 3) {
                if (order.getUser() != null) {
                    if (req.getMobile() != null && req.getMobile().trim().length() != 0) {
                        order.getUser().setPhoneNumber(req.getMobile());
                    }
                    if (req.getFullName() != null && req.getFullName().trim().length() != 0) {
                        order.getUser().setFullname(req.getFullName());
                    }
                    userRepository.save(order.getUser());
                } else {
                    if (req.getMobile() != null && req.getMobile().trim().length() != 0) {
                        order.getCustomer().setMobile(req.getMobile());
                    }
                    if (req.getFullName() != null && req.getFullName().trim().length() != 0) {
                        order.getCustomer().setFullName(req.getFullName());
                    }
                    customerRepository.save(order.getCustomer());
                }
                if (order.getAClass().getId() != req.getClassId()) {
                    if (order.getUser() != null) {
                        if (orderPackageRepository.checkClassExistUser(req.getClassId(), order.getUser().getId())
                                .size() > 0) {
                            throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                        }
                        if (traineeRepository.checkClassExistTraniee(req.getClassId(), order.getUser().getId())
                                .size() > 0) {
                            throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                        }
                    } else if (order.getCustomer() != null) {
                        if (orderPackageRepository
                                .checkClassExistCustomer(req.getClassId(), order.getCustomer().getId())
                                .size() > 0) {
                            throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                        }
                    }
                    String subject = req.getStatus() == 1 ? "????ng k??"
                            : "X??c nh???n" + " l???p h???c LRS education th??nh c??ng";
                    String des = "M?? l???p h???c: " + classes.getCode() + "\n T??n kh??a h???c"
                            + classes.getAPackage().getTitle() +
                            "\n Th???i gian: t???" + classes.getDateFrom() + "\n ?????n" + classes.getDateTo() +
                            "\n L???ch h???c: " + classes.getTime() + "\n v??o c??c ng??y" + classes.getSchedule() +
                            "\n Th??ng tin thanh to??n: \n     T???ng ti???n: " + order.getTotalCost()
                            + " \n     S??? t??i kho???n: 0586986918888\n     Ng??n h??ng: qu??n ?????i MB bank\n     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";

                    String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                            "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                            des, "L???p h???c", req.getStatus() == 1 ? "????ng k??" : "X??c nh???n");
                    senderService.sendEmail(req.getEmail(), subject, content);
                }
            } else {
                if (order.getUser() == null) {
                    String pass = RandomCode.getAlphaNumericString(8).toUpperCase();
                    User user = new User(order.getUser().getEmail(), "",
                            encoder.encode(encoder.encode(pass)), req.getMobile(), false);
                    user.setFullname(req.getFullName());
                    // N???u user bth kh??ng c?? set role th?? set th??nh ROLE_USER
                    Setting userRole = settingRepository.findByValueAndType("ROLE_GUEST", 1)
                            .orElseThrow(() -> new NotFoundException(404, "Error: Role  Kh??ng t???n t???i"));

                    user.setRole(userRole);

                    String token = RandomString.make(30);
                    user.setRegisterToken(token);
                    user.setTimeRegisterToken(LocalDateTime.now());
                    userRepository.save(user);

                    String registerLink = Utility.getSiteURL(request) + "/api/account/verify?token=" + token;

                    String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";

                    String content = TemplateSendMail.getContent(registerLink, "Thanh to??n l???p h???c" + classes.getCode()
                            + " LRS education th??nh c??ng vui l??ng x??c minh t??i kho???n v?? ????ng nh???p ????? xem th??ng tin l???p h???c",
                            "Your password: " + pass);

                    senderService.sendEmail(user.getEmail(), subject, content);
                    order.setUser(user);
                } else {
                    if (order.getAClass().getId() != req.getClassId()) {
                        if (orderPackageRepository.checkClassExistUser(req.getClassId(), order.getUser().getId())
                                .size() > 0) {
                            throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                        }
                        if (traineeRepository.checkClassExistTraniee(req.getClassId(), order.getUser().getId())
                                .size() > 0) {
                            throw new NotFoundException(404, "Kh??ch h??ng ???? ????ng k?? l???p n??y r???i");
                        }
                    }
                    if (req.getMobile() != null && req.getMobile().trim().length() != 0) {
                        order.getUser().setPhoneNumber(req.getMobile());
                    }
                    if (req.getFullName() != null && req.getFullName().trim().length() != 0) {
                        order.getUser().setFullname(req.getFullName());
                    }
                    userRepository.save(order.getUser());
                    order.setUser(order.getUser());
                    String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";
                    String des = "M?? l???p h???c: " + classes.getCode() + "\n T??n kh??a h???c"
                            + classes.getAPackage().getTitle() +
                            "\n Th???i gian: t???" + classes.getDateFrom() + "\n ?????n" + classes.getDateTo() +
                            "\n L???ch h???c: " + classes.getTime() + "\n v??o c??c ng??y" + classes.getSchedule();

                    String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                            "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                            des, "L???p h???c", "Thanh to??n");
                    senderService.sendEmail(order.getUser().getEmail(), subject, content);
                }
                Trainee trainee = new Trainee();
                trainee.setUser(order.getUser());
                trainee.setStatus(false);
                trainee.setAClass(classes);
                trainee.setDropOutDate(classes.getDateTo());
                traineeRepository.save(trainee);
            }

        } else {
            if (order.getUser() != null) {
                if (req.getMobile() != null && req.getMobile().trim().length() != 0) {
                    order.getUser().setPhoneNumber(req.getMobile());
                }
                if (req.getFullName() != null && req.getFullName().trim().length() != 0) {
                    order.getUser().setFullname(req.getFullName());
                }
                userRepository.save(order.getUser());
            } else {
                if (req.getMobile() != null && req.getMobile().trim().length() != 0) {
                    order.getCustomer().setMobile(req.getMobile());
                }
                if (req.getFullName() != null && req.getFullName().trim().length() != 0) {
                    order.getCustomer().setFullName(req.getFullName());
                }
                customerRepository.save(order.getCustomer());
            }
            if (req.getStatus() == 3) {
                String des = "Danh s??ch s???n ph???m:\n";
                for (OrderPackage op : order.getOrderPackages()) {
                    if (op.getActivationKey() != null || op.isActivated())
                        continue;

                    String code = null;
                    if (order.getAClass() == null) {
                        code = RandomCode.getAlphaNumericString(8).toUpperCase();
                        op.setActivationKey(code);
                    }
                    if (op.get_package() != null) {
                        des += "\n     Kh??a h???c: " + op.get_package().getTitle() + ", m?? k??ch ho???t: " + code;
                    } else {
                        des += "\n     Kh??a h???c: " + op.get_combo().getTitle() + ", m?? k??ch ho???t: " + code;
                    }
                    try {
                        orderPackageRepository.save(op);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                String subject = "Thanh to??n kh??a h???c LRS education th??nh c??ng";
                String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                        "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                        des, "Kh??a h???c", "Thanh to??n");
                senderService.sendEmail(req.getEmail(), subject, content);
            }
        }
        if (order.getOrderPackages().size() == 0) {
            throw new NotFoundException(404, "S???n ph???m kh??ng t???n t???i");
        }
        orderRepository.save(order);
    }

    @Override
    public Page<Order> manageGetList(Pageable paging) {
        return orderRepository.findAll(paging);
    }

    @Override
    public Page<Order> getListOrder(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        return orderRepository.getMyOrder(user.getId(), pageable);
    }

    @Override
    public Page<Order> getListOrderCancel(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        return orderRepository.getMyOrderCancel(user.getId(), pageable);
    }

    @Override
    public Page<Order> getListOrderProcess(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        return orderRepository.getMyOrderProcess(user.getId(), pageable);
    }

    @Override
    public Order getDetail(Long id) {
        Order order = null;
        try {
            order = orderRepository.findById(id).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (order == null) {
            throw new NotFoundException(404, "Order  Kh??ng t???n t???i");
        }

        return order;
    }

    @Override
    public void updateStatus(Long id, Integer status, HttpServletRequest request) {
        Order order = null;
        try {
            order = orderRepository.findById(id).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (order == null) {
            throw new NotFoundException(404, "????n h??ng Kh??ng t???n t???i");
        }

        if (status == null)
            throw new BadRequestException(400, "Vui l??ng nh???p tr???ng th??i");
        if (status == 3) {
            for (OrderPackage op : order.getOrderPackages()) {
                if (op.getActivationKey() != null || op.isActivated())
                    continue;

                String code = null;
                if (order.getAClass() == null) {
                    code = RandomCode.getAlphaNumericString(8).toUpperCase();
                    op.setActivationKey(code);
                }
                try {
                    orderPackageRepository.save(op);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                if (order.getCustomer() != null) {
                    if (order.getAClass() != null) {
                        User user = userRepository.findByEmail(order.getCustomer().getEmail()).orElse(null);
                        if (user == null) {
                            String pass = RandomCode.getAlphaNumericString(8).toUpperCase();
                            user = new User(order.getCustomer().getEmail(), "",
                                    encoder.encode(encoder.encode(pass)), order.getCustomer().getMobile(), false);
                            user.setFullname(order.getCustomer().getFullName());
                            // N???u user bth kh??ng c?? set role th?? set th??nh ROLE_USER
                            Setting userRole = settingRepository.findByValueAndType("ROLE_GUEST", 1)
                                    .orElseThrow(() -> new NotFoundException(404, "Error: Role  Kh??ng t???n t???i"));

                            user.setRole(userRole);

                            String token = RandomString.make(30);

                            user.setRegisterToken(token);
                            user.setTimeRegisterToken(LocalDateTime.now());
                            userRepository.save(user);

                            String registerLink = Utility.getSiteURL(request) + "/api/account/verify?token=" + token;

                            String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";

                            String content = TemplateSendMail.getContent(registerLink, "Thanh to??n l???p h???c"
                                    + order.getAClass().getCode()
                                    + " LRS education th??nh c??ng vui l??ng x??c minh t??i kho???n v?? ????ng nh???p ????? xem th??ng tin l???p h???c",
                                    "Your password: " + pass);

                            senderService.sendEmail(user.getEmail(), subject, content);
                        } else {
                            String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";
                            String des = "M?? l???p h???c: " + order.getAClass().getCode() + "\n T??n kh??a h???c"
                                    + order.getAClass().getAPackage().getTitle() +
                                    "\n Th???i gian: t???" + order.getAClass().getDateFrom() + "\n ?????n"
                                    + order.getAClass().getDateTo() +
                                    "\n L???ch h???c: " + order.getAClass().getTime() + "\n v??o c??c ng??y"
                                    + order.getAClass().getSchedule();

                            String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                                    "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                                    des, "L???p h???c", "Thanh to??n");
                            senderService.sendEmail(user.getEmail(), subject, content);
                        }

                        Trainee trainee = new Trainee();
                        trainee.setUser(user);
                        trainee.setStatus(false);
                        trainee.setAClass(order.getAClass());
                        trainee.setDropOutDate(order.getAClass().getDateTo());
                        traineeRepository.save(trainee);
                    } else {
                        String subject = "Thanh to??n kh??a h???c LRS education th??nh c??ng";
                        String des = "   Kh??a h???c: " + (op.get_package() != null ? op.get_package().getTitle() : op.get_combo().getTitle()) + ", m?? k??ch ho???t: " + code;

                        String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                                "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                                des, "Kh??a h???c", "Thanh to??n");
                        senderService.sendEmail(order.getUser().getEmail(), subject, content);
                    }
                } else {
                    if (order.getAClass() != null) {
                        Trainee trainee = new Trainee();
                        trainee.setUser(order.getUser());
                        trainee.setStatus(false);
                        trainee.setAClass(order.getAClass());
                        trainee.setDropOutDate(order.getAClass().getDateTo());
                        traineeRepository.save(trainee);

                        String subject = "Thanh to??n l???p h???c LRS education th??nh c??ng";
                        String des = "M?? l???p h???c: " + order.getAClass().getCode() + "\n T??n kh??a h???c"
                                + order.getAClass().getAPackage().getTitle() +
                                "\n Th???i gian: t???" + order.getAClass().getDateFrom() + "\n ?????n"
                                + order.getAClass().getDateTo() +
                                "\n L???ch h???c: " + order.getAClass().getTime() + "\n v??o c??c ng??y"
                                + order.getAClass().getSchedule();

                        String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                                "Sau ????y l?? th??ng tin l???p h???c h???c c???a b???n:",
                                des, "L???p h???c", "Thanh to??n");
                        senderService.sendEmail(order.getUser().getEmail(), subject, content);
                    } else {
                        String subject = "Thanh to??n kh??a h???c LRS education th??nh c??ng";
                        String des = "   Kh??a h???c: " + (op.get_package() != null ? op.get_package().getTitle() : op.get_combo().getTitle() )+ ", m?? k??ch ho???t: " + code;

                        String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                                "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                                des, "Kh??a h???c", "Thanh to??n");
                        senderService.sendEmail(order.getUser().getEmail(), subject, content);
                    }
                }
            }
        }
        if (status == 4) {
            for (OrderPackage op : order.getOrderPackages()) {
                if (op.isActivated()) {
                    throw new BadRequestException(400,
                            "Kh??a h???c c?? trong ????n ???? k??ch ho???t, kh??ng th??? thai ?????i tr???ng th??i n??y n???a");
                }
                op.setActivationKey(null);
                try {
                    orderPackageRepository.save(op);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        order.setStatus(status);
        order.setUpdatedDate(LocalDateTime.now().toString());
        orderRepository.save(order);
    }

    @Override
    public void pay(String code, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        Order order = null;
        try {
            order = orderRepository.getMyCart(user.getId()).orElse(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (order == null)
            throw new NotFoundException(404, "Gi??? h??ng tr???ng");

        if (code != null && code.trim().length() > 0) {
            Coupon coupon = null;
            try {
                coupon = couponRepository.findByCodeAccess(code).orElse(null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (coupon != null) {
                int countUseCoupon = 0;
                if (order.getUser() != null) {
                    countUseCoupon = orderRepository.getCountCouponUserOrder(coupon.getId(), order.getUser().getId())
                            .size();
                } else {
                    countUseCoupon = orderRepository
                            .getCountCouponCustomerOrder(coupon.getId(), order.getCustomer().getId()).size();
                }
                if (countUseCoupon < coupon.getQuantity()) {
                    double discount = order.getTotalCost() * coupon.getDiscountRate() / 100;
                    order.setTotalDiscount(discount);
                    order.setCoupon(coupon);
                    order.setTotalCost(order.getTotalCost() - discount);
                } else {
                    throw new BadRequestException(400, "B???n ???? h???t l?????t s??? d???ng m?? n??y");
                }
            } else {
                throw new BadRequestException(400, "M?? gi???m gi?? c???a b???n kh??ng ????ng ho???c ???? h???t h???n");
            }
        }
        String des = "Danh s??ch s???n ph???m:\n";
        for (OrderPackage op : order.getOrderPackages()) {
            if (op.get_package() != null) {
                des += "\n     Kh??a h???c: " + op.get_package().getTitle() + ", gi??: " + op.getPackageCost() + " VN??";
            } else {
                des += "\n     Combo: " + op.get_package().getTitle() + ", gi??: " + op.getPackageCost() + " VN??";
            }
        }
        String subject = "????ng k??  kh??a h???c LRS education th??nh c??ng";
        des += "\n Th??ng tin thanh to??n: \n     T???ng ti???n: " + order.getTotalCost()
                + " \n     S??? t??i kho???n: 0586986918888\n     Ng??n h??ng: qu??n ?????i MB bank\n     t??n ch??? t??i kho???n: Nguy???n V??n H??ng";
        String content = TemplateSendMail.getContentCourse("https://lms.nextin.com.vn",
                "Sau ????y l?? th??ng tin kh??a h???c h???c h???c c???a b???n:",
                des, "Kh??a h???c", "????ng k??");
        senderService.sendEmail(user.getEmail(), subject, content);
        order.setStatus(1);
        order.setCreatedDate(LocalDateTime.now().toString());
        order.setUpdatedDate(LocalDateTime.now().toString());
        orderRepository.save(order);
    }

    @Override
    public Page<Order> getListOrderOffline(Integer status, String keyword, Pageable paging) {
        Setting role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority rolee : auth.getAuthorities()) {
            role = settingRepository.findByValueAndType(rolee.getAuthority(), 1).get();
        }
        if (!role.getSetting_value().equals("ROLE_SUPPORTER")) {
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOfflineNotPay(paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOfflineNotPayKeyword(keyword, paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOfflineNotPayByStatus(status, paging);
            } else {
                return orderRepository.getListOfflineNotPayKeywordAndStatus(keyword, status, paging);
            }
        } else {
            User supporter = userRepository.findByUsername(auth.getName());
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOfflineNotPayRoleSupporter(supporter.getId(), paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOfflineNotPayKeywordRoleSupporter(keyword, supporter.getId(), paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOfflineNotPayByStatusRoleSupporter(status, supporter.getId(), paging);
            } else {
                return orderRepository.getListOfflineNotPayKeywordAndStatusRoleSupporter(keyword, status,
                        supporter.getId(), paging);
            }
        }
    }

    @Override
    public Page<Order> getListOrderOnline(Integer status, String keyword, Pageable paging) {
        Setting role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority rolee : auth.getAuthorities()) {
            role = settingRepository.findByValueAndType(rolee.getAuthority(), 1).get();
        }
        if (!role.getSetting_value().equals("ROLE_SUPPORTER")) {
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOnlineNotPay(paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOnlineNotPayKeyword(keyword, paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOnlineNotPayByStatus(status, paging);
            } else {
                return orderRepository.getListOnlineNotPayKeywordAndStatus(keyword, status, paging);
            }
        } else {
            User supporter = userRepository.findByUsername(auth.getName());
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOnlineNotPayRoleSupporter(supporter.getId(), paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOnlineNotPayKeywordRoleSupporter(keyword, supporter.getId(), paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOnlineNotPayByStatusRoleSupporter(status, supporter.getId(), paging);
            } else {
                return orderRepository.getListOnlineNotPayKeywordAndStatusRoleSupporter(keyword, status,
                        supporter.getId(), paging);
            }
        }
    }

    @Override
    public Page<Order> getListOrdered(Integer status, String keyword, Pageable paging) {
        Setting role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority rolee : auth.getAuthorities()) {
            role = settingRepository.findByValueAndType(rolee.getAuthority(), 1).get();
        }
        if (!role.getSetting_value().equals("ROLE_SUPPORTER")) {
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOrdered(paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOrderedKeyword(keyword, paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOrderedByStatus(status, paging);
            } else {
                return orderRepository.getListOrderedKeywordAndStatus(keyword, status, paging);
            }
        } else {
            User supporter = userRepository.findByUsername(auth.getName());
            if ((status == null || status == 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOrderedRoleSupporter(supporter.getId(), paging);
            } else if ((status == null || status == 0) && (keyword != null && keyword.length() > 0)) {
                return orderRepository.getListOrderedKeywordRoleSupporter(keyword, supporter.getId(), paging);
            } else if ((status != null && status != 0) && (keyword == null || keyword.length() == 0)) {
                return orderRepository.getListOrderedByStatusRoleSupporter(status, supporter.getId(), paging);
            } else {
                return orderRepository.getListOrderedKeywordAndStatusRoleSupporter(keyword, status, supporter.getId(),
                        paging);
            }
        }
    }
}