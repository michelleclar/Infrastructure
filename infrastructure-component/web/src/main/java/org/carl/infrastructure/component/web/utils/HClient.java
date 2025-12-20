package org.carl.infrastructure.component.web.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.vertx.web.Route;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HClient implements AutoCloseable {
    ObjectMapper mapper = new ObjectMapper();
    Object entity;
    Map<String, String> headers;
    final String url;
    Route.HttpMethod requestMethod = Route.HttpMethod.POST;
    CloseableHttpClient httpClient = HttpClients.createDefault();

    protected HClient(String url) {
        this.url = url;
    }

    public String executer() throws IOException {
        return switch (requestMethod) {
            case POST:
                {
                    yield handlePost();
                }
            default:
                yield null;
        };
    }

    private String handlePost() throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }
        }
        if (entity != null) {
            String jsonBody = mapper.writeValueAsString(this.entity);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        }
        try (CloseableHttpResponse execute = httpClient.execute(httpPost)) {
            HttpEntity response = execute.getEntity();
            return response != null ? EntityUtils.toString(response, StandardCharsets.UTF_8) : null;
        }
    }

    private String executer(HttpUriRequest request) {
        try (CloseableHttpResponse execute = httpClient.execute(request)) {
            HttpEntity response = execute.getEntity();
            return response != null ? EntityUtils.toString(response, StandardCharsets.UTF_8) : null;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }
}
