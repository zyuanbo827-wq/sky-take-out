package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 历史订单查询
      * @param ordersPageQueryDTO
     * @return
     */

    Page<Orders> pageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageConditionQuery(OrdersPageQueryDTO ordersPageQueryDTO);
    /**
     * 根据状态统计订单数量
      * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 修改订单状态
     * @param id
     */
    @Update("update orders set status = #{status} where id = #{id}")
    void updateStatusById(Long id, Integer status);

    /**
     * 查询状态为PENDING_PAYMENT的订单
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeLt(Integer status, LocalDateTime time);

    /**
     * 计算每日
     */
    @Select("select sum(amount) from orders where order_time between #{beginTime} and #{endTime} and status = #{status}")
    Double sumByMap(LocalDateTime beginTime, LocalDateTime endTime, Integer status); // Method not defined in XML



    /**
     * 查询订单数量
     * @param map
     * @return
     */
    Integer countOrder(Map map);

    /**
     * 查询销量排名top10
     * @param map
     * @return
     */
    List<GoodsSalesDTO> getByMap(Map map);

}
