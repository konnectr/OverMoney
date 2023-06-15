package com.override.orchestrator_service.service;

import com.override.orchestrator_service.config.RecentActivityProperties;
import com.override.orchestrator_service.model.OverMoneyAccount;
import com.override.orchestrator_service.model.User;
import com.override.orchestrator_service.repository.OverMoneyAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.InstanceNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OverMoneyAccountService {
    @Autowired
    private OverMoneyAccountRepository overMoneyAccountRepository;
    @Autowired
    private RecentActivityProperties recentActivityProperties;
    @Autowired
    private UserService userService;

    public List<OverMoneyAccount> getAllAccounts() {
        return (List<OverMoneyAccount>) overMoneyAccountRepository.findAll();
    }

    public List<Long> getAllActivityUsers() {
        LocalDateTime minimalDate = LocalDateTime.now().minusDays(recentActivityProperties.getActivityDays());
        return overMoneyAccountRepository.findAllActivityUsersByAccId(minimalDate);
    }

    public void saveOverMoneyAccount(Long chatId, Long userId) throws InstanceNotFoundException {
        OverMoneyAccount overMoneyAccount = OverMoneyAccount.builder()
                .chatId(chatId)
                .users(getUser(userId))
                .build();
        User user = userService.getUserById(userId);
        user.setAccount(overMoneyAccount);
        saveOverMoneyAccount(overMoneyAccount);
    }

    private Set<User> getUser(Long userId) throws InstanceNotFoundException {
        User user = userService.getUserById(userId);
        Set<User> accountUsers = new HashSet<>();
        accountUsers.add(user);
        return accountUsers;
    }

    public void saveOverMoneyAccount(OverMoneyAccount overMoneyAccount) {
        overMoneyAccountRepository.save(overMoneyAccount);
    }

    public OverMoneyAccount getOverMoneyAccountByChatId(Long chatId) {
        return overMoneyAccountRepository.findByChatId(chatId);
    }

    public OverMoneyAccount getAccountByUsername(String username) {
        return userService.getUserByUsername(username).getAccount();
    }

    public OverMoneyAccount getAccountByUserId(Long id) throws InstanceNotFoundException {
        return userService.getUserById(id).getAccount();
    }

    public OverMoneyAccount getOverMoneyAccountById(Long id) throws InstanceNotFoundException {
        return overMoneyAccountRepository.findById(id).orElseThrow(() -> new InstanceNotFoundException("OverMoneyAccount with id " + id + " does not exist"));
    }

    public void deleteOverMoneyAccountById(Long id) {
        overMoneyAccountRepository.deleteById(id);
    }
}
