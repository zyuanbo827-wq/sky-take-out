package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Service;


public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 订单详情
     * @param id
     * @return
     */
    OrderVO orderDetail(Long id);

    /**
     * 用户取消订单
     * @param id
     */
    void cancel(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repeat(Long id);

    /**
     * 条件查询顶动感
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageConditionQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计订单数据
     * @return
     */
    OrderStatisticsVO getStatistics();

    /**
     * 订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 订单完成
     * @param id
     */
    void complete(Long id);

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void reject(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 管理员取消订单
     * @param ordersCancelDTO
     */
    void adminCancel(OrdersCancelDTO ordersCancelDTO);
    /**
     * 派单
     * @param id
     */
    void delivery(Long id);

    /**
     * 催单
     * @param id
     */
    void reminder(Long id);
}
