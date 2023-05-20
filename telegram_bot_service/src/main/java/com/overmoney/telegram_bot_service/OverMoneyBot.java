package com.overmoney.telegram_bot_service;

import com.overmoney.telegram_bot_service.constants.Command;
import com.overmoney.telegram_bot_service.mapper.TransactionMapper;
import com.overmoney.telegram_bot_service.mapper.UserMapper;
import com.overmoney.telegram_bot_service.model.User;
import com.overmoney.telegram_bot_service.service.UserService;
import com.override.dto.AccountDataDTO;
import com.override.dto.TransactionMessageDTO;
import com.override.dto.TransactionResponseDTO;
import com.overmoney.telegram_bot_service.service.OrchestratorRequestService;
import com.overmoney.telegram_bot_service.service.TelegramBotApiRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.management.InstanceNotFoundException;
import java.util.Objects;

@Component
@Slf4j
public class OverMoneyBot extends TelegramLongPollingBot {

    private final String MESSAGE_INVALID = "Мы не смогли распознать ваше сообщение. Убедитесь, что сумма и товар указаны верно и попробуйте еще раз :)";

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    @Autowired
    private OrchestratorRequestService orchestratorRequestService;

    @Autowired
    private TelegramBotApiRequestService telegramBotApiRequestService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();

        if (Objects.isNull(update.getMessage().getChat().getTitle())) {
            try {
                userService.getUserByTelegramUsername(username);
            } catch (InstanceNotFoundException e) {
                User user = userMapper.mapTelegramUsertoUser(update.getMessage().getFrom());
                userService.saveUser(user);
            }
        }

        if (update.getMessage().hasText()) {
            String receivedMessage = update.getMessage().getText();
            botAnswer(receivedMessage, chatId, username);
        }

        if (update.getMessage().hasVoice()) {
            byte[] voiceMessage = telegramBotApiRequestService.getVoiceMessageBytes(update.getMessage().getVoice().getFileId());
            orchestratorRequestService.sendVoiceMessage(voiceMessage);
        }
    }

    private void botAnswer(String receivedMessage, Long chatId, String username) {
        switch (receivedMessage) {
            case "/start":
                sendMessage(chatId, Command.START.getDescription());
                orchestratorRequestService.registerOverMoneyAccount(new AccountDataDTO(chatId, username));
                break;
            case "/money":
                sendMessage(chatId, Command.MONEY.getDescription());
                break;
            default:
                try {
                    TransactionResponseDTO transactionResponseDTO = orchestratorRequestService.sendTransaction(new TransactionMessageDTO(receivedMessage, username, chatId));
                    sendMessage(chatId, transactionMapper.mapTransactionResponseToTelegramMessage(transactionResponseDTO));
                } catch (Exception e) {
                    sendMessage(chatId, MESSAGE_INVALID);
                }
                break;
        }
    }

    private void sendMessage(Long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId.toString(), messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
