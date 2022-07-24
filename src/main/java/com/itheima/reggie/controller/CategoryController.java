package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     *
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category) {
        log.info("Category: {}", category);
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {
        //分页构造器
        Page<Category> pageInfo = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //添加排序条件,根据sort进行排序
        queryWrapper.orderByAsc(Category::getSort);
        //分页查询
        categoryService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    @DeleteMapping
    public R<String> delete(long id) {
        log.info("删除分类,id为: {}", id);

        categoryService.remove(id);
        return R.success("分类信息删除成功");
    }

    /**
     * 根据id修改分类信息
     *
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category) {
        log.info("修改分类信息: {}", category);
        categoryService.updateById(category);//其他需要修改的值和id都存在对象里面,所以传入一个实体对象
        return R.success("修改分类信息成功");
    }

    /**
     * 根据条件查询分类数据(在添加菜品的菜品分类框回显/在添加套餐的套餐分类框回显)
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        //条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        // (type为1,是 菜品分类的若干条数据,如湘菜,粤菜,饮品,主食. type为2,是套餐分类的若干条数据, 如商务套餐,儿童套餐.)
        // 把相同类型的放在一组
        queryWrapper.eq(category.getType() != null,Category::getType,category.getType());
        //添加排序条件(对相同组别按照设置的要求排序)
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(queryWrapper);
        return R.success(list);
    }
}
