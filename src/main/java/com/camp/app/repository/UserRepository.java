package com.camp.app.repository;

import com.camp.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    /**
     * 根据用户名和用户类型查询用户（用于教工工号登录）
     */
    Optional<User> findByUsernameAndUserType(String username, Integer userType);
}

