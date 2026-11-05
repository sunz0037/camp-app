package com.camp.app.service;

import com.camp.app.model.UserSettingsVo;

public interface UserSettingsService {
    UserSettingsVo getMine();

    UserSettingsVo updateMine(UserSettingsVo vo);
}
