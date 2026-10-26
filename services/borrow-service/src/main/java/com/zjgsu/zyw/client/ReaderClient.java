package com.zjgsu.zyw.client;

import com.zjgsu.zyw.client.dto.ReaderInfo;
import com.zjgsu.zyw.client.dto.RemoteResult;
import com.zjgsu.zyw.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 读者服务客户端，所有调用失败都转换为明确的业务异常。
 */
@Component
public class ReaderClient {

    private static final ParameterizedTypeReference<RemoteResult<ReaderInfo>> READER_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<RemoteResult<List<ReaderInfo>>> READER_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public ReaderClient(RestTemplate restTemplate,
                        @Value("${services.reader.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ReaderInfo getReader(Integer readerId) {
        RemoteResult<ReaderInfo> body = exchange(
                baseUrl + "/internal/readers/{id}",
                HttpMethod.GET,
                null,
                READER_TYPE,
                readerId
        );
        if (body.getData() == null) {
            throw new BaseException(body.getMsg() == null ? "读者不存在" : body.getMsg());
        }
        return body.getData();
    }

    public List<ReaderInfo> searchReaders(String name) {
        RemoteResult<List<ReaderInfo>> body = exchange(
                baseUrl + "/internal/readers/search?name={name}",
                HttpMethod.GET,
                null,
                READER_LIST_TYPE,
                name == null ? "" : name
        );
        return body.getData() == null ? List.of() : body.getData();
    }

    public List<ReaderInfo> getReaders(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        RemoteResult<List<ReaderInfo>> body = exchange(
                baseUrl + "/internal/readers/batch",
                HttpMethod.POST,
                new HttpEntity<>(ids),
                READER_LIST_TYPE
        );
        return body.getData() == null ? List.of() : body.getData();
    }

    private <T> RemoteResult<T> exchange(String url,
                                         HttpMethod method,
                                         HttpEntity<?> entity,
                                         ParameterizedTypeReference<RemoteResult<T>> type,
                                         Object... uriVariables) {
        try {
            ResponseEntity<RemoteResult<T>> response =
                    restTemplate.exchange(url, method, entity, type, uriVariables);
            RemoteResult<T> body = response.getBody();
            if (body == null) {
                throw new BaseException("读者服务返回为空");
            }
            return body;
        } catch (RestClientException e) {
            throw new BaseException("读者服务不可用，请稍后重试");
        }
    }
}
