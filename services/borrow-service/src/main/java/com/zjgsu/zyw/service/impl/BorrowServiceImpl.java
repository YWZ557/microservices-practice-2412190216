package com.zjgsu.zyw.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zjgsu.zyw.client.BookClient;
import com.zjgsu.zyw.client.ReaderClient;
import com.zjgsu.zyw.client.dto.BookInfo;
import com.zjgsu.zyw.client.dto.ReaderInfo;
import com.zjgsu.zyw.constant.BookStatusConstant;
import com.zjgsu.zyw.constant.BorrowStatusConstant;
import com.zjgsu.zyw.dto.BorrowPageDTO;
import com.zjgsu.zyw.dto.BorrowRecordDTO;
import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.mapper.BorrowMapper;
import com.zjgsu.zyw.message.BorrowMessage;
import com.zjgsu.zyw.result.PageResult;
import com.zjgsu.zyw.service.BorrowService;
import com.zjgsu.zyw.vo.BorrowRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private ReaderClient readerClient;

    @Autowired
    private BookClient bookClient;

    public List<BorrowRecordVO> getBorrowRecordById(Integer id) {
        BorrowRecordVO query = BorrowRecordVO.builder()
                .readerId(id)
                .build();
        List<BorrowRecordVO> records = borrowMapper.getBorrowRecord(query);
        fillReaderNames(records);
        return records;
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
            throw new BaseException("创建借书记录失败: " + message);
        }

        // 先让图书服务扣减借出状态，再写本地借阅记录
        bookClient.markBorrowed(bookId);
        borrowMapper.createRecord(borrowRecord);
    }

    @Override
    @Transactional
    public void DeleteRecordById(Integer id) {
        BorrowRecord borrowRecord = borrowMapper.getBorrowRecordById(id);
        if (borrowRecord == null) {
            throw new BaseException(BorrowMessage.RECORD_NOT_FOUND);
        }
        Integer bookId = borrowRecord.getBookId();
        if (BorrowStatusConstant.OVERDUE.equals(borrowRecord.getStatus())
                || BorrowStatusConstant.BORROWED.equals(borrowRecord.getStatus())) {
            bookClient.markReturned(bookId);
        }
        borrowMapper.deleteRecordById(id);
    }

    @Override
    public void Update(BorrowRecord borrowRecord) {
        borrowMapper.updateRecordById(borrowRecord);
    }

    @Override
    public List<BorrowRecordVO> getBorrowRecordByName(String name) {
        BorrowRecordVO query = new BorrowRecordVO();
        if (name != null && !name.isBlank()) {
            List<ReaderInfo> readers = readerClient.searchReaders(name);
            if (readers.isEmpty()) {
                return List.of();
            }
            query.setReaderIds(readers.stream().map(ReaderInfo::getId).toList());
        }
        List<BorrowRecordVO> records = borrowMapper.getBorrowRecord(query);
        fillReaderNames(records);
        return records;
    }

    @Override
    public String CheckRecord(BorrowRecordDTO borrowRecordDTO) {
        Integer bookId = borrowRecordDTO.getBookId();
        Integer readerId = borrowRecordDTO.getReaderId();

        ReaderInfo reader = readerClient.getReader(readerId);
        if (reader == null) {
            return BorrowMessage.READER_NOT_FOUND;
        }
        if (Objects.equals(reader.getStatus(), 0)) {
            return BorrowMessage.READER_FROZEN;
        }

        BookInfo book = bookClient.getBook(bookId);
        if (book == null) {
            return BorrowMessage.BOOK_NOT_FOUND;
        }
        if (BookStatusConstant.REMOVED.equals(book.getStatus())) {
            return BorrowMessage.BOOK_REMOVED;
        }
        if (BookStatusConstant.BORROWED.equals(book.getStatus())) {
            return BorrowMessage.BOOK_NO_STOCK;
        }
        if (borrowMapper.countByReaderId(readerId) > 4) {
            return BorrowMessage.BORROW_LIMIT_EXCEEDED;
        }

        return BorrowMessage.TURE;
    }

    @Override
    @Transactional
    public void ReturnBook(Integer id) {
        BorrowRecord record = borrowMapper.getBorrowRecordById(id);
        if (record == null) {
            throw new BaseException(BorrowMessage.RECORD_NOT_FOUND);
        }
        if (BorrowStatusConstant.RETURNED.equals(record.getStatus())
                || BorrowStatusConstant.LOST.equals(record.getStatus())) {
            throw new BaseException("归还失败： 该记录处于已归还或丢失状态");
        }

        bookClient.markReturned(record.getBookId());

        BorrowRecord borrowRecord = BorrowRecord.builder()
                .id(id)
                .status(BorrowStatusConstant.RETURNED)
                .returnDate(LocalDate.now())
                .build();
        borrowMapper.updateRecordById(borrowRecord);
    }

    @Override
    public PageResult pageSearch(BorrowPageDTO borrowPageDTO) {
        BorrowRecordVO query = new BorrowRecordVO();
        BeanUtils.copyProperties(borrowPageDTO, query);

        if (borrowPageDTO.getName() != null && !borrowPageDTO.getName().isBlank()) {
            List<ReaderInfo> readers = readerClient.searchReaders(borrowPageDTO.getName());
            if (readers.isEmpty()) {
                return new PageResult(0L, List.of());
            }
            query.setReaderIds(readers.stream().map(ReaderInfo::getId).toList());
        }

        PageHelper.startPage(borrowPageDTO.getPage(), borrowPageDTO.getPageSize());
        List<BorrowRecordVO> list = borrowMapper.getBorrowRecord(query);
        fillReaderNames(list);
        Page<BorrowRecordVO> page = (Page<BorrowRecordVO>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 借阅记录只保存 readerId，姓名通过读者服务批量回填。
     */
    private void fillReaderNames(List<BorrowRecordVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> readerIds = records.stream()
                .map(BorrowRecordVO::getReaderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (readerIds.isEmpty()) {
            return;
        }
        Map<Integer, String> names = readerClient.getReaders(readerIds).stream()
                .collect(Collectors.toMap(ReaderInfo::getId, ReaderInfo::getName, (a, b) -> a));
        records.forEach(record -> record.setName(names.get(record.getReaderId())));
    }
}
