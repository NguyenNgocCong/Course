package com.courses.server.repositories;

import com.courses.server.entity.Order;
import com.courses.server.entity.OrderPackage;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface OrderPackageRepository extends JpaRepository<OrderPackage, Long> {
    Page<OrderPackage> findAllByOrder(Order order, Pageable pageable);

    List<OrderPackage> findAllByOrder(Order order);

    @Query(value = "SELECT * FROM order_package where activation_key = ?1",nativeQuery = true) // if want to write nativequery then mask nativeQuery  as true
    OrderPackage findByActivationKey(String code);

    @Modifying 
    @Query(value = "SELECT op.* FROM courses.`order` as o join order_package as op on o.id = op.order_id where o.customer_id = ?2 and o.class_id = ?1",nativeQuery = true) // if want to write nativequery then mask nativeQuery  as true
    List<OrderPackage> checkClassExistCustomer(Long class_id, Long customer_id);

    @Modifying 
    @Query(value = "SELECT op.* FROM courses.`order` as o join order_package as op on o.id = op.order_id where o.user_id = ?2 and o.class_id = ?1",nativeQuery = true) // if want to write nativequery then mask nativeQuery  as true
    List<OrderPackage> checkClassExistUser(Long class_id, Long userId);

    @Modifying 
    @Query(value = "DELETE FROM courses.order_package WHERE id = ?1",nativeQuery = true) 
    int deleteProduct(Long id); 

    @Query(value="SELECT op.* FROM courses.`order` as o join order_package as op on o.id = op.order_id where o.status = 3", nativeQuery=true)
    List<OrderPackage> countSoldOut();

    @Query(value="SELECT * FROM courses.order_package WHERE order_id = ?1", nativeQuery=true)
    List<OrderPackage> countProduct(Long order_id);

}