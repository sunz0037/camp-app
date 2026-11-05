package com.camp.app.service;

import com.camp.app.api.PageResult;
import com.camp.app.model.AnnouncementVo;

public interface AnnouncementService {
    PageResult<AnnouncementVo> list(long page, long size, String category, Integer status, Integer isTop, Integer isUrgent, String keyword);

    long unreadCount();

    AnnouncementVo detail(Long id);

    void markRead(Long id);

    AnnouncementVo create(AnnouncementVo vo);

    AnnouncementVo update(Long id, AnnouncementVo vo);

    void top(Long id, Integer isTop);

    void withdraw(Long id);

    void delete(Long id);
}
