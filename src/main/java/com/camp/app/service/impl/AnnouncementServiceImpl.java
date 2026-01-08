package com.camp.app.service.impl;

import com.camp.app.api.PageResult;
import com.camp.app.entity.Announcement;
import com.camp.app.model.AnnouncementVo;
import com.camp.app.repository.AnnouncementRepository;
import com.camp.app.service.AnnouncementService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    @Override
    public PageResult<AnnouncementVo> list(long page, long size, String category, Integer status, Integer isTop, Integer isUrgent, String keyword) {
        Pageable pageable = PageRequest.of((int) (page - 1), (int) size);
        Page<Announcement> announcementPage = announcementRepository.findByFilters(category, status, isTop, isUrgent, keyword, pageable);
        List<AnnouncementVo> voList = BeanMapper.mapList(announcementPage.getContent(), AnnouncementVo.class);
        
        return PageResult.<AnnouncementVo>builder()
                .page(page)
                .size(size)
                .total(announcementPage.getTotalElements())
                .records(voList)
                .build();
    }

    @Override
    public AnnouncementVo detail(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElse(null);
        if (announcement != null) {
            announcementRepository.incrementViewCount(id);
            return BeanMapper.map(announcement, AnnouncementVo.class);
        }
        return null;
    }

    @Override
    @Transactional
    public void create(AnnouncementVo vo) {
        Announcement announcement = BeanMapper.map(vo, Announcement.class);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void update(Long id, AnnouncementVo vo) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        BeanMapper.copyProperties(vo, announcement);
        announcement.setId(id);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void top(Long id, Integer isTop) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        announcement.setIsTop(isTop);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void withdraw(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        announcement.setStatus(2); // 已撤回
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }
}

