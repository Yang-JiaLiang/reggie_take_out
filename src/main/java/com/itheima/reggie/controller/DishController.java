package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
   @Autowired
    private DishService dishService;
   @Autowired
    private DishFlavorService dishFlavorService;

   @Autowired
   private CategoryService categoryService;

    /**
     * 新增菜品
      * @param dishDto
     * @return
     */
   @PostMapping
    public  R<String> save(@RequestBody  DishDto dishDto) {
       log.info(dishDto.toString());
       dishService.saveWithFlavor(dishDto);
       return  R.success("新增菜品成功");
   }

    /**
     * 菜品信息分类查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
   @GetMapping("/page")
   public R<Page> page(int page,int pageSize,String name) {
       //构造分页构造器对象
       Page<Dish> pageInfo = new Page<>(page,pageSize);
       Page<DishDto> dishDtoPage = new Page<>();

       //条件构造器
       LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
       //添加过滤条件
       queryWrapper.like(name != null,Dish::getName,name);
       //添加排序条件
       queryWrapper.orderByDesc(Dish::getUpdateTime);
       //执行分页查询
       dishService.page(pageInfo,queryWrapper);
       //对象拷贝
       BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
       List<Dish> records = pageInfo.getRecords();//获得除了CategoryName外的每条信息和属性(原始pageInfo里没有CategoryName)
       List<DishDto> list = records.stream().map((item) -> { //stream流遍历
           DishDto dishDto = new DishDto();
           BeanUtils.copyProperties(item,dishDto); //将除CategoryName外的基本属性 拷贝到dishDto
           Long categoryId = item.getCategoryId();//分类的id
           //根据id查询分类对象
           Category category = categoryService.getById(categoryId);
           if(category != null){ //防止空指针
               String categoryName = category.getName();
               dishDto.setCategoryName(categoryName);
           }
           return dishDto;
       }).collect(Collectors.toList()); //赋给了71行的list

       dishDtoPage.setRecords(list); //将最后完善的records,再set一下
       return R.success(dishDtoPage);
   }

    /**
     * 菜品信息在修改栏回显
     * @param id
     * @return
     */
     @GetMapping("/{id}")
       public  R<DishDto> get(@PathVariable Long id) {
       DishDto dishDto = dishService.getByIdWithFlavor(id);
       return R.success(dishDto);
   }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public  R<String> update(@RequestBody  DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        return  R.success("修改菜品成功");
    }

//    /**
//     * 根据条件查询对应的(同系的)菜品数据
//     * @param dish
//     * @return
//     */
//    @GetMapping("/list")
//    public R<List<Dish>> list (Dish dish) {
//        LambdaQueryWrapper<Dish> queryWrapper =new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        //查询status为1的(起售状态的菜)
//        queryWrapper.eq(Dish::getStatus,1);
//        List<Dish> list = dishService.list(queryWrapper);
//        return  R.success(list);
//    }

    @GetMapping("/list")
    public R<List<DishDto>> list (Dish dish) {
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        List<DishDto> dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);


            return dishDto;

        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

}
