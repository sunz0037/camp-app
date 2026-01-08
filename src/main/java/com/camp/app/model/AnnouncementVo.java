package com.camp.app.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementVo {
    private Long id;
    private String title;
    private String content;
    private String category;
    private Long publisherId;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Integer isTop;
    private Integer isUrgent;
    private String targetAudience;
    private Integer viewCount;
    /**
     * 0-草稿，1-已发布，2-已撤回
     */
    private Integer status;
    private String attachmentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

