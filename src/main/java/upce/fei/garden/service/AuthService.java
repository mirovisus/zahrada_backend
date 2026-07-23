package upce.fei.garden.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upce.fei.garden.dto.auth.AuthResponse;
import upce.fei.garden.dto.auth.LoginRequest;
import upce.fei.garden.dto.auth.RegisterRequest;
import upce.fei.garden.exception.ConflictException;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.User;
import upce.fei.garden.model.Worker;
import upce.fei.garden.repository.UserRepository;
import upce.fei.garden.security.JwtService;
import upce.fei.garden.security.UserPrincipal;

/**
 * Registrace a přihlašování uživatelů. Registrace vytváří buď {@link Owner}, nebo {@link Worker}
 * podle role v požadavku; přihlášení ověřuje údaje přes {@link AuthenticationManager}. Oba případy
 * vrací {@link AuthResponse} s podepsaným JWT (viz {@link JwtService}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Zaregistruje nového uživatele. Podle {@code request.getRole()} vytvoří {@link Owner} nebo
     * {@link Worker}; e-mail musí být v systému unikátní.
     *
     * @throws ConflictException pokud už uživatel s daným e-mailem existuje
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Pokus o registraci na již existující e-mail: {}", request.getEmail());
            throw new ConflictException("Uživatel s tímto e-mailem již existuje.");
        }

        User user = switch (request.getRole()) {
            case OWNER -> new Owner();
            case WORKER -> new Worker();
        };
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User saved = userRepository.save(user);
        log.info("Nová registrace uživatele: id={}, role={}", saved.getId(), saved.getRole());

        String token = jwtService.generateToken(saved.getEmail(), saved.getRole());
        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getRole(), token);
    }

    /**
     * Ověří přihlašovací údaje a v případě úspěchu vrátí nový JWT.
     *
     * @throws AuthenticationException pokud e-mail neexistuje nebo heslo nesouhlasí
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            log.warn("Neúspěšný pokus o přihlášení pro e-mail: {}", request.getEmail());
            throw e;
        }

        User user = ((UserPrincipal) authentication.getPrincipal()).getUser();

        log.info("Úspěšné přihlášení uživatele: id={}", user.getId());
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole(), token);
    }
}
