package com.zjgsu.zyw.pojo;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 分类 */
    private String category;

    /** 馆藏总册数 */
    @Column(name = "total_count")
    private Integer totalCount;

    /** 可借数量 */
    @Column(name = "available_count")
    private Integer availableCount;

    /** 状态：AVAILABLE在馆 / BORROWED已借出 / REMOVED已下架 */
    private String status;

    /** 馆藏位置（书架号） */
    private String location;
}
