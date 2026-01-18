package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {
    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result<Integer> setStatus(@PathVariable Integer status) {
        log.info("设置营业状态：{}", status == 1 ? "营业中" : "打烊中");
        // 将状态设置到Redis中
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success(status);
    }

    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取营业状态：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
