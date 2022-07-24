package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request,@RequestBody Employee employee){

        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }

        //4、密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        //6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }


    /**
     * 新增员工
     * @param employee
     * @return
     */
     @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
         log.info("新增员工,员工信息:{}",employee.toString()); //{}是占位符方式,在日志中可以替代"+"号连接,比较方便
           //设置初始密码123456,需要进行MD5加密
          employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

//          employee.setCreateTime(LocalDateTime.now());
//          employee.setUpdateTime(LocalDateTime.now());

          //获得当前登录用户的id (已经注释,使用的新方法"公共字段自动填充)
//         Long empId = (Long) request.getSession().getAttribute("employee");
//         employee.setCreateUser(empId);
//         employee.setUpdateUser(empId);
         employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public  R<Page> page(int page,int pageSize,String name) {
        log.info("page={},pageSize={},name={}",page,pageSize,name);
        //构造分页构造器
        Page pageInfo =new Page(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper =new LambdaQueryWrapper();
        //添加过滤条件
        queryWrapper.like(StringUtils.hasText(name),Employee::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);
         return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     * @param employee
     * @return
     */
    //启用禁用与编辑 共用这个方法 实现更新
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee) {
        log.info(employee.toString());
//        Long empId = (Long) request.getSession().getAttribute("employee");//根据id获取用户名,如zhangsan
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(empId);
        long id = Thread.currentThread().getId();
        log.info("线程id: {}",id);

        employeeService.updateById(employee);//参数是个实体对象.里面包含许多属性
        return R.success("员工信息修改成功");
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    ///当id在整个请求路径里,首先需要"/{id}",其次要加上注解 @PathVariable.
    //重新编辑时,先通过这个方法实现"查找",然后通过上个update方法实现"更新"
    @GetMapping("/{id}")
    public R<Employee> getById (@PathVariable Long id) {
        log.info("根据id查询员工信息...");
        Employee employee =employeeService.getById(id);
        if(employee!=null){
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }
}
