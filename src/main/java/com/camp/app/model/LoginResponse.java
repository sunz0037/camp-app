package com.camp.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 是否首次登录（需要激活）
     */
    private Boolean isFirstLogin;
    
    /**
     * 访问令牌。与 token 同值，契约字段名为 accessToken。
     */
    private String accessToken;

    /**
     * JWT Token（与 accessToken 同值，兼容旧客户端）
     */
    private String token;
    
    /**
     * 刷新Token（首次登录时为null，激活后返回）
     */
    private String refreshToken;
    
    /**
     * 用户信息（首次登录时可能为null）
     */
    private UserInfo userInfo;
    
    /**
     * 提示信息（首次登录时提示需要激活）
     */
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private Integer userType;
        private String avatarUrl;
        private java.util.List<String> roles;
        private java.util.List<String> permissions;
        private com.camp.app.model.DataScope dataScope;
    }
}
