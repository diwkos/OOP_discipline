package leti.schedule.bot;

import com.google.gson.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import java.time.DateTimeException;
import java.time.format.TextStyle;

public class ScheduleService {

    public static String getScheduleForGroup(String groupId) throws IOException, ScheduleException {
        String apiUrl = String.format(
                "https://digital.etu.ru/api/mobile/schedule?groupNumber=%s&season=autumn&year=2025&joinWeeks=true&withURL=true",
                groupId
        );

        return fetchJsonFromUrl(apiUrl);
    }

    private static String fetchJsonFromUrl(String url) throws IOException, ScheduleException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ScheduleException("API вернуло ошибку: " + response.getStatusLine().getStatusCode());
            }

            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }

    public static String getCurrentWeekParity() {
        LocalDate semesterStartDate = LocalDate.of(2025, 9, 2);
        long weeksSinceStart = ChronoUnit.WEEKS.between(semesterStartDate, LocalDate.now());
        boolean isOddWeek = weeksSinceStart % 2 == 0;
        return isOddWeek ? "нечётная" : "чётная";
    }

    public static String getCurrentWeekInfo() {
        return "*Текущая неделя:* " + getCurrentWeekParity();
    }

    public static String parseScheduleForDay(String jsonData, String dayName, String groupNumber) throws ScheduleException {
        try {
            JsonObject rootObject = JsonParser.parseString(jsonData).getAsJsonObject();

            if (!rootObject.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена");
            }

            JsonObject groupData = rootObject.getAsJsonObject(groupNumber);
            JsonObject daysSchedule = groupData.getAsJsonObject("days");
            int dayIndex = getDayIndex(dayName);

            if (dayIndex == -1) {
                throw new ScheduleException("Неверный день недели");
            }

            String dayKey = String.valueOf(dayIndex);
            if (!daysSchedule.has(dayKey)) {
                return "В этот день занятий нет.";
            }

            JsonObject daySchedule = daysSchedule.getAsJsonObject(dayKey);
            if (!daySchedule.has("lessons") || daySchedule.get("lessons").isJsonNull()) {
                return "В этот день занятий нет.";
            }

            JsonArray lessonsArray = daySchedule.getAsJsonArray("lessons");
            if (lessonsArray.isEmpty()) {
                return "В этот день занятий нет.";
            }

            List<JsonObject> lessonsList = new ArrayList<>();
            for (JsonElement lesson : lessonsArray) {
                lessonsList.add(lesson.getAsJsonObject());
            }

            lessonsList.sort((lesson1, lesson2) -> {
                String time1 = getSafeString(lesson1, "start_time", "00:00");
                String time2 = getSafeString(lesson2, "start_time", "00:00");
                return time1.compareTo(time2);
            });

            StringBuilder scheduleText = new StringBuilder();
            String displayDayName = daySchedule.has("name") ?
                    daySchedule.get("name").getAsString() : getRussianDayName(dayIndex);

            scheduleText.append(getCurrentWeekInfo()).append("\n");
            scheduleText.append("*").append(displayDayName).append("*\n\n");

            for (JsonObject lesson : lessonsList) {
                String startTime = getSafeString(lesson, "start_time", "??:??");
                String endTime = getSafeString(lesson, "end_time", "??:??");
                String subjectName = getSafeString(lesson, "name", "Предмет не указан");
                String lessonType = getSafeString(lesson, "subjectType", "");
                String teacherName = getSafeString(lesson, "teacher", "");
                String classroom = getSafeString(lesson, "room", "");
                String lessonFormat = getSafeString(lesson, "form", "");
                String weekType = getSafeString(lesson, "week", "");

                String weekInfoText = "";
                if (!weekType.isEmpty() && !weekType.equals("null")) {
                    weekInfoText = getWeekTypeInfo(weekType);
                }

                scheduleText.append("*").append(startTime).append(" - ").append(endTime);
                if (!weekInfoText.isEmpty()) {
                    scheduleText.append(" ").append(weekInfoText);
                }
                scheduleText.append("*\n");

                scheduleText.append(" ").append(subjectName);
                if (!lessonType.isEmpty()) {
                    scheduleText.append(" (").append(lessonType).append(")");
                }
                scheduleText.append("\n");

                if (!teacherName.isEmpty() && !teacherName.equals("null")) {
                    scheduleText.append(" ").append(teacherName).append("\n");
                }

                if ("online".equalsIgnoreCase(lessonFormat) || "distant".equalsIgnoreCase(lessonFormat)) {
                    scheduleText.append(" Онлайн");
                } else if (!classroom.isEmpty() && !classroom.equals("null")) {
                    scheduleText.append(" Ауд. ").append(classroom);
                }

                scheduleText.append("\n\n");
            }

            return scheduleText.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

    public static String getWeekSchedule(String jsonData, String groupNumber) throws ScheduleException {
        try {
            JsonObject rootObject = JsonParser.parseString(jsonData).getAsJsonObject();

            if (!rootObject.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена");
            }

            JsonObject groupData = rootObject.getAsJsonObject(groupNumber);
            JsonObject daysSchedule = groupData.getAsJsonObject("days");

            StringBuilder weekScheduleText = new StringBuilder();
            weekScheduleText.append(getCurrentWeekInfo()).append("\n");
            weekScheduleText.append("*Расписание для группы ").append(groupNumber).append("*\n\n");

            String[] russianDayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

            boolean hasAnyLessons = false;

            for (int dayIdx = 0; dayIdx < 6; dayIdx++) {
                String dayKey = String.valueOf(dayIdx);
                if (daysSchedule.has(dayKey)) {
                    JsonObject daySchedule = daysSchedule.getAsJsonObject(dayKey);
                    if (daySchedule.has("lessons") && !daySchedule.get("lessons").isJsonNull()) {
                        JsonArray lessonsArray = daySchedule.getAsJsonArray("lessons");
                        if (!lessonsArray.isEmpty()) {
                            hasAnyLessons = true;

                            String displayDayName = daySchedule.has("name") ?
                                    daySchedule.get("name").getAsString() : russianDayNames[dayIdx];
                            weekScheduleText.append("*").append(displayDayName).append("*:\n");

                            for (JsonElement lesson : lessonsArray) {
                                JsonObject lessonObj = lesson.getAsJsonObject();
                                String startTime = getSafeString(lessonObj, "start_time", "??:??");
                                String endTime = getSafeString(lessonObj, "end_time", "??:??");
                                String subjectName = getSafeString(lessonObj, "name", "Предмет");
                                String lessonType = getSafeString(lessonObj, "subjectType", "");
                                String classroom = getSafeString(lessonObj, "room", "");

                                weekScheduleText.append("  • ").append(startTime).append("-").append(endTime);
                                weekScheduleText.append(" - ").append(subjectName);

                                if (!lessonType.isEmpty()) {
                                    weekScheduleText.append(" (").append(lessonType).append(")");
                                }

                                if (!classroom.isEmpty() && !classroom.equals("null")) {
                                    weekScheduleText.append(" (").append(classroom).append(")");
                                }

                                weekScheduleText.append("\n");
                            }
                            weekScheduleText.append("\n");
                        }
                    }
                }
            }

            if (!hasAnyLessons) {
                return "На этой неделе занятий нет.";
            }

            return weekScheduleText.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

    public static String findNearestLesson(String jsonData, String groupNumber) throws ScheduleException {
        try {
            LocalTime currentTime = LocalTime.now();
            LocalDate currentDate = LocalDate.now();
            DayOfWeek currentDayOfWeek = currentDate.getDayOfWeek();

            JsonObject rootObject = JsonParser.parseString(jsonData).getAsJsonObject();

            if (!rootObject.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена. Попробуйте изменить номер группы.");
            }

            JsonObject groupData = rootObject.getAsJsonObject(groupNumber);
            JsonObject daysSchedule = groupData.getAsJsonObject("days");

            // Ищем на сегодня
            int todayIndex = currentDayOfWeek.getValue() - 1;
            String todayKey = String.valueOf(todayIndex);

            if (daysSchedule.has(todayKey)) {
                JsonObject todaySchedule = daysSchedule.getAsJsonObject(todayKey);
                if (todaySchedule.has("lessons") && !todaySchedule.get("lessons").isJsonNull()) {
                    JsonArray lessonsArray = todaySchedule.getAsJsonArray("lessons");

                    JsonObject nearestLesson = null;
                    LocalTime nearestLessonTime = null;

                    for (JsonElement lesson : lessonsArray) {
                        JsonObject lessonObj = lesson.getAsJsonObject();
                        String startTimeStr = getSafeString(lessonObj, "start_time", "");
                        if (!startTimeStr.isEmpty()) {
                            try {
                                LocalTime lessonTime = LocalTime.parse(startTimeStr);

                                String weekType = getSafeString(lessonObj, "week", "");
                                if (isLessonForCurrentWeek(weekType)) {
                                    if (lessonTime.isAfter(currentTime) || lessonTime.equals(currentTime)) {
                                        if (nearestLessonTime == null || lessonTime.isBefore(nearestLessonTime)) {
                                            nearestLessonTime = lessonTime;
                                            nearestLesson = lessonObj;
                                        }
                                    }
                                }
                            } catch (DateTimeException ignored) {
                                // Пропускаем невалидное время и продолжаем цикл
                            }
                        }
                    }

                    if (nearestLesson != null) {
                        return formatNearestLesson(nearestLesson, "сегодня");
                    }
                }
            }

            // Если на сегодня не нашли, ищем на ближайшие дни
            for (int daysOffset = 1; daysOffset <= 7; daysOffset++) {
                int nextDayIndex = (todayIndex + daysOffset) % 7;
                String nextDayKey = String.valueOf(nextDayIndex);

                if (daysSchedule.has(nextDayKey)) {
                    JsonObject daySchedule = daysSchedule.getAsJsonObject(nextDayKey);
                    if (daySchedule.has("lessons") && !daySchedule.get("lessons").isJsonNull()) {
                        JsonArray lessonsArray = daySchedule.getAsJsonArray("lessons");

                        if (!lessonsArray.isEmpty()) {
                            JsonObject firstLesson = lessonsArray.get(0).getAsJsonObject();

                            String dayDisplayName;
                            switch (daysOffset) {
                                case 1 -> dayDisplayName = "завтра";
                                case 2 -> dayDisplayName = "послезавтра";
                                default -> {
                                    LocalDate targetDate = currentDate.plusDays(daysOffset);
                                    String rawDayName = targetDate.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
                                    dayDisplayName = rawDayName.substring(0, 1).toUpperCase() + rawDayName.substring(1);
                                }
                            }

                            return formatNearestLesson(firstLesson, dayDisplayName);
                        }
                    }
                }
            }

            return "Ближайших занятий не найдено";

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

    private static String formatNearestLesson(JsonObject lesson, String dayDisplayName) {
        String startTime = getSafeString(lesson, "start_time", "??:??");
        String endTime = getSafeString(lesson, "end_time", "??:??");
        String subjectName = getSafeString(lesson, "name", "Предмет не указан");
        String lessonType = getSafeString(lesson, "subjectType", "");
        String teacherName = getSafeString(lesson, "teacher", "");
        String classroom = getSafeString(lesson, "room", "");
        String lessonFormat = getSafeString(lesson, "form", "");
        String weekType = getSafeString(lesson, "week", "");

        StringBuilder resultText = new StringBuilder();
        resultText.append("*Ближайшее занятие*\n\n");
        resultText.append("*").append(dayDisplayName).append("*\n");
        resultText.append("*").append(startTime).append(" - ").append(endTime).append("*\n");
        resultText.append(" ").append(subjectName);

        if (!lessonType.isEmpty()) {
            resultText.append(" (").append(lessonType).append(")");
        }
        resultText.append("\n");

        if (!teacherName.isEmpty() && !teacherName.equals("null")) {
            resultText.append(" ").append(teacherName).append("\n");
        }

        if ("online".equalsIgnoreCase(lessonFormat) || "distant".equalsIgnoreCase(lessonFormat)) {
            resultText.append(" Онлайн");
        } else if (!classroom.isEmpty() && !classroom.equals("null")) {
            resultText.append(" Ауд. ").append(classroom);
        }

        if (!weekType.isEmpty() && !weekType.equals("null")) {
            resultText.append("\n ").append(getWeekTypeInfo(weekType));
        }

        return resultText.toString();
    }

    private static boolean isLessonForCurrentWeek(String weekType) {
        if (weekType.isEmpty() || weekType.equals("null") || weekType.equals("3")) {
            return true;
        }

        String currentParity = getCurrentWeekParity();
        boolean isCurrentOdd = currentParity.equals("нечётная");

        return (isCurrentOdd && weekType.equals("1")) || (!isCurrentOdd && weekType.equals("2"));
    }

    private static String getWeekTypeInfo(String weekType) {
        return switch (weekType) {
            case "1" -> "(Нечётная неделя)";
            case "2" -> "(Чётная неделя)";
            case "3" -> "(Все недели)";
            default -> "";
        };
    }

    private static int getDayIndex(String dayName) {
        return switch (dayName.toLowerCase()) {
            case "monday" -> 0;
            case "tuesday" -> 1;
            case "wednesday" -> 2;
            case "thursday" -> 3;
            case "friday" -> 4;
            case "saturday" -> 5;
            default -> -1;
        };
    }

    private static String getRussianDayName(int index) {
        String[] russianDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
        return (index >= 0 && index < russianDays.length) ? russianDays[index] : "День недели";
    }

    private static String getSafeString(JsonObject jsonObj, String key, String defaultValue) {
        if (jsonObj.has(key) && !jsonObj.get(key).isJsonNull()) {
            String value = jsonObj.get(key).getAsString();
            return (value == null || value.equals("null") || value.trim().isEmpty()) ?
                    defaultValue : value.trim();
        }
        return defaultValue;
    }

    public static String getTomorrowDayName() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DayOfWeek day = tomorrow.getDayOfWeek();
        return translateDay(day.getDisplayName(TextStyle.FULL, new Locale("ru")).toLowerCase());
    }

    public static String getTodayDayName() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        return translateDay(day.getDisplayName(TextStyle.FULL, new Locale("ru")).toLowerCase());
    }

    private static String translateDay(String russianDayName) {
        return switch (russianDayName) {
            case "понедельник" -> "monday";
            case "вторник" -> "tuesday";
            case "среда" -> "wednesday";
            case "четверг" -> "thursday";
            case "пятница" -> "friday";
            case "суббота" -> "saturday";
            default -> russianDayName;
        };
    }
}

class ScheduleException extends Exception {
    public ScheduleException(String message) {
        super(message);
    }
}