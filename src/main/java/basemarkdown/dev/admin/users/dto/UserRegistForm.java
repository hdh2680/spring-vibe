package basemarkdown.dev.admin.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class UserRegistForm {
    private Long id;

    @NotBlank(message = "아이디(username)는 필수입니다.")
    @Size(max = 50, message = "아이디(username)는 50자 이하여야 합니다.")
    private String username;

    // 신규 생성 시에는 필수, 수정 시에는 선택 처리(컨트롤러에서 추가 검증)
    @Size(max = 255, message = "비밀번호(password)는 255자 이하여야 합니다.")
    private String password;

    @Size(max = 100, message = "이름(name)은 100자 이하여야 합니다.")
    private String name;

    @Email(message = "이메일(email) 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일(email)은 100자 이하여야 합니다.")
    private String email;

    @Size(max = 20, message = "전화번호(phone)는 20자 이하여야 합니다.")
    private String phone;

    @NotBlank(message = "상태(status)는 필수입니다.")
    @Size(max = 20, message = "상태(status)는 20자 이하여야 합니다.")
    private String status = "ACTIVE";

    private List<Long> roleIds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}

