package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.api.PageResult;
import com.camp.app.entity.Announcement;
import com.camp.app.entity.AnnouncementRead;
import com.camp.app.entity.AnnouncementScope;
import com.camp.app.model.AnnouncementVo;
import com.camp.app.model.DataScope;
import com.camp.app.repository.AnnouncementReadRepository;
import com.camp.app.repository.AnnouncementRepository;
import com.camp.app.repository.AnnouncementScopeRepository;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.AccessService;
import com.camp.app.service.AnnouncementService;
import com.camp.app.util.BeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final AnnouncementScopeRepository announcementScopeRepository;
    private final AccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AnnouncementVo> list(long page, long size, String category, Integer status, Integer isTop, Integer isUrgent, String keyword) {
        DataScope scope = accessService.current();
        Integer effectiveStatus = status;
        if (effectiveStatus == null && !scope.isAll()) {
            effectiveStatus = 1;
        }
        List<Announcement> rows = announcementRepository
                .findByFilters(category, effectiveStatus, isTop, isUrgent, keyword, PageRequest.of(0, 500))
                .getContent();
        List<Announcement> visible = rows.stream().filter(row -> visible(row, scope)).toList();
        int from = (int) Math.min((page - 1) * size, visible.size());
        int to = (int) Math.min(from + size, visible.size());
        List<AnnouncementVo> items = visible.subList(from, to).stream().map(row -> toVo(row, false)).toList();
        return PageResult.<AnnouncementVo>builder().page(page).size(size).total(visible.size()).records(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        DataScope scope = accessService.current();
        Long userId = scope.getUserId();
        List<Announcement> visible = announcementRepository
                .findByFilters(null, 1, null, null, null, PageRequest.of(0, 500))
                .getContent().stream().filter(row -> visible(row, scope)).toList();
        if (userId == null || visible.isEmpty()) {
            return visible.size();
        }
        List<Long> ids = visible.stream().map(Announcement::getId).toList();
        long read = announcementReadRepository.countByUserIdAndAnnouncementIdIn(userId, ids);
        return Math.max(0, ids.size() - read);
    }

    @Override
    @Transactional
    public AnnouncementVo detail(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("公告不存在"));
        DataScope scope = accessService.current();
        if (!visible(announcement, scope)) {
            throw BizException.forbidden("不在公告发布范围内");
        }
        announcementRepository.incrementViewCount(id);
        if (scope.getUserId() != null) {
            announcementReadRepository.findByAnnouncementIdAndUserId(id, scope.getUserId()).orElseGet(() -> {
                AnnouncementRead read = new AnnouncementRead();
                read.setAnnouncementId(id);
                read.setUserId(scope.getUserId());
                return announcementReadRepository.save(read);
            });
        }
        AnnouncementVo vo = toVo(announcement, true);
        vo.setIsRead(true);
        return vo;
    }

    @Override
    @Transactional
    public void create(AnnouncementVo vo) {
        if (vo.getTitle() == null || vo.getContent() == null) {
            throw BizException.badRequest("标题和内容不能为空");
        }
        assertCanPublish(vo);
        Announcement announcement = BeanMapper.map(vo, Announcement.class);
        if (announcement.getPublisherId() == null) {
            announcement.setPublisherId(AuthContext.userId());
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        announcementRepository.save(announcement);
        replaceScopes(announcement.getId(), vo.getScopeType() == null ? "ALL" : vo.getScopeType(), vo.getScopeIds());
    }

    @Override
    @Transactional
    public void update(Long id, AnnouncementVo vo) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("公告不存在"));
        assertCanEdit(announcement);
        if (vo.getScopeType() != null || vo.getScopeIds() != null) {
            ScopeView current = scopeView(announcement.getId());
            AnnouncementVo check = new AnnouncementVo();
            check.setScopeType(vo.getScopeType() == null ? current.type() : vo.getScopeType());
            check.setScopeIds(vo.getScopeIds() == null ? current.ids() : vo.getScopeIds());
            assertCanPublish(check);
        }
        if (vo.getTitle() != null) {
            announcement.setTitle(vo.getTitle());
        }
        if (vo.getContent() != null) {
            announcement.setContent(vo.getContent());
        }
        if (vo.getCategory() != null) {
            announcement.setCategory(vo.getCategory());
        }
        if (vo.getIsTop() != null) {
            announcement.setIsTop(vo.getIsTop());
        }
        if (vo.getIsUrgent() != null) {
            announcement.setIsUrgent(vo.getIsUrgent());
        }
        if (vo.getExpireTime() != null) {
            announcement.setExpireTime(vo.getExpireTime());
        }
        if (vo.getStatus() != null) {
            announcement.setStatus(vo.getStatus());
        }
        if (vo.getAttachmentUrl() != null) {
            announcement.setAttachmentUrl(vo.getAttachmentUrl());
        }
        if (vo.getScopeType() != null || vo.getScopeIds() != null) {
            ScopeView current = scopeView(announcement.getId());
            replaceScopes(announcement.getId(),
                    vo.getScopeType() == null ? current.type() : vo.getScopeType(),
                    vo.getScopeIds() == null ? current.ids() : vo.getScopeIds());
        }
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void top(Long id, Integer isTop) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("公告不存在"));
        announcement.setIsTop(isTop);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void withdraw(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("公告不存在"));
        assertCanEdit(announcement);
        announcement.setStatus(2);
        announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("公告不存在"));
        AuthPrincipal principal = AuthContext.get();
        boolean admin = principal != null && principal.roles() != null && principal.roles().contains(Phase1Roles.ADMIN);
        boolean author = principal != null && principal.userId() != null && principal.userId().equals(announcement.getPublisherId());
        if (!admin && !author) {
            throw BizException.forbidden("仅管理员或作者可删除公告");
        }
        announcementRepository.deleteById(id);
    }

    private void assertCanPublish(AnnouncementVo vo) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            return;
        }
        String highest = Phase1Roles.highest(principal.roles());
        String scopeType = vo.getScopeType() == null ? "ALL" : vo.getScopeType();
        if (!Set.of("ALL", "GRADE", "CLASS").contains(scopeType)) {
            throw BizException.badRequest("scopeType 必须是 ALL、GRADE 或 CLASS");
        }
        DataScope scope = accessService.current();
        if (Phase1Roles.TEACHER.equals(highest)) {
            if (!"CLASS".equals(scopeType) || vo.getScopeIds() == null || vo.getScopeIds().isEmpty()) {
                throw BizException.forbidden("教师只能向本人班级发布公告");
            }
            if (scope.getClassIds() == null || !scope.getClassIds().containsAll(vo.getScopeIds())) {
                throw BizException.forbidden("公告范围超出所授班级");
            }
            return;
        }
        if (Phase1Roles.ACADEMIC.equals(highest) && "GRADE".equals(scopeType)) {
            if (vo.getScopeIds() == null || scope.getGradeIds() == null || !scope.getGradeIds().containsAll(vo.getScopeIds())) {
                throw BizException.forbidden("公告范围超出授权年级");
            }
        }
        if (Phase1Roles.ACADEMIC.equals(highest) && "CLASS".equals(scopeType)) {
            if (vo.getScopeIds() == null || scope.getClassIds() == null || !scope.getClassIds().containsAll(vo.getScopeIds())) {
                throw BizException.forbidden("公告范围超出授权班级");
            }
        }
    }

    private void assertCanEdit(Announcement announcement) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            return;
        }
        boolean elevated = principal.roles().contains(Phase1Roles.ADMIN) || principal.roles().contains(Phase1Roles.ACADEMIC);
        boolean author = principal.userId() != null && principal.userId().equals(announcement.getPublisherId());
        if (!elevated && !author) {
            throw BizException.forbidden("仅作者、教务或管理员可修改该公告");
        }
    }

    private boolean visible(Announcement announcement, DataScope scope) {
        if (announcement.getStatus() != null && announcement.getStatus() == 2 && !scope.isAll()) {
            AuthPrincipal principal = AuthContext.get();
            boolean author = principal != null && principal.userId() != null && principal.userId().equals(announcement.getPublisherId());
            if (!author) {
                return false;
            }
        }
        ScopeView current = scopeView(announcement.getId());
        String scopeType = current.type();
        if (scope.isAll() || scopeType == null || scopeType.isBlank() || "ALL".equals(scopeType)) {
            return true;
        }
        List<Long> ids = current.ids();
        if ("GRADE".equals(scopeType)) {
            return intersects(ids, scope.getGradeIds());
        }
        if ("CLASS".equals(scopeType)) {
            return intersects(ids, scope.getClassIds());
        }
        return false;
    }

    private AnnouncementVo toVo(Announcement announcement, boolean ignoreReadLookup) {
        AnnouncementVo vo = BeanMapper.map(announcement, AnnouncementVo.class);
        ScopeView current = scopeView(announcement.getId());
        vo.setScopeType(current.type());
        vo.setScopeIds(current.ids());
        DataScope scope = accessService.current();
        if (!ignoreReadLookup && scope.getUserId() != null && announcement.getId() != null) {
            vo.setIsRead(announcementReadRepository.findByAnnouncementIdAndUserId(announcement.getId(), scope.getUserId()).isPresent());
        } else if (scope.getUserId() == null) {
            vo.setIsRead(false);
        }
        return vo;
    }

    private boolean intersects(List<Long> left, List<Long> right) {
        if (left == null || right == null) {
            return false;
        }
        Set<Long> set = new HashSet<>(right);
        for (Long id : left) {
            if (set.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private record ScopeView(String type, List<Long> ids) {
    }

    private ScopeView scopeView(Long announcementId) {
        if (announcementId == null) {
            return new ScopeView("ALL", List.of());
        }
        List<AnnouncementScope> rows = announcementScopeRepository.findByAnnouncementId(announcementId);
        if (rows.isEmpty() || rows.stream().anyMatch(row -> row.getScopeType() != null && row.getScopeType() == 1)) {
            return new ScopeView("ALL", List.of());
        }
        if (rows.stream().anyMatch(row -> row.getScopeType() != null && row.getScopeType() == 2)) {
            return new ScopeView("GRADE", rows.stream().map(AnnouncementScope::getScopeId).filter(id -> id != null && id > 0).toList());
        }
        return new ScopeView("CLASS", rows.stream().map(AnnouncementScope::getScopeId).filter(id -> id != null && id > 0).toList());
    }

    private void replaceScopes(Long announcementId, String scopeType, List<Long> scopeIds) {
        announcementScopeRepository.deleteByAnnouncementId(announcementId);
        String type = scopeType == null ? "ALL" : scopeType;
        if ("ALL".equals(type) || scopeIds == null || scopeIds.isEmpty()) {
            AnnouncementScope row = new AnnouncementScope();
            row.setAnnouncementId(announcementId);
            row.setScopeType(1);
            row.setScopeId(0L);
            announcementScopeRepository.save(row);
            return;
        }
        int code = "GRADE".equals(type) ? 2 : 3;
        for (Long scopeId : scopeIds) {
            AnnouncementScope row = new AnnouncementScope();
            row.setAnnouncementId(announcementId);
            row.setScopeType(code);
            row.setScopeId(scopeId);
            announcementScopeRepository.save(row);
        }
    }
}
