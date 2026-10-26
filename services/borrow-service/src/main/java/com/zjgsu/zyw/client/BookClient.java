package com.zjgsu.zyw.client;

import com.zjgsu.zyw.client.dto.BookInfo;
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

/**
 * 图书服务客户端，所有调用失败都转换为明确的业务异常。
 */
@Component
public class BookClient {

    private static final ParameterizedTypeReference<RemoteResult<BookInfo>> BOOK_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<RemoteResult<Void>> VOID_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    private final String baseUrl;

    public BookClient(RestTemplate restTemplate,
                      @Value("${services.book.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public BookInfo getBook(Integer bookId) {
        RemoteResult<BookInfo> body = exchange(
                baseUrl + "/internal/books/{id}",
                HttpMethod.GET,
                null,
                BOOK_TYPE,
                bookId
        );
        if (body.getData() == null) {
            throw new BaseException(body.getMsg() == null ? "图书不存在" : body.getMsg());
        }
        return body.getData();
    }

    public void markBorrowed(Integer bookId) {
        RemoteResult<Void> body = exchange(
                baseUrl + "/internal/books/{id}/borrow",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                VOID_TYPE,
                bookId
        );
        checkSuccess(body);
    }

    public void markReturned(Integer bookId) {
        RemoteResult<Void> body = exchange(
                baseUrl + "/internal/books/{id}/return",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                VOID_TYPE,
                bookId
        );
        checkSuccess(body);
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
                throw new BaseException("图书服务返回为空");
            }
            return body;
        } catch (RestClientException e) {
            throw new BaseException("图书服务不可用，请稍后重试");
        }
    }

    private void checkSuccess(RemoteResult<?> body) {
        if (body.getCode() == null || body.getCode() != 1) {
            throw new BaseException(body.getMsg() == null ? "图书服务调用失败" : body.getMsg());
        }
    }
}
