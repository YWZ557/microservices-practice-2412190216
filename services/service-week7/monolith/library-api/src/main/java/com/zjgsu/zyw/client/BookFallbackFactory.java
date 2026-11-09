package com.zjgsu.zyw.client;

import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.result.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookFallbackFactory implements FallbackFactory<BookClient> {

    private static final String UNAVAILABLE = "图书服务不可用，请稍后重试";

    @Override
    public BookClient create(Throwable cause) {
        return new BookClient() {
            @Override
            public void updateBook(Book book) {
                throw translate("更新图书状态 id=" + book.getId(), cause);
            }

            @Override
            public Result getBookById(Integer id) {
                throw translate("查询图书 id=" + id, cause);
            }
        };
    }

    private BaseException translate(String action, Throwable cause) {
        log.warn("调用 library-book 失败：{}", action, cause);

        if (cause instanceof FeignException ex) {
            int status = ex.status();
            if (status == 404) {
                return new BaseException("图书不存在");
            }
            if (status == 400 || status == 409) {
                return new BaseException("图书服务拒绝了本次请求");
            }
        }

        return new BaseException(UNAVAILABLE);
    }
}
