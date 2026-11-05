package com.camp.app.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OaVisitorVo {
    private Long id;
    private String visitorName;
    private String phone;
    private String idCard;
    private String org;
    private String purpose;
    private Long hostUserId;
    private String hostName;
    private String hostDept;
    private LocalDateTime appointmentTime;
    private LocalDateTime expectedLeave;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 前端新增来访把事由放在 reason。 */
    @JsonAlias({"reason", "visitPurpose"})
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    /** 前端证件号字段是 idNo。响应仍用 idCard。 */
    @JsonAlias({"idNo", "idNumber", "certificateNo"})
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
