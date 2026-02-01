package top.enderliquid.audioflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdatePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    @Size(min = 6, max = 64, message = "旧密码长度必须在6-64个字符之间")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度必须在6-64个字符之间")
    private String newPassword;
}