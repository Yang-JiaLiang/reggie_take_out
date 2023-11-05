package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true) //删除setmealCache这个分类下所以的缓存数据
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //构造分页构造器对象
        Page<Setmeal> pageinfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();
        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null, Setmeal::getName, name);
        //添加排序添加
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //执行分页查询
        setmealService.page(pageinfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageinfo, setmealDtoPage, "records");//不拷贝records,是因为泛型不一样
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
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true) //删除setmealCache这个分类下所以的缓存数据
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids: {}", ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询对应的套餐数据
     *
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId+'_'+#setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        //构造查询条件
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        //添加条件,查询状态为1的套餐
        queryWrapper.eq(Setmeal::getStatus, 1);

        //添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 改变套餐的销售状态
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateSetmealStatus(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        //套餐具体的销售状态，由前端修改并返回，该方法传入的status是 修改之后的售卖状态，可以直接根据一个或多个菜品id进行查询并修改售卖即可
        //e.g： Request URL: http://localhost:8080/setmeal/status/0?ids=1553203698658373634,1553203616563261441
        log.info("ids :" + ids);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null, Setmeal::getId, ids);
        List<Setmeal> list = setmealService.list(queryWrapper);
        if (list != null) {
            for (Setmeal setmeal : list) {
                setmeal.setStatus(status);
                setmealService.updateById(setmeal);
            }
            return R.success("套餐的售卖状态已修改!");
        }
        return R.error("售卖状态不可更改,请联系管理员或客服！");
    }

    /**
     * 根据id查询套餐信息和对应套餐内菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getSetmel(@PathVariable("id") Long id) {
        SetmealDto setmealDto = setmealService.getSetmealData(id);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐
     *
     * @param setmealDto
     * @return
     */
    @CacheEvict(value = "setmealCache", allEntries = true) //删除setmealCache这个分类下所以的缓存数据
    @PutMapping
    public R<String> updateMeal(@RequestBody SetmealDto setmealDto) {
        setmealService.updateById(setmealDto);
        return R.success("套餐修改成功！");
    }


    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 这里返回的是dto 对象，因为前端需要copies这个属性
     * 前端主要要展示的信息是:套餐中菜品的基本信息，图片，菜品描述，以及菜品的份数
     *
     * @param SetmealId
     * @return
     */
    //这里前端是使用路径来传值的，要注意，不然你前端的请求都接收不到，就有点尴尬哈
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long SetmealId) {
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, SetmealId);
        //获取套餐里面的所有菜品  这个就是SetmealDish表里面的数据
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        List<DishDto> dishDtos = list.stream().map((setmealDish) -> { //这里的setmealDish可以是item，只是一个变量
            DishDto dishDto = new DishDto();
            //拷贝后，dishDto可以现实份数copies
            BeanUtils.copyProperties(setmealDish, dishDto);
            //这里是为了把套餐中的菜品的基本信息填充到dto中，比如菜品描述，菜品图片等菜品的基本信息
            Long dishId = setmealDish.getDishId();

            Dish dish = dishService.getById(dishId);
            if (dish != null) {
                BeanUtils.copyProperties(dish, dishDto);
            }
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtos);
    }

}
