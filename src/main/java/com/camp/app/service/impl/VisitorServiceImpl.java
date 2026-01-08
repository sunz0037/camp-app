package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.VisitorRegistration;
import com.camp.app.model.VisitorRegistrationVo;
import com.camp.app.repository.VisitorRegistrationRepository;
import com.camp.app.service.VisitorService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorServiceImpl implements VisitorService {
    private final VisitorRegistrationRepository visitorRegistrationRepository;

    @Override
    public PageResult<VisitorRegistrationVo> list(long page, long size, String visitDate, Integer status, Long visitTargetUserId, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        LocalDate date = visitDate != null ? LocalDate.parse(visitDate) : null;
        Page<VisitorRegistration> visitorPage = visitorRegistrationRepository.findByFilters(date, status, visitTargetUserId, keyword, pageable);
        List<VisitorRegistrationVo> voList = BeanMapper.mapList(visitorPage.getContent(), VisitorRegistrationVo.class);
        
        return PageResult.<VisitorRegistrationVo>builder()
                .page(page)
                .size(size)
                .total(visitorPage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public VisitorRegistrationVo detail(Long id) {
        return visitorRegistrationRepository.findById(id)
                .map(visitor -> BeanMapper.map(visitor, VisitorRegistrationVo.class))
                .orElse(null);
    }

    @Override
    @Transactional
    public void create(VisitorRegistrationVo vo) {
        VisitorRegistration visitor = BeanMapper.map(vo, VisitorRegistration.class);
        visitorRegistrationRepository.save(visitor);
    }

    @Override
    @Transactional
    public void update(Long id, VisitorRegistrationVo vo) {
        VisitorRegistration visitor = visitorRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitorRegistration not found: " + id));
        BeanMapper.copyProperties(vo, visitor);
        visitor.setId(id);
        visitorRegistrationRepository.save(visitor);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        visitorRegistrationRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void approve(Long id, Integer status, String remark) {
        VisitorRegistration visitor = visitorRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitorRegistration not found: " + id));
        visitor.setStatus(status != null ? status : 1);
        if (remark != null) {
            visitor.setRemark(remark);
        }
        visitorRegistrationRepository.save(visitor);
    }

    @Override
    @Transactional
    public void reject(Long id, String remark) {
        VisitorRegistration visitor = visitorRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitorRegistration not found: " + id));
        visitor.setStatus(4); // 已拒绝
        if (remark != null) {
            visitor.setRemark(remark);
        }
        visitorRegistrationRepository.save(visitor);
    }

    @Override
    @Transactional
    public void entry(Long id, String entryTime) {
        VisitorRegistration visitor = visitorRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitorRegistration not found: " + id));
        visitor.setEntryTime(LocalDateTime.parse(entryTime));
        visitor.setStatus(2); // 已进入
        visitorRegistrationRepository.save(visitor);
    }

    @Override
    @Transactional
    public void exit(Long id, String exitTime) {
        VisitorRegistration visitor = visitorRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitorRegistration not found: " + id));
        visitor.setExitTime(LocalDateTime.parse(exitTime));
        visitor.setStatus(3); // 已离开
        visitorRegistrationRepository.save(visitor);
    }
}

