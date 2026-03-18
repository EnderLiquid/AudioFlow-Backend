package top.enderliquid.audioflow.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.PASSWORD_MAX;
import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.PASSWORD_MIN;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdatePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = "旧密码长度必须在{min}-{max}个字符之间")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = "新密码长度必须在{min}-{max}个字符之间")
    private String newPassword;
}