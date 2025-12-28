package leti.schedule.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.ArrayList;
import java.util.List;

public class KeyboardService {

    public static ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Сегодня");
        row1.add("Завтра");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Ближайшее");
        row2.add("Неделя");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Понедельник");
        row3.add("Вторник");
        row3.add("Среда");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Четверг");
        row4.add("Пятница");
        row4.add("Суббота");
        keyboard.add(row4);

        KeyboardRow row5 = new KeyboardRow();
        row5.add("Настройка группы");
        keyboard.add(row5);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getGroupSetupKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("4351");
        row1.add("4352");
        row1.add("4353");
        row1.add("4354");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Ввести свою группу");
        row2.add("Назад в меню");
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


    //назад
    public static ReplyKeyboardMarkup getBackKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Назад в меню");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


}