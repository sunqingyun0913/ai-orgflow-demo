package com.orgflow.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>必须注册分页插件，否则 {@code Page} 查询不会真正分页（会把全表查出来），
 * 员工列表、日程列表的分页就是这个插件在起作用。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数限制，防止前端传 size=999999 把数据库拖垮。
        // 注意：必须大于前端「日程指派」下拉用的 size=500，否则会被截断。
        pagination.setMaxLimit(1000L);
        // 超过最大页数时返回空数据，而不是回到第一页（避免前端翻页出现重复数据）
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
