package com.lognex.api.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lognex.api.LognexApi;
import com.lognex.api.entities.MetaEntity;
import com.lognex.api.responses.ErrorResponse;
import com.lognex.api.responses.ListEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public final class HttpRequestExecutor {
    private static final Logger logger = LogManager.getLogger(HttpRequestExecutor.class);
    private static final Base64.Encoder b64enc = Base64.getEncoder();
    private static Charset queryParamsCharset = Charset.forName("UTF-8");

    private final String url;
    private Map<String, Object> query;
    private List<String> expand;
    private Map<String, Object> headers;
    private final Gson gson;
    private final CloseableHttpClient client;
    private Object body;

    private HttpRequestExecutor(LognexApi api, String url) {
        if (api == null) throw new IllegalArgumentException("Для выполнения запроса к API нужен проинициализированный экземпляр LognexApi!");

        this.client = api.getClient();
        this.url = api.getHost() + LognexApi.API_PATH + url;
        query = new HashMap<>();
        headers = new HashMap<>();
        body = null;
        auth(api);

        if (api.isTimeWithMilliseconds()) header("X-Lognex-Format-Millisecond", "true");
        gson = LognexApi.createGson(false, api.isTimeWithMilliseconds());
    }

    private HttpRequestExecutor(CloseableHttpClient client, String url) {
        if (client == null) throw new IllegalArgumentException("Для выполнения запроса нужен проинициализированный экземпляр CloseableHttpClient!");

        this.client = client;
        this.url = url;
        query = new HashMap<>();
        headers = new HashMap<>();
        body = null;
        gson = LognexApi.createGson();
    }

    /**
     * Задаёт кодировку параметров запроса
     */
    public static void setQueryParamsCharset(Charset queryParamsCharset) {
        HttpRequestExecutor.queryParamsCharset = queryParamsCharset;
    }

    /**
     * Создаёт билдер запроса к URL
     */
    public static HttpRequestExecutor url(LognexApi api, String url) {
        return new HttpRequestExecutor(api.getClient(), url).auth(api);
    }

    /**
     * Создаёт билдер запроса к методу API
     *
     * @param api  проинициализированный экземпляр класса с данными API
     * @param path путь к методу API (например <code>/entity/counterparty/metadata</code>)
     */
    public static HttpRequestExecutor path(LognexApi api, String path) {
        return new HttpRequestExecutor(api, path);
    }

    /**
     * Добавляет авторизационный заголовок с данными доступа API
     */
    private HttpRequestExecutor auth(LognexApi api) {
        return this.header(
                "Authorization",
                "Basic " + b64enc.encodeToString((api.getLogin() + ":" + api.getPassword()).getBytes())
        );
    }

    /**
     * Добавить параметр в строку запроса после URL в формате <code>key=value&</code>
     */
    public HttpRequestExecutor query(String key, Object value) {
        if (query == null) query = new HashMap<>();
        query.put(key, value);
        return this;
    }

    /**
     * Добавить параметр в заголовки запроса
     */
    public HttpRequestExecutor header(String key, Object value) {
        if (headers == null) headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }

    /**
     * Добавить поля ответа, которые необходимо получить с сервера сразу с данными (параметр <code>expand</code>)
     */
    public HttpRequestExecutor expand(String... fields) {
        if (fields == null || fields.length == 0) return this;

        if (expand == null) expand = new ArrayList<>();
        Collections.addAll(expand, fields);
        return this;
    }

    /**
     * Добавить тело запроса (для запросов, поддерживающих отправку данных в теле)
     */
    public HttpRequestExecutor body(Object o) {
        body = o;
        return this;
    }

    /**
     * Строит полный URL запроса с учётом добавленных ранее параметров запроса
     */
    private String getFullUrl() {
        if (expand != null && !expand.isEmpty()) {
            query("expand", expand.stream().collect(Collectors.joining(",")));
        }

        if (query == null || query.isEmpty()) return url;

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, Object> e : query.entrySet()) {
            if (queryBuilder.length() > 0) queryBuilder.append("&");
            try {
                queryBuilder.
                        append(URLEncoder.encode(e.getKey(), queryParamsCharset.name())).
                        append("=").
                        append(URLEncoder.encode(String.valueOf(e.getValue()), queryParamsCharset.name()));
            } catch (UnsupportedEncodingException e1) {
            }
        }

        return url + (queryBuilder.length() == 0 ? "" : "?" + queryBuilder.toString());
    }

    /**
     * Добавляет заголовки в запрос
     */
    private void applyHeaders(HttpUriRequest request) {
        for (Map.Entry<String, Object> e : headers.entrySet()) {
            request.setHeader(e.getKey(), String.valueOf(e.getValue()));
        }
    }

    /**
     * Выполняет созданный запрос
     *
     * @return тело ответа
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    private String executeRequest(HttpUriRequest request) throws IOException, LognexApiException {
        logger.debug("Выполнение запроса  {} {}...", request.getMethod(), request.getURI());
        try (CloseableHttpResponse response = client.execute(request)) {
            String json = response.getStatusLine().getStatusCode() == 204 ?
                    "" :
                    EntityUtils.toString(response.getEntity());

            logger.debug(
                    "Ответ на запрос     {} {}: ({}) {}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusLine().getStatusCode(),
                    json
            );

            if (response.getStatusLine().getStatusCode() != 200 &&
                    response.getStatusLine().getStatusCode() != 201 &&
                    response.getStatusLine().getStatusCode() != 204) {
                ErrorResponse er = gson.fromJson(json, ErrorResponse.class);

                throw new LognexApiException(
                        request.getMethod() + " " + request.getURI(),
                        response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(),
                        er
                );
            }

            return json;
        }
    }

    /**
     * Выполняет GET-запрос с указанными ранее параметрами
     *
     * @return тело ответа
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public String get() throws IOException, LognexApiException {
        HttpGet request = new HttpGet(getFullUrl());
        applyHeaders(request);
        return executeRequest(request);
    }

    /**
     * Выполняет GET-запрос с указанными ранее параметрами и конвертирует ответ в объект указанного класса
     *
     * @param cl класс, в который нужно сконвертировать ответ на запрос
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public <T> T get(Class<T> cl) throws IOException, LognexApiException {
        return gson.fromJson(get(), cl);
    }

    /**
     * Выполняет GET-запрос с указанными ранее параметрами и конвертирует ответ в <b>массив</b> объектов указанного класса
     *
     * @param cl класс объектов массива, в который нужно сконвертировать ответ на запрос
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public <T extends MetaEntity> ListEntity<T> list(Class<T> cl) throws IOException, LognexApiException {
        return gson.fromJson(get(), TypeToken.getParameterized(ListEntity.class, cl).getType());
    }

    /**
     * Выполняет POST-запрос с указанными ранее параметрами
     *
     * @return тело ответа
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public String post() throws IOException, LognexApiException {
        HttpPost request = new HttpPost(getFullUrl());
        applyHeaders(request);

        if (body != null) {
            String strBody = gson.toJson(body);
            logger.debug("Тело запроса        {} {}: {}", request.getMethod(), request.getURI(), strBody);
            StringEntity requestEntity = new StringEntity(strBody, ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
        }

        return executeRequest(request);
    }

    /**
     * Выполняет POST-запрос с указанными ранее параметрами и конвертирует ответ в объект указанного класса
     *
     * @param cl класс, в который нужно сконвертировать ответ на запрос
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public <T> T post(Class<T> cl) throws IOException, LognexApiException {
        return gson.fromJson(post(), cl);
    }

    /**
     * Выполняет DELETE-запрос с указанными ранее параметрами
     *
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public void delete() throws IOException, LognexApiException {
        HttpDelete request = new HttpDelete(getFullUrl());
        applyHeaders(request);
        executeRequest(request);
    }

    /**
     * Выполняет PUT-запрос с указанными ранее параметрами
     *
     * @return тело ответа
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public String put() throws IOException, LognexApiException {
        HttpPut request = new HttpPut(getFullUrl());
        applyHeaders(request);

        if (body != null) {
            StringEntity requestEntity = new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
        }

        return executeRequest(request);
    }

    /**
     * Выполняет PUT-запрос с указанными ранее параметрами и конвертирует ответ в объект указанного класса
     *
     * @param cl класс, в который нужно сконвертировать ответ на запрос
     * @throws IOException        когда возникла сетевая ошибка
     * @throws LognexApiException когда возникла ошибка API
     */
    public <T> T put(Class<? extends T> cl) throws IOException, LognexApiException {
        return gson.fromJson(put(), cl);
    }
}
