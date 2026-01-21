package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    /**
     * 统计营业额数据
     * @return
     */
    TurnoverReportVO getThisWeekOrderCount(LocalDate begin, LocalDate end);

    /**
     * 统计用户数据
     * @return
     */
    UserReportVO getThisWeekUserCount(LocalDate begin, LocalDate end);

    /**
     * 统计订单数据
     * @return
     */
    OrderReportVO getThisWeekOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计销量排名
     * @return
     */
    SalesTop10ReportVO getTOP10Statistics(LocalDate begin, LocalDate end);

}
