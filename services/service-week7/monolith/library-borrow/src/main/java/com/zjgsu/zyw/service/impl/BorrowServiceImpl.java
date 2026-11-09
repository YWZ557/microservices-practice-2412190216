package com.zjgsu.zyw.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zjgsu.zyw.client.BookClient;
import com.zjgsu.zyw.client.UserClient;
import com.zjgsu.zyw.constant.BookStatusConstant;
import com.zjgsu.zyw.constant.BorrowStatusConstant;
import com.zjgsu.zyw.constant.UserConstant;
import com.zjgsu.zyw.context.BaseContext;
import com.zjgsu.zyw.dto.BorrowPageDTO;
import com.zjgsu.zyw.dto.BorrowRecordDTO;
import com.zjgsu.zyw.enity.Book;
import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.mapper.BorrowMapper;
import com.zjgsu.zyw.message.BorrowMessage;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.result.Result;
import com.zjgsu.zyw.service.BorrowService;
import com.zjgsu.zyw.vo.BorrowRecordVO;
import com.zjgsu.zyw.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;
    @Autowired
    private BookClient bookClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private RestTemplate restTemplate;


    public List<BorrowRecordVO> getBorrowRecordById(Integer id) {
        BorrowRecordVO borrowRecordVO = BorrowRecordVO.builder()
                .readerId(id)
                .build();
        return borrowMapper.getBorrowRecord(borrowRecordVO);
    }

    @Override
    @Transactional
    public void CreateRecord(BorrowRecordDTO borrowRecordDTO) {
        BorrowRecord borrowRecord = new BorrowRecord();
        BeanUtils.copyProperties(borrowRecordDTO, borrowRecord);

        borrowRecord.setBorrowDate(LocalDate.now());
        borrowRecord.setStatus(BorrowStatusConstant.BORROWED);
        borrowRecord.setOverdueDays(0);
        borrowRecord.setFineAmount(0);
        Integer bookId = borrowRecord.getBookId();
        String message = CheckRecord(borrowRecordDTO);
        if (!message.equals(BorrowMessage.TURE)) {
            log.info("创建借书记录失败{}", message);
            throw new BaseException("创建借书记录失败: "+message);
        }
        Book book = new Book();
        book.setId(bookId);
        book.setStatus(BookStatusConstant.BORROWED);
        bookClient.updateBook(book);
        borrowMapper.createRecord(borrowRecord);

    }

    @Override
    @Transactional
    public void DeleteRecordById(Integer id) {
        BorrowRecord borrowRecord = borrowMapper.getBorrowRecordById(id);
        Integer bookId = borrowRecord.getBookId();
        if (BorrowStatusConstant.OVERDUE.equals(borrowRecord.getStatus())||BorrowStatusConstant.BORROWED.equals(borrowRecord.getStatus())){
            Book book = new Book();
            book.setId(bookId);
            book.setStatus(BookStatusConstant.AVAILABLE);
            bookClient.updateBook(book);
        }
        borrowMapper.deleteRecordById(id);
    }

    @Override
    public void Update(BorrowRecord borrowRecord) {
        borrowMapper.updateRecordById(borrowRecord);
    }

    @Override
    public List<BorrowRecordVO> getBorrowRecordByName(String name) {
        BorrowRecordVO borrowRecordVO = BorrowRecordVO.builder()
                .name(name)
                .build();
        return borrowMapper.getBorrowRecord(borrowRecordVO);
    }

    @Override
    public String CheckRecord(BorrowRecordDTO borrowRecordDTO) {
        Integer bookId = borrowRecordDTO.getBookId();
        Integer readerId = borrowRecordDTO.getReaderId();

        Book book = bookClient.getBookById(bookId).getData();
        if (book == null) {
            return BorrowMessage.BOOK_NOT_FOUND;
        }
        if (BookStatusConstant.REMOVED.equals(book.getStatus())) {
            return BorrowMessage.BOOK_NOT_FOUND;
        }
        if (BookStatusConstant.BORROWED.equals(book.getStatus())) {
            return BorrowMessage.BOOK_NO_STOCK;
        }
        if (borrowMapper.countByReaderId(readerId) > 4) {
            return BorrowMessage.BORROW_LIMIT_EXCEEDED;
        }
        UserVO userVO = userClient.getUserById(readerId).getData();
        if (userVO == null) {
            return BorrowMessage.READER_NOT_FOUND;
        }
        if (UserConstant.STATUS_DISABLED.equals(userVO.getStatus())) {
             return BorrowMessage.READER_FROZEN;
        }
        return BorrowMessage.TURE;
    }

    @Override
    @Transactional
    public void ReturnBook(Integer id) {
        BorrowRecord record = borrowMapper.getBorrowRecordById(id);
        if (BorrowStatusConstant.RETURNED.equals(record.getStatus()) || BorrowStatusConstant.LOST.equals(record.getStatus())) {
            throw new BaseException("归还失败： 该记录处于已归还或丢失状态");
        }
        Book book = new Book();
        book.setId(record.getBookId());
        book.setStatus(BookStatusConstant.AVAILABLE);
        bookClient.updateBook(book);
        BorrowRecord borrowRecord = BorrowRecord.builder()
                .id(id)
                .status(BorrowStatusConstant.RETURNED)
                .returnDate(LocalDate.now())
                .build();
        borrowMapper.updateRecordById(borrowRecord);
    }

    @Override
    public PageResult pageSearch(BorrowPageDTO borrowPageDTO) {
        PageHelper.startPage(borrowPageDTO.getPage(), borrowPageDTO.getPageSize());
        BorrowRecordVO borrowRecordVO = new BorrowRecordVO();
        BeanUtils.copyProperties(borrowPageDTO, borrowRecordVO);
        List<BorrowRecordVO> list = borrowMapper.getBorrowRecord(borrowRecordVO);
        Page<BorrowRecordVO> p = (Page<BorrowRecordVO>) list;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void test(Integer id) {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("library-book");
        ServiceInstance serviceInstance = serviceInstances.get(RandomUtil.randomInt(serviceInstances.size()));
        ResponseEntity<Result> responseEntity = restTemplate.exchange(
                serviceInstance.getUri() + "/book/admin/{id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result>() {},
                Map.of("id",id)
        );
        if(!responseEntity.getStatusCode().is2xxSuccessful()){
            return;
        }
        Result result = responseEntity.getBody();
        System.out.println(result.getData().toString());
    }
}
