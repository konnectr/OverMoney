package com.override.orchestrator_service.service;

import com.override.dto.AnalyticsMonthlyIncomeForCategoryDTO;
import com.override.dto.AnalyticsMonthlyReportForYearDTO;
import com.override.dto.TransactionDTO;
import com.override.orchestrator_service.exception.TransactionNotFoundException;
import com.override.orchestrator_service.mapper.TransactionMapper;
import com.override.orchestrator_service.model.Transaction;
import com.override.orchestrator_service.model.User;
import com.override.orchestrator_service.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.InstanceNotFoundException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionMapper transactionMapper;

    public void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    public List<Transaction> findTransactionsListByUserIdWithoutCategories(Long id) throws InstanceNotFoundException {
        Long accID = userService.getUserById(id).getAccount().getId();
        return transactionRepository.findAllWithoutCategoriesByAccountId(accID);
    }

    public Transaction getTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId).orElseThrow(TransactionNotFoundException::new);
    }

    public void updateCategory(Long categoryToMergeId, Long categoryToChangeId) {
        transactionRepository.updateCategoryId(categoryToMergeId, categoryToChangeId);
    }

    @Transactional
    public void setCategoryForAllUndefinedTransactionsWithSameKeywords(UUID transactionId, Long categoryId) {
        Long accId = transactionRepository.findAccountIdByTransactionId(transactionId);
        String transactionMessage = getTransactionById(transactionId).getMessage();
        transactionRepository.updateCategoryIdWhereCategoryIsNull(categoryId, transactionMessage, accId);
    }

    public List<TransactionDTO> findTransactionsByUserIdLimited(Long id, Integer pageSize, Integer pageNumber) throws InstanceNotFoundException {
        Long accID = userService.getUserById(id).getAccount().getId();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("date").descending());

        List<TransactionDTO> transactionList = transactionRepository.findAllByAccountId(accID, pageable).getContent().stream()
                .map(transaction -> transactionMapper.mapTransactionToDTO(transaction))
                .collect(Collectors.toList());
        return enrichTransactionsWithTgUsernames(transactionList);
    }

    private List<TransactionDTO> enrichTransactionsWithTgUsernames(List<TransactionDTO> transactionList) {
        Map<Long, User> userMap = userService.getUsersByIds(transactionList.stream()
                        .map(TransactionDTO::getTelegramUserId)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        transactionList.forEach(transactionDTO -> {
            User user = userMap.get(transactionDTO.getTelegramUserId());
            if (user != null) {
                transactionDTO.setTelegramUserName(user.getUsername());
            }
        });

        return transactionList;
    }

    @Transactional
    public void removeCategoryFromTransactionsWithSameMessage(UUID transactionId) {
        Long accountId = transactionRepository.findAccountIdByTransactionId(transactionId);
        String transactionMessage = getTransactionById(transactionId).getMessage();
        transactionRepository.removeCategoryIdFromTransactionsWithSameMessage(transactionMessage, accountId);
    }

    public Transaction enrichTransactionWithSuggestedCategory(TransactionDTO transactionDTO) {
        Transaction transaction = getTransactionById(transactionDTO.getId());
        transaction.setSuggestedCategoryId(transactionDTO.getSuggestedCategoryId());
        return transaction;
    }

    public List<Integer> findAvailableYears(Long accountId) {
        return transactionRepository.findAvailableYearsForAccountByAccountId(accountId);
    }

    public List<AnalyticsMonthlyReportForYearDTO> findMonthlyIncomeStatisticsForYearByAccountId(Long accountId, Integer year) {
        List<AnalyticsMonthlyIncomeForCategoryDTO> list = transactionRepository.findMonthlyIncomeStatisticsByYearAndAccountId(accountId, year);
        return mapObjectToAnalyticsMonthIncomeDTO(list);
    }

    private List<AnalyticsMonthlyReportForYearDTO> mapObjectToAnalyticsMonthIncomeDTO(List<AnalyticsMonthlyIncomeForCategoryDTO> objects) {
        Set<String> setOfCategoryNames = new HashSet<>();
        List<AnalyticsMonthlyReportForYearDTO> result = new ArrayList<>();
        objects.forEach(object -> {
            setOfCategoryNames.add(object.getCategoryName());
        });
        setOfCategoryNames.forEach(categoryName -> {
            Map<Integer, Double> monthlyAnalytics = new HashMap<>();
            objects.forEach(object -> {
                if (Objects.equals(categoryName, object.getCategoryName())) {
                    monthlyAnalytics.put(object.getMonth(), object.getAmount());
                }
            });
            for (Integer monthCounter = 1; monthCounter <= 12; monthCounter++) {
                if (!monthlyAnalytics.containsKey(monthCounter)) {
                    monthlyAnalytics.put(monthCounter, 0d);
                }
            }
            result.add(new AnalyticsMonthlyReportForYearDTO(categoryName, monthlyAnalytics));
        });
        return result;
    }
}
