package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //业务异常（地址簿为空，购物车数据为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> carts = shoppingCartMapper.list(shoppingCart);
        if (carts == null || carts.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入1条数据，并返回主键
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : carts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);


        //清空购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        // 通过WebSocket向客户端推送消息
        Map map = new HashMap();
        map.put("type", 1);//1表示来单提醒
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 分页查询历史订单
     */
    @Override
    public PageResult pageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> orders = orderMapper.pageQueryHistory(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        //对于每一个订单，都要查询订单的详情
        if (orders != null && orders.size() > 0) {
            orders.getResult().forEach(order -> {
                //对于每一个订单，查询所有对应的订单详情，并记录到一个数组中
                Long id = order.getId();
                List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                orderVOList.add(orderVO);
            });
        }
        return new PageResult(orders.getTotal(), orderVOList);

    }

    @Override
    public OrderVO orderDetail(Long id) {
        //根据订单id查询订单明细，并封装到OrderVO中返回
        OrderVO orderVO = new OrderVO();
        //查询订单基本信息并封装到OrderVO中
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailMapper.listByOrderId(id));
        return orderVO;
    }

    @Override
    public void cancel(Long id) {

        Orders orders = orderMapper.getById(id);
        //判断订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单为待支付或待接单，用户可以直接取消订单
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //TODO 调用微信支付退款接口
            orders.setPayStatus(Orders.REFUND);
        }
        // 商家已接单状态下，用户取消订单需电话沟通商家
        if (orders.getStatus().equals(Orders.CONFIRMED) || orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_CONFIRM_NOT_ALLOWED);
        }
        //修改订单状态为已取消，更新订单取消原因
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void repeat(Long id) {
        //根据订单id，将订单中的菜品信息加到购物车中
        List<OrderDetail> orderDetails = orderDetailMapper.listByOrderId(id);
        //将每一个菜品或者套餐信息添加购物车
        orderDetails.forEach(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            //将无法拷贝的数据赋值，在插入数据的时候不报错
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            //TODO 批量插入比循环单个插入更快
            shoppingCartMapper.insert(shoppingCart);
        });
    }

    @Override
    public PageResult pageConditionQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageConditionQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = new ArrayList<>();
        if (orders != null && orders.size() > 0) {
            orders.getResult().forEach(order -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                //将菜品信息以字符串形式保存到订单VO中
                orderVO.setOrderDishes(orderDetailMapper.getOrderDishes(order.getId()));
                orderVOList.add(orderVO);
            });
        }
        return new PageResult(orders.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO getStatistics() {
        //分别统计待派送，派送中，和待接单的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public OrderVO details(Long id) {
        //根据订单id查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orderMapper.getById(id), orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 订单完成
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        //只有派送中的订单才能完成
        if (!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.COMPLETED);
        orderMapper.update(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderMapper.updateStatusById(ordersConfirmDTO.getId(), Orders.CONFIRMED);
    }


    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        //只有订单处于“待接单”状态时可以执行拒单操作
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        //如果用户已支付，需要退款
        if (orders.getPayStatus().equals(Orders.PAID)) {
            //TODO 退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if (orders.getPayStatus().equals(Orders.PAID)) {
            //TODO 退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        //只有订单处于“已接单”状态时，才可以执行派送操作
        Orders orders = orderMapper.getById(id);
        if (!orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orders.getNumber() + "，请稍后接单");
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }
}
