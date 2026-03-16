package com.campusfit.api.security;

import com.campusfit.api.domain.User;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

                return new org.springframework.security.core.userdetails.User(
                                String.valueOf(user.getId()),
                                user.getPasswordHash(),
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        }

        public UserDetails loadUserById(Long id) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id));

                return new org.springframework.security.core.userdetails.User(
                                String.valueOf(user.getId()),
                                user.getPasswordHash(),
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        }
}
