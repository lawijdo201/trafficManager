package gwnucapstone.trafficmanager.data.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity//Entity 설정
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    private String id;

    @Column
    private String pw;

    @Column
    private String name;

    @Column
    private String email;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 여기를 구현해야 할듯? 실행시 토큰에 문제가 있다고 나오는게 이거 때문인듯
        return null;
    }

    @Override
    public String getPassword() {
        return this.pw;
    }

    @Override
    public String getUsername() {
        return this.id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
