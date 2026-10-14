package com.example.novamart.util;

import com.github.pagehelper.PageHelper;

/**
 * 分页参数处理。
 *
 * <p>PageHelper 靠 ThreadLocal 传递分页参数：必须在查询语句的「上一行」调用 startPage，
 * 查询执行完插件会自动清理，所以不要跨方法乱传 Page 对象。
 */
public final class PageUtil {

    /** 单页最大条数，防止前端传个超大 pageSize 把整张表拉出来 */
    private static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    /**
     * 归一化页码和每页条数并开启分页。
     * 页码最小 1；每页条数限制在 1~100 之间，超出就按边界值处理。
     */
    public static void start(int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        PageHelper.startPage(safePageNum, safePageSize);
    }
}
