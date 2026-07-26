package upce.fei.garden.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import upce.fei.garden.exception.ForbiddenException;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.User;
import upce.fei.garden.model.Worker;

/**
 * Poskytuje přístup k aktuálně přihlášenému uživateli odvozenému z {@link SecurityContextHolder}.
 * Ostatní servisní vrstvy by neměly číst {@code SecurityContext} samy – měly by využívat tuto službu,
 * aby bylo ověřování role (owner/worker) na jednom místě.
 */
@Slf4j
@Service
public class CurrentUserService {

    /**
     * Vrátí aktuálně přihlášeného uživatele bez ohledu na jeho roli.
     *
     * @throws ForbiddenException pokud v kontextu není žádný autentizovaný uživatel
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            log.warn("Pokus o přístup k chráněné akci bez platné autentizace.");
            throw new ForbiddenException("Uživatel není přihlášen.");
        }
        return principal.getUser();
    }

    /**
     * Vrátí aktuálně přihlášeného uživatele jako {@link Owner}.
     *
     * @throws ForbiddenException pokud přihlášený uživatel není vlastník zahrady
     */
    public Owner getCurrentOwner() {
        User user = getCurrentUser();
        if (user instanceof Owner owner) {
            return owner;
        }
        log.warn("Pokus o akci vyžadující roli OWNER: userId={}, role={}", user.getId(), user.getRole());
        throw new ForbiddenException("Tato akce je dostupná pouze vlastníkům zahrady.");
    }

    /**
     * Vrátí aktuálně přihlášeného uživatele jako {@link Worker}.
     *
     * @throws ForbiddenException pokud přihlášený uživatel není zahradník
     */
    public Worker getCurrentWorker() {
        User user = getCurrentUser();
        if (user instanceof Worker worker) {
            return worker;
        }
        log.warn("Pokus o akci vyžadující roli WORKER: userId={}, role={}", user.getId(), user.getRole());
        throw new ForbiddenException("Tato akce je dostupná pouze zahradníkům.");
    }
}
