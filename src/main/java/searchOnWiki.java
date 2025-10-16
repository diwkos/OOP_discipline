import java.net.URI;//класс для отправки url
import java.util.Scanner;//класс для считывания данных пользователя
import java.net.http.HttpClient;//класс для создания HTTP клиента и отправки запросов
import java.net.http.HttpRequest;//класс для построения HTTP запроса
import java.net.http.HttpResponse;//класс для представления HTTP ответа от сервера
import java.time.Duration;//класс для работы с временными промежутками
import java.net.URLEncoder;//класс для кодирования строк в URL-формат
import java.nio.charset.StandardCharsets;//Класс с стандартными кодировками
import java.util.List;//интерфейс для работы со списками (упорядоченными коллекциями)
import java.awt.Desktop;//класс для просмотра в браузере в данном случае
import com.google.gson.Gson;//библиотека для преобразования JSON в Java-объекты и обратно
public class searchOnWiki {

    private static final String WIKIPEDIA_BASE_URL = "https://ru.wikipedia.org/wiki/";
    private static final String WIKIPEDIA_API_URL = "https://ru.wikipedia.org/w/api.php?action=query&list=search&utf8=&format=json&srsearch=";
    private static final int REQUEST_TIMEOUT_SECONDS = 15;//время таймаута

    private static final Gson gson = new Gson();

    public static String fetchJSONfromURL(String url){
        try{
            // 1. Создаем HTTP клиент
            HttpClient client = HttpClient.newHttpClient();

            // 2. Создаем запрос
            HttpRequest request = HttpRequest.newBuilder()//создаем "строителя для удобства/читаемости"                    .uri(URI.create(url))
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))//таймаут - "обход" вечного ожидания
                    .header("Accept", "application/json")//"просим" сервер ответить файлом json
                    .header("User-Agent", "WikiSearchBot/1.0")
                    .GET() //указываем метод GET
                    .build();

            // 3. Отправляем запрос и получаем ответ
            HttpResponse<String>/*ответ строка*/ response = client.send(
                    request,//запрос
                    HttpResponse.BodyHandlers.ofString()//преобразовываем тело ответа в строку
            );

            // 4. Проверяем статус и обрабатываем ответ
            if (response.statusCode() >= 200 && response.statusCode()<300) {//если успешный код (200-299), то...
                return response.body();//возвращаем тело ответа
            }
            else {
                throw new RuntimeException("HTTP ошибка: " + response.statusCode() + " - " + response.body());//обработка исключений
            }
        } catch(Exception e){
            throw new RuntimeException("Ошибка при запросе: " + e.getMessage(), e);//обработка исключений
        }
    }

    public static void parseAndDisplayResults(String jsonResponse){
        try {
            // Превращаем JSON в Java объект
            WikipediaResponse response = gson.fromJson(jsonResponse, WikipediaResponse.class);
            // Проверяем есть ли результат
            if (response.query.search.isEmpty()) {
                System.out.println("Ничего не найдено");
                return;//выходим, если нет
            }
            // Берем первую найденную статью
            SearchResult firstResult = response.query.search.get(0);
            // Создаем ссылку на статью Wikipedia
            String articleUrl = WIKIPEDIA_BASE_URL +
                    URLEncoder.encode(firstResult.title, StandardCharsets.UTF_8);

            // Открываем в браузере
            Desktop.getDesktop().browse(new URI(articleUrl));

        }catch(Exception e) {
            // Выводим ошибку, если что-то пошло не так
            System.out.println("Ошибка: " + e.getMessage());

        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String Text = scanner.nextLine();
        try {
            String new_str = URLEncoder.encode(Text, StandardCharsets.UTF_8);
            String apiurl = WIKIPEDIA_API_URL+new_str;

            String jsonResponse = fetchJSONfromURL(apiurl);
            // Просто передаем JSON в Gson - он всё сделает сам
            parseAndDisplayResults(jsonResponse);

        }catch(Exception e){
            System.out.println("Ошибка: " + e.getMessage());
        }finally{
            scanner.close(); // Закрываем scanner
        }
    }
    //------------------------------------
    // Классы для автоматического парсинга

    // Класс для хранения всего ответа от Wikipedia
    static class WikipediaResponse {
        public Query query; // Объект, содержащий результаты запроса
    }
    // Класс для хранения раздела с поиском
    static class Query {
        public List<SearchResult> search; // Список найденных статей в результате поиска
    }
    // Класс для хранения информации об одной статье
    static class SearchResult {
        public String title; // Заголовок найденной статьи Wikipedia
    }
}