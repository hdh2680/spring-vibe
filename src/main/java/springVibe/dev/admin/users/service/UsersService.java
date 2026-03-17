package springVibe.dev.admin.users.service;

import springVibe.dev.admin.users.domain.AdminUser;
import springVibe.dev.admin.users.domain.Role;
import springVibe.dev.admin.users.dto.UserRegistForm;
import springVibe.dev.admin.users.mapper.UsersMapper;
import springVibe.system.exception.BaseException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsersService {
    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;

    public UsersService(UsersMapper usersMapper, PasswordEncoder passwordEncoder) {
        this.usersMapper = usersMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUser> findAll() {
        return usersMapper.findAll();
    }

    public AdminUser findById(Long id) {
        return usersMapper.findById(id);
    }

    public List<Role> findAllRoles() {
        return usersMapper.findAllRoles();
    }

    public List<Long> findRoleIdsByUserId(Long userId) {
        return usersMapper.findRoleIdsByUserId(userId);
    }

    @Transactional
    public Long create(UserRegistForm form) {
        AdminUser exists = usersMapper.findByUsername(form.getUsername());
        if (exists != null) {
            throw new BaseException("USR_DUP_USERNAME", "이미 존재하는 아이디(username)입니다.");
        }

        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BaseException("USR_PASSWORD_REQUIRED", "신규 등록 시 비밀번호는 필수입니다.");
        }

        AdminUser u = new AdminUser();
        u.setUsername(form.getUsername());
        u.setPassword(passwordEncoder.encode(form.getPassword()));
        u.setName(form.getName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setStatus(form.getStatus());

        try {
            usersMapper.insert(u);
        } catch (DuplicateKeyException e) {
            // Could be username/email unique constraint.
            throw new BaseException("USR_DUPLICATE", "중복 데이터가 존재합니다(username/email).", e);
        }

        replaceRoles(u.getId(), form.getRoleIds());
        return u.getId();
    }

    @Transactional
    public void update(UserRegistForm form) {
        if (form.getId() == null) {
            throw new BaseException("USR_ID_REQUIRED", "수정 시 사용자 ID는 필수입니다.");
        }

        AdminUser current = usersMapper.findById(form.getId());
        if (current == null) {
            throw new BaseException("USR_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }

        AdminUser u = new AdminUser();
        u.setId(form.getId());
        u.setName(form.getName());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setStatus(form.getStatus());

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        try {
            usersMapper.update(u);
        } catch (DuplicateKeyException e) {
            throw new BaseException("USR_DUPLICATE", "중복 데이터가 존재합니다(username/email).", e);
        }

        replaceRoles(form.getId(), form.getRoleIds());
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        usersMapper.deleteUserRoles(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            usersMapper.insertUserRole(userId, roleId);
        }
    }
}

