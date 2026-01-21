package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getThisWeekOrderCount(LocalDate begin, LocalDate end) {
        //当前集合用于存放日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询date日期对应的营业额数据，指状态为“已完成”的订单营业总额
            //select sum(amount) from orders where order_time >= #{begin} and order_time <=#{end} and status = 5
            Double turnover = orderMapper.sumByMap(beginTime, endTime, Orders.COMPLETED);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 获取指定时间区间内的总用户数量和新增用户数量
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getThisWeekUserCount(LocalDate begin, LocalDate end) {


        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        //循环得到每一天的起始时间和结束时间
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //新建用户总数列表和新增员工数列表
        List<Integer> totalUserCountList = new ArrayList<>();
        List<Integer> newUserCountList = new ArrayList<>();

        //对List<LocalDate>进行遍历，查询用户总数和每一天的新增用户数
        for (LocalDate date : dateList) {
            //获取准确的每一天的起始时间和结束时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //对于用户总数，只需要满足用户建立时间小于结束时间即可，使用动态sql进行查询，输入map
            Map map = new HashMap();
            map.put("endTime", endTime);

            Integer totalUserCount = userMapper.countUser(map);

            map.put("beginTime", beginTime);
            Integer newUserCount = userMapper.countUser(map);

            //添加到对应的列表中
            totalUserCountList.add(totalUserCount);
            newUserCountList.add(newUserCount);
        }
        //封装结果到UserReportVO对象中并返回


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserCountList, ","))
                .newUserList(StringUtils.join(newUserCountList, ","))
                .build();
    }

    /**
     * 获取指定时间区间内的订单统计数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getThisWeekOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        //循环得到每一天的起始时间和结束时间
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        //对List<LocalDate>进行遍历，查询每一天的订单总数和有效订单数量
        for (LocalDate date : dateList) {
            //获取准确的每一天的起始时间和结束时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //对于用户总数，只需要满足用户建立时间小于结束时间即可，使用动态sql进行查询，输入map
            Map map = new HashMap();
            map.put("endTime", endTime);
            map.put("beginTime", beginTime);

            Integer totalOrderCount = orderMapper.countOrder(map);
            map.put("status", Orders.COMPLETED);

            Integer newOrderCount = orderMapper.countOrder(map);

            //添加到对应的列表中
            orderCountList.add(totalOrderCount);
            validOrderCountList.add(newOrderCount);
        }
        //计算订单总量、有效订单数和订单完成率
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount;


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 获取指定时间区间内的销量排名前10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTOP10Statistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        Map map = new HashMap();
        map.put("endTime", endTime);
        map.put("beginTime", beginTime);
        List<GoodsSalesDTO> goodsSalesDTOlist = orderMapper.getByMap(map);

        //把goodsSalesDTOlist封装到GoodsSalesVO中,
        List<String> nameList = goodsSalesDTOlist.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOlist.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();

    }

}
