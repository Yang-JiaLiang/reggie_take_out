package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @GetMapping("/page")
    public  R<Page> page(int page,int pageSize,String name) {
        //构造分页构造器对象
        Page<Setmeal> pageinfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage =new Page<>();
        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null, Setmeal::getName, name);
        //添加排序添加
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //执行分页查询
        setmealService.page(pageinfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageinfo,setmealDtoPage,"records");//不拷贝records,是因为泛型不一样
        List<Setmeal> records = pageinfo.getRecords();
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {        //防止空指针
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());//赋给了66行的list

          setmealDtoPage.setRecords(list); //将最后完善的records,再set一下

        return R.success(setmealDtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids: {}",ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询对应的套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public  R<List<Setmeal>> list(Setmeal setmeal) {
        //构造查询条件
        LambdaQueryWrapper<Setmeal> queryWrapper =new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId()!=null ,Setmeal::getCategoryId,setmeal.getCategoryId());
        //添加条件,查询状态为1的套餐
        queryWrapper.eq(Setmeal::getStatus,1);

        //添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }
}
