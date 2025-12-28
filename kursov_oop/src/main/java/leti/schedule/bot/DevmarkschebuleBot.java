package leti.schedule.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class DevmarkschebuleBot {
    public static void main(String[] args) {
        String token = BotConfig.getBotToken();
        if (token == null || token.contains("${") || token.equals("telegram.bot.token")) {
            System.err.println("Токен не найден, ошибка");
            System.exit(1);
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ScheduleBot());
            System.out.println("Бот запущен! Имя: " + BotConfig.getBotUsername());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
        }
    }
}