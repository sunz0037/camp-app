package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.User;
import com.camp.app.model.UserVo;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.UserService;
import com.camp.app.util.BeanMapper;
import com.camp.app.util.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public PageResult<UserVo> list(long page, long size, Integer userType, Integer status, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (userType != null) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate usernamePredicate = cb.like(root.get("username"), likePattern);
                Predicate realNamePredicate = cb.like(root.get("realName"), likePattern);
                predicates.add(cb.or(usernamePredicate, realNamePredicate));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<User> userPage = userRepository.findAll(spec, pageable);
        
        List<UserVo> voList = userPage.getContent().stream()
            .map(this::toVo)
            .collect(Collectors.toList());
        
        return PageResult.<UserVo>builder()
            .records(voList)
            .total(userPage.getTotalElements())
            .page(page)
            .size(size)
            .build();
    }
    
    @Override
    public UserVo detail(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toVo(user);
    }
    
    @Override
    @Transactional
    public void create(UserVo userVo) {
        // 验证用户名唯一性
        if (userRepository.findByUsername(userVo.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        BeanMapper.copyProperties(userVo, user);
        
        // 如果提供了密码，进行加密
        if (StringUtils.hasText(userVo.getPassword())) {
            user.setPassword(PasswordEncoderUtil.encode(userVo.getPassword()));
        } else {
            // 默认密码：身份证后六位或123456
            String defaultPassword = StringUtils.hasText(userVo.getIdCard()) && userVo.getIdCard().length() >= 6
                ? userVo.getIdCard().substring(userVo.getIdCard().length() - 6)
                : "123456";
            user.setPassword(PasswordEncoderUtil.encode(defaultPassword));
        }
        
        // 设置默认状态
        if (userVo.getStatus() == null) {
            user.setStatus(1); // 默认启用
        }
        
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void update(Long id, UserVo userVo) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证用户名唯一性（排除自己）
        if (StringUtils.hasText(userVo.getUsername()) && !user.getUsername().equals(userVo.getUsername())) {
            if (userRepository.findByUsername(userVo.getUsername()).isPresent()) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(userVo.getUsername());
        }
        
        // 更新其他字段
        if (StringUtils.hasText(userVo.getRealName())) {
            user.setRealName(userVo.getRealName());
        }
        if (StringUtils.hasText(userVo.getIdCard())) {
            user.setIdCard(userVo.getIdCard());
        }
        if (StringUtils.hasText(userVo.getPhone())) {
            user.setPhone(userVo.getPhone());
        }
        if (StringUtils.hasText(userVo.getEmail())) {
            user.setEmail(userVo.getEmail());
        }
        if (userVo.getUserType() != null) {
            user.setUserType(userVo.getUserType());
        }
        if (userVo.getStatus() != null) {
            user.setStatus(userVo.getStatus());
        }
        if (StringUtils.hasText(userVo.getAvatarUrl())) {
            user.setAvatarUrl(userVo.getAvatarUrl());
        }
        
        // 如果提供了新密码，进行加密更新
        if (StringUtils.hasText(userVo.getPassword())) {
            user.setPassword(PasswordEncoderUtil.encode(userVo.getPassword()));
        }
        
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        userRepository.delete(user);
    }
    
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }
    
    /**
     * 转换为VO
     */
    private UserVo toVo(User user) {
        UserVo vo = new UserVo();
        BeanMapper.copyProperties(user, vo);
        // 不返回密码
        vo.setPassword(null);
        return vo;
    }
}
