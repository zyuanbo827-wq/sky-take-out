package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 订单超时取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("处理订单超时");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //查询哪些订单已经超时
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLt(Orders.PENDING_PAYMENT, time);
        if (orders != null && orders.size() > 0){
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 订单派送
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        //查询哪些订单已经超时
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLt(Orders.DELIVERY_IN_PROGRESS, time);
        if (orders != null && orders.size() > 0){
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
