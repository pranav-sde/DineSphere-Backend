package com.festora.menuservice.config;

import com.festora.menuservice.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MenuDataRestorer implements CommandLineRunner {

    private final MenuItemRepository itemRepo;

    @Override
    public void run(String... args) {
        log.info("Menu data restorer initialized (Redis sync disabled).");
    }
}
