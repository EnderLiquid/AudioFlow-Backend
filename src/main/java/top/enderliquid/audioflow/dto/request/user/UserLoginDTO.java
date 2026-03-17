package top.enderliquid.audioflow.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static top.enderliquid.audioflow.common.constant.FieldLengthConstants.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(min = EMAIL_MIN, max = EMAIL_MAX, message = "邮箱长度必须在" + EMAIL_MIN + "-" + EMAIL_MAX + "个字符之间")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = PASSWORD_MIN, max = PASSWORD_MAX, message = "密码长度必须在" + PASSWORD_MIN + "-" + PASSWORD_MAX + "个字符之间")
    private String password;
}