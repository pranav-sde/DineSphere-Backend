package com.festora.menuservice.config;

import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.repository.MenuItemRepository;
import com.festora.menuservice.service.MenuRedisSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MenuDataRestorer implements CommandLineRunner {

    private final MenuItemRepository itemRepo;
    private final MenuRedisSyncService redisSyncService;

    @Override
    public void run(String... args) {
        log.info("Starting background menu sync to Redis...");
        List<MenuItem> allItems = itemRepo.findAll();
        allItems.forEach(redisSyncService::syncMenuItem);
        log.info("Successfully synced {} menu items to Redis", allItems.size());
    }
}
