package com.camp.app.service.impl;

import com.camp.app.api.BizException;
import com.camp.app.entity.UserPreference;
import com.camp.app.model.UserSettingsVo;
import com.camp.app.phase2.Phase2Access;
import com.camp.app.phase2.Phase2Codes;
import com.camp.app.repository.UserPreferenceRepository;
import com.camp.app.repository.UserRepository;
import com.camp.app.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final Phase2Access access;

    @Override
    @Transactional(readOnly = true)
    public UserSettingsVo getMine() {
        Long userId = access.userId();
        userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"));
        String theme = userPreferenceRepository.findByUserId(userId)
                .map(UserPreference::getThemeKey)
                .orElse(Phase2Codes.THEME_DEFAULT);
        return new UserSettingsVo(theme);
    }

    @Override
    @Transactional
    public UserSettingsVo updateMine(UserSettingsVo vo) {
        if (vo == null) {
            throw BizException.badRequest("请求体不能为空");
        }
        String theme = Phase2Codes.theme(vo.getThemeKey());
        Long userId = access.userId();
        userRepository.findById(userId).orElseThrow(() -> BizException.notFound("用户不存在"));
        UserPreference row = userPreferenceRepository.findByUserId(userId).orElseGet(() -> {
            UserPreference created = new UserPreference();
            created.setUserId(userId);
            return created;
        });
        row.setThemeKey(theme);
        userPreferenceRepository.save(row);
        return new UserSettingsVo(theme);
    }
}
