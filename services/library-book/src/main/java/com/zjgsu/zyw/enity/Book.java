package com.zjgsu.zyw.enity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Book {
    private Integer id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 分类 */
    private String category;

    /** 国际标准书号 */
    private String isbn;

    /** 状态：AVAILABLE在馆 / BORROWED已借出 / REMOVED已下架 */
    private String status;

    /** 馆藏位置（书架号） */
    private String location;
}
