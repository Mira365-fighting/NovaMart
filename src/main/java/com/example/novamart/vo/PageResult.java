package com.example.novamart.vo;

import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果统一封装。
 *
 * @param pageNum  当前页码
 * @param pageSize 每页条数
 * @param total    总记录数
 * @param pages    总页数
 * @param list     当前页数据
 */
public record PageResult<T>(int pageNum, int pageSize, long total, int pages, List<T> list) {

    /**
     * 由 PageHelper 的 PageInfo 转换而来，顺便把实体转成 VO。
     * 这样分页信封的组装只在这一处，各个 Service 不用各写一遍。
     */
    public static <E, V> PageResult<V> of(PageInfo<E> pageInfo, Function<E, V> converter) {
        return new PageResult<>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal(),
                pageInfo.getPages(), pageInfo.getList().stream().map(converter).toList());
    }
}
