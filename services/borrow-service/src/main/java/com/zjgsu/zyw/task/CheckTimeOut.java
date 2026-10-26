package com.zjgsu.zyw.task;

import com.zjgsu.zyw.constant.BorrowStatusConstant;
import com.zjgsu.zyw.enity.BorrowRecord;
import com.zjgsu.zyw.mapper.BorrowMapper;
import com.zjgsu.zyw.vo.BorrowRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
public class CheckTimeOut {

    @Autowired
    private BorrowMapper borrowMapper;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *",zone = "Asia/Shanghai")
    public void checkTimeOut() {
        log.info("进行逾期检验");
        LocalDate date = LocalDate.now();
        BorrowRecordVO borrowRecordVO = BorrowRecordVO.builder()
                .status(BorrowStatusConstant.BORROWED)
                .build();
        List<BorrowRecordVO> list = borrowMapper.getBorrowRecord(borrowRecordVO);
        for (BorrowRecordVO recordVO : list) {
            if(date.isAfter(recordVO.getDueDate())){
                BorrowRecord record = BorrowRecord.builder()
                        .id(recordVO.getId())
                        .status(BorrowStatusConstant.OVERDUE)
                        .build();
                borrowMapper.updateRecordById(record);
            }
        }
        borrowRecordVO.setStatus(BorrowStatusConstant.OVERDUE);
        list = borrowMapper.getBorrowRecord(borrowRecordVO);
        for (BorrowRecordVO recordVO : list){
            BorrowRecord record = BorrowRecord.builder()
                    .id(recordVO.getId())
                    .overdueDays((int) ChronoUnit.DAYS.between(recordVO.getDueDate(),date))
                    .fineAmount((int) ChronoUnit.DAYS.between(recordVO.getDueDate(), date) * 2)
                    .build();
            borrowMapper.updateRecordById(record);

        }

    }
}
