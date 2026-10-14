package com.example.novamart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.novamart.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 商品表。单表 CRUD 由 BaseMapper 提供，复杂的几处写在这里。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 前台分类筛选用的分类列表（只取在售商品的分类，去重排序） */
    @Select("SELECT DISTINCT category FROM product "
            + "WHERE status = 1 AND category IS NOT NULL AND category <> '' ORDER BY category")
    List<String> selectCategories();

    /**
     * 悲观锁：SELECT ... FOR UPDATE。
     *
     * <p>下单时用它对要购买的商品加行锁，锁会一直持有到事务提交/回滚，
     * 别的下单事务在这一步只能排队等，所以「判断库存 + 扣减」不会被并发穿插，不会超卖。
     *
     * <p>{@code ORDER BY id} 很关键：并发事务按同样的顺序申请行锁，
     * 不会出现 A 等 B、B 等 A 的循环等待（死锁）。
     *
     * <p>必须在事务里调用，否则锁会立刻释放，等于没加。
     */
    @Select("""
            <script>
            SELECT * FROM product
            WHERE id IN <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            FOR UPDATE
            </script>
            """)
    List<Product> selectByIdsForUpdate(@Param("ids") Collection<Long> ids);
}
