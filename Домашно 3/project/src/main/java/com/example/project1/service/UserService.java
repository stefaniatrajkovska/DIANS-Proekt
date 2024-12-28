package com.example.project1.service;

import com.example.project1.model.exceptions.UserAlreadyExistsException;
import com.example.project1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.project1.model.User;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void addUser(String username, String password) throws UserAlreadyExistsException {
        // Проверка дали корисникот веќе постои
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            // Ако корисникот постои, фрлај грешка
            throw new UserAlreadyExistsException("User with username " + username + " already exists.");
        }

        // Ако не постои, создај нов корисник
        User userModel = new User(username, passwordEncoder.encode(password));
        userRepository.save(userModel);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Пребарување за корисникот
        Optional<com.example.project1.model.User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            // Ако не постои, фрли грешка
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Врати деталите за корисникот со улога ROLE_USER
        return new org.springframework.security.core.userdetails.User(
                user.get().getUsername(),
                user.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
